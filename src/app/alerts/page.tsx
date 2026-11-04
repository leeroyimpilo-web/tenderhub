'use client';
import Link from 'next/link';
import {Bell,CalendarClock,Check,RotateCcw} from 'lucide-react';
import {useLocal} from '@/components/LocalProvider';
import {daysUntil,fmtDate} from '@/lib/format';
import {CalendarButton} from '@/components/TenderActions';
export default function Alerts(){
 const {data,ready,dismissAlert,resetAlerts}=useLocal();
 const days=data.settings.remindDays;
 const matches=data.saved.filter(t=>{const d=daysUntil(t.closing_at);return d!==null&&d>=0&&d<=days&&!data.dismissedAlerts.includes(t.id)}).sort((a,b)=>(a.closing_at??'').localeCompare(b.closing_at??''));
 return <main className="tb-v2-content">
  <p className="tb-v2-eyebrow">Your attention</p><h1 className="tb-v2-heading mt-2">Deadline alerts</h1>
  <p className="tb-v2-muted mt-2">In-app alerts for saved tenders closing in the next {days} days. These are checked when you open TenderBase; Android background notifications are not enabled yet.</p>
  {!ready?<div className="tb-v2-nodata mt-6">Loading alerts…</div>:<>
   <div className="tb-v2-topbar mt-6 mb-3"><span className="font-semibold text-navy text-[15px]">{matches.length} active alert(s)</span><button className="tb-v2-secondary" disabled={!data.dismissedAlerts.length} type="button" onClick={resetAlerts}><RotateCcw size={15}/> Restore dismissed</button></div>
   {!matches.length?<div className="tb-v2-nodata">No saved tender deadlines need attention right now.<div className="mt-4"><Link className="tb-v2-primary" href="/saved">View saved tenders</Link></div></div>:
    <div className="tb-v2-list">{matches.map(t=><article className="tb-v2-panel" key={t.id}>
     <p className="tb-v2-eyebrow flex items-center gap-2"><Bell size={14}/> Closing in {daysUntil(t.closing_at)} day(s)</p>
     <Link href={'/tenders/'+encodeURIComponent(t.id)} className="block font-semibold text-navy mt-2">{t.title}</Link>
     <p className="tb-v2-muted mt-2">Closes {fmtDate(t.closing_at)} · {t.buyer??'Buyer not supplied'}</p>
     <div className="flex flex-wrap gap-2 mt-3"><CalendarButton tender={t}/><button type="button" className="tb-v2-secondary" onClick={()=>dismissAlert(t.id)}><Check size={15}/> Dismiss</button></div>
    </article>)}</div>}
  </>}
 </main>;
}
