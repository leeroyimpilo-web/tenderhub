import Link from 'next/link';
import type {Metadata} from 'next';
import {notFound} from 'next/navigation';
import {ExternalLink,ArrowLeft,Building2,MapPin,CalendarClock,FileText} from 'lucide-react';
import {getTender} from '@/lib/tenders';
import {fmtDate,tenderStatus} from '@/lib/format';
import {DataError} from '@/components/DataState';
import {SaveButton,TrackButton,CalendarButton,ShareButton} from '@/components/TenderActions';
export const dynamic='force-dynamic';
export const metadata:Metadata={title:'Tender details'};
function Text({title,value}:{title:string;value:string|null|undefined}){return <div className="py-3 border-b border-line last:border-0"><p className="tb-v2-label">{title}</p><p className="text-[13px] text-ink break-words">{value||'Not supplied in the official record'}</p></div>}
export default async function Details({params}:{params:{id:string}}){
 let tender:Awaited<ReturnType<typeof getTender>>|undefined;
 try{tender=await getTender(params.id)}catch(error){console.error('[tender details]',error)}
 if(tender===undefined)return <main className="tb-v2-content"><DataError/></main>;
 if(tender===null)notFound();
 const t=tender;
 const raw=(t.raw?.tender??{}) as Record<string,any>;
 const briefing=(raw.briefingSession??{}) as Record<string,unknown>;
 const contact=(raw.contactPerson??{}) as Record<string,unknown>;
 return <main className="tb-v2-content">
  <Link href="/search" className="inline-flex items-center gap-2 text-[12px] font-semibold text-blue mb-5"><ArrowLeft size={15}/> Back to search</Link>
  <div className="tb-v2-panel">
   <div className="flex gap-2 flex-wrap items-center justify-between mb-3"><span className={'tb-v2-badge '+(tenderStatus(t.closing_at)==='Open'?'open':'')}>{tenderStatus(t.closing_at)}</span><span className="text-[11px] text-ink-2 break-all">{t.reference??t.id}</span></div>
   <h1 className="text-[23px] font-semibold text-navy leading-snug tracking-[-.035em]">{t.title}</h1>
   <p className="tb-v2-muted mt-3">{t.description??'The publisher did not include a description.'}</p>
   <div className="flex flex-wrap gap-2 mt-5"><SaveButton tender={t}/><TrackButton tender={t}/><CalendarButton tender={t}/><ShareButton tender={t}/></div>
  </div>
  <div className="grid grid-cols-1 lg:grid-cols-[1.25fr_.75fr] gap-4 mt-4">
   <section className="tb-v2-panel">
    <h2 className="font-semibold text-navy text-[17px] mb-2">Official notice details</h2>
    <Text title="Buyer / department" value={t.buyer}/><Text title="Province" value={t.province}/><Text title="Category" value={t.category}/><Text title="Notice status (publisher)" value={t.status}/><Text title="Tender reference" value={t.reference}/>
    <Text title="Published" value={fmtDate(t.published_at)}/><Text title="Closing date" value={fmtDate(t.closing_at)}/><Text title="Briefing date" value={typeof briefing.date==='string'?fmtDate(briefing.date):null}/><Text title="Briefing venue" value={typeof briefing.venue==='string'?briefing.venue:null}/>
    <Text title="Contact name" value={typeof contact.name==='string'?contact.name:null}/><Text title="Contact email" value={typeof contact.email==='string'?contact.email:null}/><Text title="Contact telephone" value={typeof contact.telephone==='string'?contact.telephone:null}/>
   </section>
   <div className="flex flex-col gap-4">
    <section className="tb-v2-panel">
     <h2 className="font-semibold text-navy text-[17px] mb-3">Tender documents ({t.documents.length})</h2>
     {t.documents.length?<div className="tb-v2-list">{t.documents.map((d,i)=><a key={d.id+'-'+i} href={d.url} target="_blank" rel="noreferrer" className="flex items-center justify-between gap-3 border-b border-line py-2 text-[12px] text-blue hover:underline"><span className="flex items-center gap-2 break-all"><FileText size={17} className="shrink-0"/>{d.title}</span><ExternalLink size={14} className="shrink-0"/></a>)}</div>:<p className="tb-v2-muted">The OCDS release does not provide any document download links. Check the publisher notice.</p>}
    </section>
    <section className="tb-v2-panel">
     <h2 className="font-semibold text-navy text-[17px] mb-3">Source & verification</h2>
     <p className="tb-v2-muted">OCDS ID: <span className="break-all">{t.id}</span></p>
     <p className="tb-v2-muted mt-2">Locally indexed: {fmtDate(t.synced_at)}</p>
     <a href="https://www.etenders.gov.za/Home/opportunities?id=1" target="_blank" rel="noreferrer" className="tb-v2-primary mt-4"><ExternalLink size={16}/> Open official eTenders portal</a>
     <p className="tb-v2-muted mt-3">Source information is provided by National Treasury and should be checked with the procuring entity.</p>
    </section>
   </div>
  </div>
 </main>;
}
