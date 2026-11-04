import type {Metadata} from 'next';
import {getResearch} from '@/lib/tenders';
import {fmtNumber} from '@/lib/format';
import {DataError,Empty} from '@/components/DataState';
export const metadata:Metadata={title:'Research'};
export const dynamic='force-dynamic';
function Group({title,rows}:{title:string;rows:{name:string;count:number}[]}){
 const top=rows[0]?.count||1;
 return <section className="tb-v2-panel">
  <h2 className="text-[17px] font-semibold text-navy mb-4">{title}</h2>
  {rows.length?rows.map((r)=><div key={r.name} className="mb-4 last:mb-0">
   <div className="flex justify-between gap-3 text-[11px] mb-1"><span className="text-ink-2 truncate">{r.name}</span><b className="text-navy">{fmtNumber(r.count)}</b></div>
   <div className="h-[7px] rounded-full bg-blue-soft overflow-hidden"><div className="h-full bg-blue rounded-full" style={{width:Math.max(3,r.count/top*100)+'%'}}/></div>
  </div>):<Empty message="No classified tenders in the database."/>}
 </section>;
}
export default async function Research(){
 let result:Awaited<ReturnType<typeof getResearch>>|null=null;
 try{result=await getResearch()}catch(error){console.error('[research]',error)}
 return <main className="tb-v2-content">
  <p className="tb-v2-eyebrow">Market intelligence</p>
  <h1 className="tb-v2-heading mt-2">Research</h1>
  <p className="tb-v2-muted mt-2">Statistics calculated from all currently imported National Treasury records, not from example data.</p>
  {!result?<div className="mt-6"><DataError/></div>:<>
   <div className="tb-v2-panel my-6"><p className="tb-v2-eyebrow">Total indexed procurement records</p><p className="text-[32px] font-semibold text-navy mt-2">{fmtNumber(result.total)}</p></div>
   <div className="grid grid-cols-1 lg:grid-cols-2 gap-4"><Group title="Most active buyers" rows={result.buyers}/><Group title="Tenders by province" rows={result.provinces}/><Group title="Procurement categories" rows={result.categories}/></div>
   <p className="tb-v2-muted mt-5">Counts reflect only the official eTenders records successfully imported into TenderBase. Not all national procurement processes are published in this dataset.</p>
  </>}
 </main>;
}
