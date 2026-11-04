'use client';
import Link from 'next/link';
import {useState} from 'react';
import {ArrowRight,SlidersHorizontal,Search} from 'lucide-react';
import {useLocal} from '@/components/LocalProvider';
import {TenderCard} from '@/components/TenderCard';
import type {Tender} from '@/lib/types';
export function DiscoveryHome({latest,closing,count,lastSync}:{latest:Tender[];closing:Tender[];count:number;lastSync:string|null}){
 const {data}=useLocal();const [tab,setTab]=useState<'for-you'|'latest'|'closing'>('for-you');
 const chosen=tab==='closing'?closing:latest;
 const name=(q:string)=>data.savedSearches.find(s=>s.q===q)?.name??q;
 return <main className="tb-discover">
  <h1>Opportunities<br/>for your business.</h1>
  <p className="tb-discover-sub">National Treasury eTenders</p>
  <form action="/search" className="tb-discover-search"><label><Search size={22}/><input name="q" placeholder="Keyword or tender number" aria-label="Keyword or tender number"/></label><button formAction="/search" aria-label="Refine search"><SlidersHorizontal size={22}/></button></form>
  <div className="tb-tabs"><button className={tab==='for-you'?'active':''} onClick={()=>setTab('for-you')}>For you</button><button className={tab==='latest'?'active':''} onClick={()=>setTab('latest')}>Latest</button><button className={tab==='closing'?'active':''} onClick={()=>setTab('closing')}>Closing soon</button></div>
  {tab==='for-you'&&<div className="tb-preference-strip"><span>{data.settings.industries[0]??'All industries'}{data.settings.defaultProvince?' · '+data.settings.defaultProvince:''}</span><Link href="/onboarding">Edit</Link></div>}
  <section className="tb-saved-searches"><div className="tb-section-heading"><h2>Saved searches</h2><Link href="/saved-searches">View all <ArrowRight size={15}/></Link></div><div className="tb-chip-row">{data.savedSearches.slice(0,3).map(s=><Link href={'/search?'+new URLSearchParams({q:s.q,province:s.province,category:s.category,buyer:s.buyer,status:s.status,sort:s.sort}).toString()} key={s.id}>{s.name}</Link>)}{!data.savedSearches.length&&<Link href="/saved-searches" className="tb-add-search-chip">+ Save your first search</Link>}</div></section>
  <div className="tb-results-heading"><b>{count.toLocaleString('en-ZA')} opportunities</b><Link href="/search">See all tenders <ArrowRight size={15}/></Link></div>
  <p className="tb-source-strip">{lastSync?'Source updated '+new Date(lastSync).toLocaleString('en-ZA',{day:'numeric',month:'short',hour:'2-digit',minute:'2-digit'}):'National Treasury eTenders'}</p>
  {chosen.length?<div className="tb-home-results">{chosen.map((t,i)=><TenderCard key={t.id} tender={t} highlighted={i===0} match={tab==='for-you'?data.settings.industries[0]??'Your selected categories':undefined}/>)}</div>:<div className="tb-v2-nodata">No opportunities are available in this imported dataset yet. Check back after the next official data import.</div>}
 </main>
}
