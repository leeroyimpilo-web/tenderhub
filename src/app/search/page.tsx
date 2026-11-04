import type {Metadata} from 'next';
import Link from 'next/link';
import {Search,SlidersHorizontal} from 'lucide-react';
import {getFacets,searchTenders} from '@/lib/tenders';
import type {TenderFilter,TenderPage} from '@/lib/types';
import {TenderCard} from '@/components/TenderCard';
import {DataError,Empty} from '@/components/DataState';
import {SearchPreference} from '@/components/SearchPreference';
export const metadata:Metadata={title:'Find tenders'};
export const dynamic='force-dynamic';
type Params={q?:string;province?:string;category?:string;buyer?:string;status?:string;sort?:string;page?:string};
export default async function SearchPage({searchParams}:{searchParams:Params}){
 const f:TenderFilter={q:searchParams.q??'',province:searchParams.province??'',category:searchParams.category??'',buyer:searchParams.buyer??'',status:searchParams.status??'open',sort:searchParams.sort??'newest',page:Number(searchParams.page??1)};
 let result:TenderPage|null=null,facets:{provinces:string[];categories:string[]}={provinces:[],categories:[]};
 try{[result,facets]=await Promise.all([searchTenders(f),getFacets()])}catch(error){console.error('[find]',error)}
 const href=(page:number)=>{const p=new URLSearchParams();for(const [k,v] of Object.entries({...f,page})){if(v!==undefined&&v!==null&&String(v))p.set(k,String(v))}return '/search?'+p.toString()};
 return <main className="tb-v2-content"><SearchPreference/>
  <div className="flex gap-2 items-center"><span className="tb-v2-eyebrow">Official opportunities</span><SlidersHorizontal size={14} className="text-ink-3"/></div>
  <h1 className="tb-v2-heading mt-2">Find tenders</h1>
  <p className="tb-v2-muted mt-2">Search only notices stored from National Treasury eTenders OCDS releases.</p>
  <form action="/search" method="get" className="tb-v2-panel mt-5">
   <div className="flex flex-col sm:flex-row gap-2"><label className="grow"><span className="tb-v2-label">Keywords or reference</span><input name="q" defaultValue={f.q} className="tb-v2-field" placeholder="Search title, buyer or reference"/></label><label className="grow"><span className="tb-v2-label">Buyer / department</span><input name="buyer" defaultValue={f.buyer} className="tb-v2-field" placeholder="Department name"/></label></div>
   <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 mt-3">
    <label><span className="tb-v2-label">Province</span><select name="province" defaultValue={f.province} className="tb-v2-field"><option value="">All provinces</option>{facets.provinces.map(x=><option key={x} value={x}>{x}</option>)}</select></label>
    <label><span className="tb-v2-label">Category</span><select name="category" defaultValue={f.category} className="tb-v2-field"><option value="">All categories</option>{facets.categories.map(x=><option key={x} value={x}>{x}</option>)}</select></label>
    <label><span className="tb-v2-label">Deadline</span><select name="status" defaultValue={f.status} className="tb-v2-field"><option value="open">Open deadlines</option><option value="">All dates</option><option value="closed">Closed deadlines</option><option value="undated">Not specified</option></select></label>
    <label><span className="tb-v2-label">Sort</span><select name="sort" defaultValue={f.sort} className="tb-v2-field"><option value="newest">Recently published</option><option value="closing">Closing earliest</option><option value="oldest">Oldest first</option></select></label>
   </div>
   <div className="mt-4 flex flex-wrap gap-2"><button type="submit" className="tb-v2-primary"><Search size={16}/> Search</button><Link href="/search" className="tb-v2-secondary">Reset filters</Link></div>
  </form>
  <div className="tb-v2-topbar mt-7 mb-3"><h2 className="text-[17px] font-semibold text-navy">Results</h2>{result&&<span className="tb-v2-muted">{result.total.toLocaleString('en-ZA')} records</span>}</div>
  {!result?<DataError/>:result.items.length?<div className="tb-v2-list">{result.items.map(t=><TenderCard tender={t} key={t.id}/>)}</div>:<Empty/>}
  {result&&result.pages>1&&<nav className="flex items-center justify-between mt-5" aria-label="Result pages">
   {result.page>1?<Link href={href(result.page-1)} className="tb-v2-secondary">Previous page</Link>:<span/>}
   <span className="tb-v2-muted">Page {result.page} of {result.pages}</span>
   {result.page<result.pages?<Link href={href(result.page+1)} className="tb-v2-secondary">Next page</Link>:<span/>}
  </nav>}
 </main>;
}
