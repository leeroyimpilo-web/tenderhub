import pg from 'pg';
import {normalizePortalOpportunity,validatePortalPage} from './normalize-portal.mjs';
if(!process.env.DATABASE_URL){console.error('DATABASE_URL required; run npm run db:init first');process.exit(2)}
const api='https://www.etenders.gov.za/Home/PaginatedTenderOpportunities';
const pageFlag=process.argv.find(a=>a.startsWith('--pages='));
const maxPages=pageFlag?Number(pageFlag.split('=')[1]):200;
if(!Number.isInteger(maxPages)||maxPages<1||maxPages>200){console.error('Use --pages=1..200');process.exit(2)}
const client=new pg.Client({connectionString:process.env.DATABASE_URL});
let runId=null,pages=0,releases=0;
async function page(start){
 const url=new URL(api);
 Object.entries({draw:'1',start:String(start),length:'100',status:'1','search[value]':'','search[regex]':'false'}).forEach(([k,v])=>url.searchParams.set(k,v));
 for(let attempt=1;attempt<=3;attempt++){
  try{
   const res=await fetch(url,{signal:AbortSignal.timeout(30000),headers:{accept:'application/json, text/javascript, */*; q=0.01','x-requested-with':'XMLHttpRequest','user-agent':'TenderBaseV2/0.1 National Treasury portal importer',referer:'https://www.etenders.gov.za/Home/opportunities?id=1'}});
   if(!res.ok)throw new Error('Official eTenders opportunities HTTP '+res.status+': '+(await res.text()).slice(0,200));
   return validatePortalPage(await res.json());
  }catch(err){if(attempt===3)throw err;await new Promise(r=>setTimeout(r,1500*attempt))}
 }
 throw new Error('Portal request failed');
}
const sql=String.raw`INSERT INTO tenders(id,reference,title,description,buyer,province,category,status,published_at,closing_at,release_at,source_url,documents,raw,synced_at)
VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13::jsonb,$14::jsonb,now())
ON CONFLICT(id) DO UPDATE SET
 reference=EXCLUDED.reference,title=EXCLUDED.title,description=EXCLUDED.description,buyer=EXCLUDED.buyer,
 province=EXCLUDED.province,category=EXCLUDED.category,status=EXCLUDED.status,published_at=EXCLUDED.published_at,
 closing_at=EXCLUDED.closing_at,release_at=EXCLUDED.release_at,source_url=EXCLUDED.source_url,
 documents=EXCLUDED.documents,raw=EXCLUDED.raw,synced_at=now()`;
try{
 await client.connect();
 const r=await client.query("INSERT INTO sync_runs(source,success) VALUES($1,NULL) RETURNING id",['National Treasury official eTenders opportunities']);runId=r.rows[0].id;
 let start=0;
 for(let i=0;i<maxPages;i++){
  const batch=await page(start);pages++;
  for(const value of batch.rows){
   const t=normalizePortalOpportunity(value);if(!t)continue;
   await client.query(sql,[t.id,t.reference,t.title,t.description,t.buyer,t.province,t.category,t.status,t.published_at,t.closing_at,t.release_at,t.source_url,JSON.stringify(t.documents),JSON.stringify(t.raw)]);
   releases++;
  }
  console.log('Official portal page '+(i+1)+': '+batch.rows.length+' records; total='+batch.total);
  start+=batch.rows.length;
  if(!batch.rows.length||batch.rows.length<100||(batch.total&&start>=batch.total))break;
  if(i===maxPages-1 && maxPages===200)throw new Error('Exceeded pagination safety limit');
  await new Promise(r=>setTimeout(r,250));
 }
 if(releases===0)throw new Error('Zero valid notices imported. Refusing to mark sync successful.');
 await client.query('UPDATE sync_runs SET success=true,finished_at=now(),pages=$2,releases=$3 WHERE id=$1',[runId,pages,releases]);
 console.log('SUCCESS: '+releases+' genuine official eTenders opportunity rows indexed.');
}catch(err){
 console.error('Official portal sync failed:',err);
 if(runId!==null)await client.query('UPDATE sync_runs SET success=false,finished_at=now(),pages=$2,releases=$3,error=$4 WHERE id=$1',[runId,pages,releases,String(err).slice(0,1800)]).catch(()=>{});
 process.exitCode=1;
}finally{await client.end()}
