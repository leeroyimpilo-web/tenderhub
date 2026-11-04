import type {Metadata} from 'next';
import Link from 'next/link';
import {CalendarDays,CalendarClock} from 'lucide-react';
import {getUpcoming} from '@/lib/tenders';
import {fmtDate} from '@/lib/format';
import {CalendarButton} from '@/components/TenderActions';
import {DataError,Empty} from '@/components/DataState';
export const dynamic='force-dynamic';
export const metadata:Metadata={title:'Closing calendar'};
export default async function CalendarPage(){
 let records:Awaited<ReturnType<typeof getUpcoming>>|null=null;
 try{records=await getUpcoming(90)}catch(error){console.error('[calendar]',error)}
 const grouped=new Map<string,NonNullable<typeof records>>();
 (records??[]).forEach(t=>{const key=fmtDate(t.closing_at);grouped.set(key,[...(grouped.get(key)??[]),t])});
 return <main className="tb-v2-content">
  <p className="tb-v2-eyebrow">Stay ahead</p><h1 className="tb-v2-heading mt-2">Deadline calendar</h1>
  <p className="tb-v2-muted mt-2">Upcoming official tender deadlines within 90 days, grouped by closing date. Download events into your phone calendar.</p>
  {!records?<div className="mt-6"><DataError/></div>:!records.length?<div className="mt-6"><Empty message="There are no upcoming deadlines in the indexed official dataset."/></div>:
  <div className="tb-v2-list mt-5">{[...grouped.entries()].map(([date,items])=><section key={date} className="tb-v2-panel">
   <h2 className="font-semibold text-navy flex items-center gap-2"><CalendarDays size={17}/>{date} <span className="tb-v2-badge">{items.length}</span></h2>
   {items.map(t=><div key={t.id} className="py-4 border-t border-line mt-3 first:border-0">
    <Link href={'/tenders/'+encodeURIComponent(t.id)} className="font-semibold text-[13px] text-navy hover:underline">{t.title}</Link>
    <p className="tb-v2-muted mt-1">{t.buyer??'Buyer not supplied'} · {t.reference??t.id}</p>
    <div className="mt-3"><CalendarButton tender={t}/></div>
   </div>)}
  </section>)}</div>}
 </main>;
}
