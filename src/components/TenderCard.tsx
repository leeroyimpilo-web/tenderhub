import Link from 'next/link';
import {CalendarClock,MapPin,Building2,ArrowUpRight} from 'lucide-react';
import type {Tender} from '@/lib/types';
import {fmtDate,tenderStatus} from '@/lib/format';
import {SaveButton,TrackButton} from '@/components/TenderActions';
export function TenderCard({tender}:{tender:Tender}){
 const status=tenderStatus(tender.closing_at);
 return <article className="tb-v2-panel">
  <div className="flex flex-wrap gap-2 justify-between items-center mb-3"><span className={'tb-v2-badge '+(status==='Open'?'open':status==='Closed'?'closed':'')}>{status}</span><span className="text-[11px] text-ink-3">{tender.reference??tender.id}</span></div>
  <Link href={'/tenders/'+encodeURIComponent(tender.id)} className="text-[15px] font-semibold text-navy leading-snug hover:underline flex gap-2 justify-between">{tender.title}<ArrowUpRight size={16} className="shrink-0"/></Link>
  <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-[11px] text-ink-2">
   <span className="inline-flex items-center gap-1"><Building2 size={13}/>{tender.buyer??'Buyer not supplied'}</span>
   {tender.province&&<span className="inline-flex items-center gap-1"><MapPin size={13}/>{tender.province}</span>}
   <span className="inline-flex items-center gap-1"><CalendarClock size={13}/>Closes {fmtDate(tender.closing_at)}</span>
  </div>
  <div className="mt-4 flex flex-wrap gap-2"><SaveButton tender={tender}/><TrackButton tender={tender}/></div>
 </article>;
}
