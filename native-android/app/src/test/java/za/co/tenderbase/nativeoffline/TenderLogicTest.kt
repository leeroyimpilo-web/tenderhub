package za.co.tenderbase.nativeoffline

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TenderLogicTest {
    private val now=Instant.parse("2026-11-04T10:00:00Z")
    private fun tender(id:String,close:String?,province:String="KwaZulu-Natal",
                       title:String="Borehole repair"):Tender =
        Tender(id,"T-"+id,title,"Repair and install pump","Public Works",
            province,"Construction","Published","2026-11-02T10:00:00",
            close,null,"https://etenders.gov.za/test/"+id,"[]",null)

    @Test fun searchFiltersAllDeadlineStates() {
        val open=tender("open","2026-11-12T11:00:00")
        val closed=tender("closed","2026-11-01T11:00:00")
        val undated=tender("undated",null)
        val all=listOf(open,closed,undated)
        assertEquals(listOf(open),searchOffline(all,SearchCriteria(status="open"),now))
        assertEquals(listOf(closed),searchOffline(all,SearchCriteria(status="closed"),now))
        assertEquals(listOf(undated),searchOffline(all,SearchCriteria(status="undated"),now))
        assertEquals(3,searchOffline(all,SearchCriteria(status="all"),now).size)
    }
    @Test fun allFiltersWorkTogether() {
        val all=listOf(tender("1","2026-11-12T11:00:00"),
            tender("2","2026-11-10T11:00:00","Gauteng"),
            tender("3",null))
        assertEquals(listOf("1"),searchOffline(all,SearchCriteria(query="pump",province="KwaZulu-Natal",
            buyer="Public",category="Construction",status="open"),now).map{it.id})
    }
    @Test fun sourceTimesUseSouthAfricanClock() {
        val d="2026-11-12T11:00:00"
        assertEquals("12 Nov 2026 · 11:00",formatDate(d,true))
        assertEquals("11:00",formatClock(d))
        assertEquals(null,formatClock("2026-11-12T00:00:00"))
        assertEquals("12 Nov 2026",formatDate("2026-11-12T00:00:00",true))
    }
    @Test fun closedNoticeHasNoNegativeDaysRemaining() {
        val old=tender("old","2026-11-01T11:00:00")
        assertFalse(isOpenTender(old,now))
        assertEquals("Closed",closingSummary(old,now))
    }
    @Test fun alertDismissalsDoNotChangeSavedState() {
        val open=tender("x","2026-11-06T11:00:00")
        assertEquals(1,activeAlertTenders(listOf(open),7,emptySet(),now).size)
        assertEquals(0,activeAlertTenders(listOf(open),7,
            setOf("x|2026-11-06T11:00:00"),now).size)
    }
    @Test fun repeatedOfflineSearchKeepsResultsStable() {
        val records=(1..2020).map { index ->
            tender(index.toString(),if(index%2==0)"2026-11-12T11:00:00"
                else "2026-11-01T11:00:00")
        }
        val criteria=SearchCriteria(query="pump",province="KwaZulu-Natal",status="open")
        repeat(4) {
            val found=searchOffline(records,criteria,now)
            assertEquals(1010,found.size)
            assertTrue(found.all { it.id.toInt()%2==0 })
        }
    }
    @Test fun invalidSourceDatesNeverCrashSearch() {
        val malformed=tender("invalid","not-a-date")
        assertNull(tenderInstant(malformed.closingAt))
        assertNull(tenderInstant(malformed.closingAt))
        assertEquals("Not supplied",formatDate(malformed.closingAt))
        assertTrue(searchOffline(listOf(malformed),SearchCriteria(status="open"),now).isEmpty())
    }

    @Test fun insightsUseActualGroupCounts() {
        val data=listOf(tender("a",null),tender("b",null),tender("c",null,"Gauteng"))
        assertEquals(listOf("KwaZulu-Natal" to 2,"Gauteng" to 1),analytics(data,"Provinces"))
    }
}
