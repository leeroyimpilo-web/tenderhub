package za.co.tenderbase.nativeoffline

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val saZone=ZoneId.of("Africa/Johannesburg")
fun tenderInstant(value:String?):Instant?{
    if(value.isNullOrBlank())return null
    return try{Instant.parse(value)}catch(_:Exception){
        try{OffsetDateTime.parse(value).toInstant()}catch(_:Exception){
            try{LocalDateTime.parse(value).atZone(saZone).toInstant()}catch(_:Exception){
                try{LocalDate.parse(value).atStartOfDay(saZone).toInstant()}catch(_:Exception){null}
            }
        }
    }
}
fun sourceHasTime(value:String?):Boolean{
    if(value.isNullOrBlank() || !value.contains("T"))return false
    val time=value.substringAfter("T").take(8)
    return time!="00:00:00" && time!="00:00"
}
fun closingDate(value:String?):LocalDate?=tenderInstant(value)?.atZone(saZone)?.toLocalDate()
fun formatDate(value:String?,withTime:Boolean=false):String{
    val instant=tenderInstant(value)?:return "Not supplied"
    val pattern=if(withTime && sourceHasTime(value))"dd MMM yyyy · HH:mm" else "dd MMM yyyy"
    return DateTimeFormatter.ofPattern(pattern).withZone(saZone).format(instant)
}
fun formatClock(value:String?):String?{
    val instant=tenderInstant(value)?:return null
    if(!sourceHasTime(value))return null
    return DateTimeFormatter.ofPattern("HH:mm").withZone(saZone).format(instant)
}
fun isOpenTender(t:Tender,now:Instant=Instant.now()):Boolean{
    val date=closingDate(t.closingAt)?:return false
    return if(sourceHasTime(t.closingAt))tenderInstant(t.closingAt)!!.isAfter(now)
    else !date.isBefore(now.atZone(saZone).toLocalDate())
}
fun isClosedTender(t:Tender,now:Instant=Instant.now())=
    closingDate(t.closingAt)!=null && !isOpenTender(t,now)
fun daysRemaining(value:String?,now:Instant=Instant.now()):Long?{
    val date=closingDate(value)?:return null
    return ChronoUnit.DAYS.between(now.atZone(saZone).toLocalDate(),date)
}
fun closingSummary(t:Tender,now:Instant=Instant.now()):String{
    if(t.closingAt==null)return "Closing date not supplied"
    if(!isOpenTender(t,now))return "Closed"
    val days=daysRemaining(t.closingAt,now)?:return "Closing date not supplied"
    return when(days){
        0L->"Closes today"
        1L->"Closes tomorrow"
        else->"Closes in "+days+" days"
    }
}
fun isUrgent(t:Tender,now:Instant=Instant.now()):Boolean=
    isOpenTender(t,now) && (daysRemaining(t.closingAt,now)?:Long.MAX_VALUE)<=3L

data class SearchCriteria(
    val query:String="",val province:String="",val category:String="",val buyer:String="",
    val status:String="open",val sort:String="newest"
)
fun searchOffline(tenders:List<Tender>,f:SearchCriteria,now:Instant=Instant.now()):List<Tender>{
    val q=f.query.trim()
    val result=tenders.filter{t->
        (q.isEmpty()||listOf(t.title,t.reference,t.description,t.buyer).any{it?.contains(q,true)==true}) &&
        (f.province.isBlank()||t.province==f.province) &&
        (f.category.isBlank()||t.category==f.category) &&
        (f.buyer.isBlank()||t.buyer?.contains(f.buyer.trim(),true)==true) &&
        when(f.status){
            "open"->isOpenTender(t,now)
            "closed"->isClosedTender(t,now)
            "undated"->t.closingAt.isNullOrBlank()
            else->true
        }
    }
    return when(f.sort){
        "closing"->result.sortedWith(compareBy(nullsLast()){tenderInstant(it.closingAt)})
        "oldest"->result.sortedWith(compareBy(nullsLast()){tenderInstant(it.publishedAt)})
        else->result.sortedByDescending{tenderInstant(it.publishedAt)?:tenderInstant(it.releaseAt)?:Instant.EPOCH}
    }
}
fun activeAlertTenders(saved:List<Tender>,days:Int,dismissed:Set<String>,now:Instant=Instant.now()):List<Tender> =
    saved.filter{
        val d=daysRemaining(it.closingAt,now)
        isOpenTender(it,now)&&d!=null&&d in 0..days.toLong() &&
            (it.id+"|"+(it.closingAt?:"")) !in dismissed
    }.sortedBy{tenderInstant(it.closingAt)}
fun analytics(tenders:List<Tender>,by:String):List<Pair<String,Int>>{
    val values=tenders.mapNotNull{t->
        when(by){"Provinces"->t.province;"Buyers"->t.buyer;else->t.category}
            ?.trim()?.takeIf{it.isNotEmpty()}
    }
    return values.groupingBy{it}.eachCount().toList().sortedWith(compareByDescending<Pair<String,Int>>{it.second}.thenBy{it.first})
}
