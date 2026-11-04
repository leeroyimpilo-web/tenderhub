import Link from 'next/link';
import {ArrowRight,CalendarClock,Search,TrendingUp,Clock3,Database,ShieldCheck} from 'lucide-react';
import {getDashboard} from '@/lib/tenders';
import {fmtDate,fmtNumber} from '@/lib/format';
import {TenderCard} from '@/components/TenderCard';
import {DataError,Empty} from '@/components/DataState';
export const dynamic='force-dynamic';
export default async function TodayPage(){
 let data:Awaited<ReturnType<typeof getDashboard>>|null=null;
 try{data=await getDashboard()}catch(error){console.error('[today]',error)}
 return <main>
  <div className="tb-v2-hero"><div className="tb-v2-hero-inner">
   <div className="flex items-center gap-2"><ShieldCheck size={16} className="text-blue-200"/><span className="uppercase tracking-[.12em] text-[10px] font-bold text-blue-200/70">National Treasury eTenders</span></div>
   <p className="mt-6 text-[12px] text-blue-100/65">Your tender department</p>
   <h1 className="mt-2">Know what matters.<br/>Do the next right thing.</h1>
   <p className="mt-3 max-w-[560px] text-[13px] text-white/65 leading-relaxed">Search real procurement notices, follow deadlines and keep your bids organised — no account needed.</p>
   <div className="mt-6 flex gap-2 flex-wrap"><Link href="/search" className="tb-v2-secondary"><Search size={16}/> Find tenders</Link><Link href="/research" className="inline-flex items-center gap-2 border border-white/20 rounded-xl px-4 py-2.5 text-white text-[13px] font-semibold">Research <ArrowRight size={15}/></Link></div>
  </div></div>
  <div className="tb-v2-content relative -mt-5">
   {!data?<DataError/>:<>
    <div className="tb-v2-grid">
     {([{label:'Records imported',value:data.stats.total,icon:Database},{label:'Open deadlines',value:data.stats.open,icon:TrendingUp},{label:'Closing within 7 days',value:data.stats.closing,icon:Clock3}]).map(({label,value,icon:Icon})=><div className="tb-v2-panel" key={label}><div className="flex items-center justify-between"><p className="tb-v2-eyebrow">{label}</p><Icon size={18} className="text-blue"/></div><p className="text-[27px] font-semibold text-navy mt-3">{fmtNumber(Number(value))}</p></div>)}
    </div>
    <div className="mt-5 flex items-center gap-2 text-[11px] text-ink-2"><Database size={14}/><span>{data.lastSync?'Last successful official sync: '+fmtDate(data.lastSync):'No successful data sync yet. The importer must run before live records appear.'}</span></div>
    <section className="mt-8"><div className="tb-v2-topbar mb-4"><div><p className="tb-v2-eyebrow">Your radar</p><h2 className="text-[21px] font-semibold text-navy">Closing soon</h2></div><Link className="text-[12px] font-semibold text-blue" href="/calendar">Calendar →</Link></div>
     {data.closing.length?<div className="tb-v2-list">{data.closing.map(t=><TenderCard key={t.id} tender={t}/>)}</div>:<Empty message="No upcoming closing dates in the imported eTenders records."/>}
    </section>
    <section className="mt-9"><div className="tb-v2-topbar mb-4"><div><p className="tb-v2-eyebrow">Market activity</p><h2 className="text-[21px] font-semibold text-navy">Recently published</h2></div><Link className="text-[12px] font-semibold text-blue" href="/search">View all →</Link></div>
     {data.latest.length?<div className="tb-v2-list">{data.latest.map(t=><TenderCard key={t.id} tender={t}/>)}</div>:<Empty message="No tender records are imported yet. Run the official eTenders synchronisation."/>}
    </section>
   </>}
  </div>
 </main>;
}
