import fs from 'node:fs/promises';
import path from 'node:path';
import {gzipSync} from 'node:zlib';
const root = process.env.TENDERBASE_API_URL || 'https://tenderhub-v2.onrender.com';
const endpoint = new URL(root);
if(endpoint.protocol !== 'https:') throw new Error('Official source must use HTTPS');
async function getJson(pathname){
 let error;
 for(let retry=0;retry<4;retry++){
  try{
   const r=await fetch(new URL(pathname,endpoint),{headers:{Accept:'application/json'},signal:AbortSignal.timeout(60000)});
   if(!r.ok)throw Error('HTTP '+r.status+' for '+pathname);
   return await r.json();
  }catch(e){error=e;await new Promise(resolve=>setTimeout(resolve,(retry+1)*1800))}
 }
 throw error;
}
const health=await getJson('/api/health');
if(!health.ok||!health.lastSync||health.count<1000) throw Error('No verified official dataset at API');
const rows=[],seen=new Set();
const pageSize=50;
let expectedTotal=null;
for(let page=1;page<=250;page++){
 const batch=await getJson('/api/tenders?pageSize=50&page='+page);
 if(!Array.isArray(batch.items)||!Number.isInteger(batch.total))throw Error('Malformed tender page '+page);
 if(expectedTotal===null)expectedTotal=batch.total;
 if(batch.total!==expectedTotal)throw Error('Dataset changed during export, retry build');
 if(!batch.items.length && rows.length<expectedTotal)throw Error('Premature empty page '+page);
 for(const t of batch.items){
  if(typeof t.id!=='string'||!t.id||typeof t.title!=='string'||!t.title.trim())throw Error('Missing official tender identifier/title');
  if(seen.has(t.id))throw Error('Duplicate tender id '+t.id+' (unstable pagination)');
  if(typeof t.source_url!=='string'||!/^https:\/\/[^/]+/.test(t.source_url))throw Error('Invalid source URL for '+t.id);
  seen.add(t.id);
  rows.push({
   id:t.id,reference:t.reference??null,title:t.title,description:t.description??null,
   buyer:t.buyer??null,province:t.province??null,category:t.category??null,
   status:t.status??null,published_at:t.published_at??null,closing_at:t.closing_at??null,
   release_at:t.release_at??null,source_url:t.source_url,
   documents:(Array.isArray(t.documents)?t.documents:[]).map(d=>({
    id:String(d.id??''),title:String(d.title??'Document'),url:String(d.url??''),
    format:d.format??null,publishedAt:d.publishedAt??null
   })).filter(d=>/^https:\/\//.test(d.url)),
   synced_at:t.synced_at??null
  });
 }
 console.log('Official tenders exported: '+rows.length+'/'+expectedTotal);
 if(rows.length===expectedTotal)break;
}
if(rows.length!==expectedTotal||rows.length<1000||rows.length!==health.count)
 throw Error('Incomplete snapshot '+rows.length+' expected '+expectedTotal+' (health '+health.count+')');
const timestamp=new Date().toISOString();
const header={type:'tenderbase-offline-bootstrap-v1',count:rows.length,exportedAt:timestamp,lastServerSync:health.lastSync,source:'National Treasury eTenders via TenderBase'};
const lines=[JSON.stringify(header),...rows.map(t=>JSON.stringify(t))].join('\n')+'\n';
const zipped=gzipSync(Buffer.from(lines),{level:9});
const dest=path.resolve('native-android/app/src/main/assets');
await fs.mkdir(dest,{recursive:true});
await fs.writeFile(path.join(dest,'bootstrap.jsonl.gz'),zipped);
await fs.writeFile(path.join(dest,'snapshot-info.json'),JSON.stringify({...header,compressedBytes:zipped.length},null,2));
console.log('Verified official snapshot: '+rows.length+' records, '+zipped.length+' gzip bytes, updated '+health.lastSync);
