import fs from 'node:fs/promises';
import path from 'node:path';
import {gzipSync} from 'node:zlib';
const base=new URL(process.env.TENDERBASE_API_URL || 'https://tenderhub-v2.onrender.com');
if(base.protocol!=='https:')throw Error('HTTPS endpoint required');
async function get(pathname) {
 let last;
 for(let attempt=0;attempt<4;attempt++){
  try {
   const r=await fetch(new URL(pathname,base),{signal:AbortSignal.timeout(60000),
    headers:{Accept:'application/json'}});
   if(!r.ok)throw Error('HTTP '+r.status+' '+pathname);
   return await r.json();
  }catch(e){last=e;if(attempt<3)await new Promise(r=>setTimeout(r,1800*(attempt+1)))}
 }
 throw last;
}
const health=await get('/api/health');
if(!health.ok || !health.lastSync || health.count<1000)throw Error('No verified official tender database');
const items=[],ids=new Set();
let expected=-1;
for(let page=1;page<=250;page++){
 const batch=await get('/api/tenders?pageSize=50&page='+page);
 if(!Array.isArray(batch.items)||!Number.isInteger(batch.total))throw Error('Invalid tender page');
 if(expected<0)expected=batch.total;
 if(batch.total!==expected)throw Error('Tender count changed during snapshot');
 for(const t of batch.items){
  if(!t.id||!t.title||!/^https:\/\//.test(t.source_url||''))throw Error('Invalid official record');
  if(ids.has(t.id))throw Error('Duplicate tender '+t.id);
  ids.add(t.id);
  // Snapshot remains the same shape as the existing native format, with five
  // additional fields supplied from the first-party raw portal values.
  items.push({
   id:String(t.id),reference:t.reference??null,title:String(t.title),
   description:t.description??null,buyer:t.buyer??null,province:t.province??null,
   category:t.category??null,status:t.status??null,
   published_at:t.published_at??null,closing_at:t.closing_at??null,
   release_at:t.release_at??null,source_url:t.source_url,
   documents:(t.documents??[]).filter(d=>/^https:\/\//.test(d.url||'')).map(d=>({
    id:String(d.id??''),title:String(d.title??'Document'),
    url:d.url,format:d.format??null,publishedAt:d.publishedAt??null
   })),
   synced_at:t.synced_at??null,
   contact_name:t.contact_name??t.raw?.contactPerson??null,
   contact_email:t.contact_email??t.raw?.email??null,
   contact_phone:t.contact_phone??t.raw?.telephone??null,
   briefing_at:t.briefing_at??t.raw?.compulsory_briefing_session??null,
   briefing_venue:t.briefing_venue??t.raw?.briefingVenue??null
  });
 }
 console.log('Exported official records '+items.length+'/'+expected);
 if(items.length===expected)break;
}
if(expected!==health.count||items.length!==expected||items.length<1000)throw Error('Snapshot incomplete or changed');
const header={type:'tenderbase-offline-bootstrap-v1',
 count:items.length,exportedAt:new Date().toISOString(),lastServerSync:health.lastSync,
 source:'National Treasury eTenders'};
const zipped=gzipSync(Buffer.from([JSON.stringify(header),
 ...items.map(t=>JSON.stringify(t))].join('\n')+'\n'),{level:9});
const output=path.resolve('native-android/app/src/main/assets');
await fs.mkdir(output,{recursive:true});
await fs.writeFile(path.join(output,'bootstrap.tenderpack'),zipped);
await fs.writeFile(path.join(output,'snapshot-info.json'),JSON.stringify({...header,compressedBytes:zipped.length},null,2));
console.log('Verified '+items.length+' official records / '+zipped.length+' compressed bytes');
