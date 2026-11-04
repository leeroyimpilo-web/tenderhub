import pg from 'pg';
import {normalizeRelease,releaseArray} from './normalize-ocds.mjs';
const base=(process.env.ETENDERS_BASE_URL??'https://ocds-api.etenders.gov.za').replace(/\/+$/,'');
const rawDays=process.argv.find(a=>a.startsWith('--days='));
const days=rawDays?Number(rawDays.split('=')[1]):Number(process.env.SYNC_LOOKBACK_DAYS??7);
if(!Number.isInteger(days)||days<1||days>365){console.error('Use --days=1..365');process.exit(2)}
if(!process.env.DATABASE_URL){console.error('DATABASE_URL required; run npm run db:init first');process.exit(2)}
const pgClient=new pg.Client({connectionString:process.env.DATABASE_URL});
let runId=null,pages=0,releases=0;
const iso=(d)=>d.toISOString().slice(0,10);
const addDays=(d,n)=>{const x=new Date(d);x.setUTCDate(x.getUTCDate()+n);return x};
async function loadPage(from,to,page){
  const url=new URL('/api/OCDSReleases',base);
  for(const [k,v] of Object.entries({dateFrom:iso(from),dateTo:iso(to),PageNumber:page,PageSize:100}))url.searchParams.set(k,String(v));
  for(let attempt=0;attempt<3;attempt++){
    try{
      const controller=new AbortController();
      const timeout=setTimeout(()=>controller.abort(),45000);
      try{
        const response=await fetch(url,{signal:controller.signal,headers:{accept:'application/json','user-agent':'TenderBaseV2/0.1 (public OCDS importer)'}});
        if(!response.ok){const body=await response.text();throw new Error('Official eTenders HTTP '+response.status+': '+body.slice(0,250))}
        return releaseArray(await response.json());
      }finally{clearTimeout(timeout)}
    }catch(err){if(attempt===2)throw err;await new Promise(r=>setTimeout(r,1000*(attempt+1)))}
  }
  throw new Error('Official eTenders request failed');
}
const sql=String.raw`INSERT INTO tenders(id,reference,title,description,buyer,province,category,status,published_at,closing_at,release_at,source_url,documents,raw,synced_at)
VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13::jsonb,$14::jsonb,now())
ON CONFLICT(id) DO UPDATE SET
 reference=EXCLUDED.reference,title=EXCLUDED.title,description=EXCLUDED.description,buyer=EXCLUDED.buyer,
 province=EXCLUDED.province,category=EXCLUDED.category,status=EXCLUDED.status,published_at=EXCLUDED.published_at,
 closing_at=EXCLUDED.closing_at,release_at=EXCLUDED.release_at,source_url=EXCLUDED.source_url,
 documents=EXCLUDED.documents,raw=EXCLUDED.raw,synced_at=now()
WHERE COALESCE(tenders.release_at,'epoch'::timestamptz)<=COALESCE(EXCLUDED.release_at,'epoch'::timestamptz)`;
try{
 await pgClient.connect();
 const created=await pgClient.query("INSERT INTO sync_runs(success) VALUES(NULL) RETURNING id");runId=created.rows[0].id;
 const end=new Date();
 let from=addDays(new Date(Date.UTC(end.getUTCFullYear(),end.getUTCMonth(),end.getUTCDate())),-days+1);
 const stop=new Date(Date.UTC(end.getUTCFullYear(),end.getUTCMonth(),end.getUTCDate()));
 while(from<=stop){
  const to=new Date(Math.min(addDays(from,6).getTime(),stop.getTime()));
  for(let page=1;page<=500;page++){
    const batch=await loadPage(from,to,page);pages++;
    for(const record of batch){
      const t=normalizeRelease(record);if(!t)continue;
      await pgClient.query(sql,[t.id,t.reference,t.title,t.description,t.buyer,t.province,t.category,t.status,t.published_at,t.closing_at,t.release_at,t.source_url,JSON.stringify(t.documents),JSON.stringify(t.raw)]);
      releases++;
    }
    console.log('Window '+iso(from)+' to '+iso(to)+' / page '+page+': '+batch.length+' releases');
    if(batch.length<100)break;
    if(page===500)throw new Error('Exceeded safe 500 pages in one date window');
  }
  from=addDays(to,1);
 }
 await pgClient.query('UPDATE sync_runs SET success=true,finished_at=now(),pages=$2,releases=$3 WHERE id=$1',[runId,pages,releases]);
 console.log('Synced '+releases+' real OCDS releases from '+pages+' pages');
}catch(error){
 console.error('SYNC FAILED:',error);
 if(runId!==null)await pgClient.query('UPDATE sync_runs SET success=false,finished_at=now(),pages=$2,releases=$3,error=$4 WHERE id=$1',[runId,pages,releases,String(error).slice(0,1800)]).catch(()=>{});
 process.exitCode=1;
}finally{await pgClient.end()}
