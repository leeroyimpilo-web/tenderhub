'use client';
import {useState} from 'react';
import Link from 'next/link';
import {Bookmark,Search,Trash2,KanbanSquare,ExternalLink} from 'lucide-react';
import {useLocal} from '@/components/LocalProvider';
import {fmtDate,tenderStatus} from '@/lib/format';
export default function Saved(){
 const {data,ready,toggleSaved,addPipeline,isTracked}=useLocal();const [q,setQ]=useState('');
 const items=data.saved.filter(t=>(t.title+' '+t.reference+' '+t.buyer).toLowerCase().includes(q.toLowerCase()));
 return <main className="tb-v2-content">
  <p className="tb-v2-eyebrow">Your watchlist</p><h1 className="tb-v2-heading mt-2">Saved tenders</h1>
  <p className="tb-v2-muted mt-2">Stored locally on this device. No login or cloud account required.</p>
  {!ready?<div className="tb-v2-nodata mt-5">Loading on-device data…</div>:<>
  <label className="block mt-5"><span className="tb-v2-label">Find a saved tender</span><div className="flex gap-2"><input className="tb-v2-field" value={q} onChange={e=>setQ(e.target.value)} placeholder="Search your watchlist"/><span className="tb-v2-secondary" aria-hidden><Search size={18}/></span></div></label>
  <p className="tb-v2-muted my-4">{items.length} of {data.saved.length} saved tender(s)</p>
  {!items.length?<div className="tb-v2-nodata">{data.saved.length?'No saved tenders match this search.':'Nothing saved yet. Use the Save button on any official tender.'}<div className="mt-4"><Link className="tb-v2-primary" href="/search">Find tenders</Link></div></div>:<div className="tb-v2-list">
   {items.map(t=><article key={t.id} className="tb-v2-panel">
    <p className="tb-v2-eyebrow">{t.reference??t.id} · {tenderStatus(t.closing_at)}</p>
    <Link className="block text-[16px] font-semibold text-navy mt-2 hover:underline" href={'/tenders/'+encodeURIComponent(t.id)}>{t.title} <ExternalLink size={14} className="inline"/></Link>
    <p className="tb-v2-muted mt-2">{t.buyer??'Buyer not supplied'} · Closes {fmtDate(t.closing_at)}</p>
    <div className="flex gap-2 flex-wrap mt-4"><button className="tb-v2-secondary" type="button" onClick={()=>toggleSaved(t)}><Trash2 size={15}/> Remove save</button><button className="tb-v2-secondary" disabled={isTracked(t.id)} type="button" onClick={()=>addPipeline(t)}><KanbanSquare size={15}/> {isTracked(t.id)?'Already tracking':'Track bid'}</button></div>
   </article>)}
  </div>}
  </>}
 </main>;
}
