import type {Metadata} from 'next';
import Link from 'next/link';
import {ArrowDown} from 'lucide-react';
import {getFacets,searchTenders} from '@/lib/tenders';
import type {TenderFilter,TenderPage} from '@/lib/types';
import {TenderCard} from '@/components/TenderCard';
import {DataError,Empty} from '@/components/DataState';
import {SearchPreference} from '@/components/SearchPreference';
import {SearchControls} from '@/components/SearchControls';
export const metadata:Metadata={title:'Discover tenders'};export const dynamic='force-dynamic';
type Params={q?:string;province?:string;category?:string;buyer?:string;status?:string;sort?:string;page?:string};
export default async function SearchPage({searchParams}:{searchParams:Params}){
 const f:TenderFilter={q:searchParams.q??'',province:searchParams.province??'',category:searchParams.category??'',buyer:searchParams.buyer??'',status:searchParams.status??'open',sort:searchParams.sort??'newest',page:Number(searchParams.page??1)};
 let result:TenderPage|null=null,facets:{provinces:string[];categories:string[]}={provinces:[],categories:[]};try{[result,facets]=await Promise.all([searchTenders(f),getFacets()])}catch(error){console.error('[discover search]',error)}
 const href=(page:number)=>{const p=new URLSearchParams();for(const [k,v] of Object.entries({...f,page})){if(v!==undefined&&v!==null&&String(v))p.set(k,String(v))}return '/search?'+p.toString()};
 return <main className="tb-discover tb-search-results"><SearchPreference/><h1>Find your next<br/>opportunity.</h1><p className="tb-discover-sub">National Treasury eTenders</p><SearchControls filters={{q:f.q??'',province:f.province??'',category:f.category??'',buyer:f.buyer??'',status:f.status??'',sort:f.sort??'newest'}} provinces={facets.provinces} categories={facets.categories}/><p className="tb-source-strip">Official source · Imported tender notices</p><div className="tb-page-tabs"><Link className="active" href="/search">Latest</Link><Link href={href(1).replace('sort=newest','sort=closing')}>Closing soon</Link></div><div className="tb-results-heading"><b>{result?.total.toLocaleString('en-ZA')??'—'} opportunities</b><Link href={href(1).replace('sort=newest','sort=closing')}>Closing earliest <ArrowDown size={15}/></Link></div>
  {!result?<DataError/>:result.items.length?<div className="tb-home-results">{result.items.map(t=><TenderCard tender={t} key={t.id}/>)}</div>:<Empty message="No matching tenders. Adjust your keywords or reset filters to see other notices."/>}
  {result&&result.pages>1&&<nav className="flex items-center justify-between mt-5" aria-label="Result pages">{result.page>1?<Link href={href(result.page-1)} className="tb-v2-secondary">Previous page</Link>:<span/>}<span className="tb-v2-muted">Page {result.page} of {result.pages}</span>{result.page<result.pages?<Link href={href(result.page+1)} className="tb-v2-secondary">Next page</Link>:<span/>}</nav>}
 </main>
}
