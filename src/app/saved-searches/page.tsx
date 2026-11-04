'use client';
import Link from 'next/link';
import {useState} from 'react';
import {useSearchParams} from 'next/navigation';
import {ArrowRight,MoreHorizontal,Trash2} from 'lucide-react';
import {useLocal} from '@/components/LocalProvider';
export default function SavedSearches(){
 const {data,saveSearch,removeSearch,renameSearch}=useLocal();const params=useSearchParams();const [name,setName]=useState('');
 const query={q:params.get('q')??'',province:params.get('province')??data.settings.defaultProvince,category:params.get('category')??'',buyer:params.get('buyer')??'',status:params.get('status')??'open',sort:params.get('sort')??'closing'};
 const run=(s:typeof data.savedSearches[number])=>'/search?'+new URLSearchParams({q:s.q,province:s.province,category:s.category,buyer:s.buyer,status:s.status,sort:s.sort}).toString();
 return <main className="tb-page-content"><p className="tb-back-label">Discover</p><h1 className="tb-page-title">Saved searches.</h1><p className="tb-page-subtitle">Your filters, ready when you need them.</p>
  <div className="tb-saved-search-list">{data.savedSearches.map((s,i)=><article className={'tb-saved-search-card '+(i===0?'featured':'')} key={s.id}><div className="tb-saved-search-title"><h2>{s.name}</h2><button aria-label={'Options for '+s.name} onClick={()=>{const n=window.prompt('Rename saved search',s.name);if(n?.trim())renameSearch(s.id,n.trim())}}><MoreHorizontal size={22}/></button></div><div className="tb-search-tags">{s.q&&<span>{s.q}</span>}<span>{s.province||'All provinces'}</span><span>{s.status==='open'?'Open':s.status||'Any date'}</span></div><div className="tb-search-card-bottom"><span>{s.sort==='closing'?'Closing earliest':'Recently published'}</span><Link href={run(s)}>Run search <ArrowRight size={17}/></Link></div><button className="tb-search-delete" onClick={()=>removeSearch(s.id)} aria-label={'Delete '+s.name}><Trash2 size={17}/></button></article>)}{!data.savedSearches.length&&<div className="tb-empty-inline">No saved searches yet. Save a search to keep its filters ready.</div>}</div>
  <section className="tb-save-search-form"><h2>Save current search</h2><input value={name} onChange={e=>setName(e.target.value)} placeholder="Water and drilling tenders" aria-label="Saved search name"/><p>Uses the current keywords and filters.</p><button onClick={()=>{const label=name.trim()||query.q||'Tender search';saveSearch({...query,name:label});setName('')}}>Save search</button></section>
 </main>
}
