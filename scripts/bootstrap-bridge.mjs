import {normalizePortalOpportunity,validatePortalPage} from './normalize-portal.mjs';
const root='https://www.etenders.gov.za/Home/PaginatedTenderOpportunities';
const maxPages=30;
let start=0,emitted=0;
for(let pageNo=1;pageNo<=maxPages;pageNo++){
  const url=new URL(root);
  Object.entries({draw:'1',start:String(start),length:'100',status:'1','search[value]':'','search[regex]':'false'}).forEach(([k,v])=>url.searchParams.set(k,v));
  const response=await fetch(url,{signal:AbortSignal.timeout(45000),headers:{accept:'application/json, text/javascript, */*; q=0.01','x-requested-with':'XMLHttpRequest','user-agent':'TenderBaseV2/0.1 bootstrap source verification',referer:'https://www.etenders.gov.za/Home/opportunities?id=1'}});
  if(!response.ok)throw new Error('Official eTenders HTTP '+response.status+' on page '+pageNo);
  const data=validatePortalPage(await response.json());
  const records=data.rows.map(row=>{
    const t=normalizePortalOpportunity(row);
    if(!t)return null;
    const asString=v=>typeof v==='string'&&v.trim()?v.trim():null;
    const source=row&&typeof row==='object'?row:{};
    const briefing=source.compulsory_briefing_session??source.briefingVenue;
    const briefingDate=briefing&&typeof briefing==='object'?briefing.date:null;
    const briefingVenue=source.briefingVenue;
    t.raw={source:'National Treasury eTenders official opportunities',tender:{
      briefingSession:{date:asString(briefingDate),venue:asString(briefingVenue)},
      contactPerson:{name:asString(source.contactPerson),email:asString(source.email),telephone:asString(source.telephone)}
    }};
    return t;
  }).filter(Boolean);
  for(let i=0;i<records.length;i+=10){
    const chunk=records.slice(i,i+10);
    console.log('TENDERBASE_IMPORT_CHUNK='+JSON.stringify(chunk));
  }
  emitted+=records.length;start+=data.rows.length;
  console.log('TENDERBASE_PAGE '+pageNo+' '+data.rows.length+' official results, imported '+emitted+' / '+data.total);
  if(!data.rows.length||data.rows.length<100||(data.total>0&&start>=data.total))break;
  if(pageNo===maxPages)throw new Error('Stopped at pagination safety cap');
  await new Promise(r=>setTimeout(r,200));
}
if(emitted<1)throw new Error('No real records from official source');
console.log('TENDERBASE_IMPORT_COMPLETED '+emitted);
