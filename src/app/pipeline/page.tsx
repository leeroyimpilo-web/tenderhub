'use client';
import Link from 'next/link';
import {KanbanSquare,Trash2,ArrowUpRight,CheckCircle2} from 'lucide-react';
import {useLocal} from '@/components/LocalProvider';
import {STAGES,PipelineStage} from '@/lib/types';
import {fmtDate} from '@/lib/format';
export default function Pipeline(){
 const {data,ready,setStage,setNotes,removePipeline}=useLocal();
 return <main className="tb-v2-content">
  <p className="tb-v2-eyebrow">Bid management</p><h1 className="tb-v2-heading mt-2">Pipeline</h1>
  <p className="tb-v2-muted mt-2">Manage stages and private notes on this device. Changes save automatically.</p>
  {!ready?<div className="tb-v2-nodata mt-6">Loading your pipeline…</div>:!data.pipeline.length?
   <div className="tb-v2-nodata mt-6"><KanbanSquare className="mx-auto mb-3" size={24}/>No tenders being tracked.<div className="mt-4"><Link className="tb-v2-primary" href="/search">Find an opportunity</Link></div></div>:
   <div className="tb-v2-list mt-6">{data.pipeline.map(({tender,stage,notes,updatedAt})=><article key={tender.id} className="tb-v2-panel">
    <p className="tb-v2-eyebrow">{tender.reference??tender.id}</p>
    <Link href={'/tenders/'+encodeURIComponent(tender.id)} className="block text-[16px] font-semibold text-navy mt-2 hover:underline">{tender.title} <ArrowUpRight size={15} className="inline"/></Link>
    <p className="tb-v2-muted mt-2">{tender.buyer??'No buyer supplied'} · Closes {fmtDate(tender.closing_at)}</p>
    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-4">
     <label><span className="tb-v2-label">Bid stage</span><select className="tb-v2-field" value={stage} onChange={e=>setStage(tender.id,e.target.value as PipelineStage)}>{STAGES.map(s=><option key={s} value={s}>{s}</option>)}</select></label>
     <label><span className="tb-v2-label">Your notes (autosaved)</span><textarea className="tb-v2-field min-h-[82px]" rows={3} placeholder="Bid preparation notes" value={notes} onChange={e=>setNotes(tender.id,e.target.value)}/></label>
    </div>
    <div className="flex flex-wrap items-center justify-between gap-3 mt-3"><span className="text-[11px] text-ink-3 flex items-center gap-1"><CheckCircle2 size={13}/> Saved on device · {fmtDate(updatedAt)}</span><button type="button" className="tb-v2-secondary" onClick={()=>{if(window.confirm('Remove this tender from your pipeline?'))removePipeline(tender.id)}}><Trash2 size={15}/> Remove</button></div>
   </article>)}</div>}
 </main>;
}
