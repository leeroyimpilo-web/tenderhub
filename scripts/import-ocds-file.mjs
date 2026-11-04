import {readFile} from 'node:fs/promises';
import {gunzipSync} from 'node:zlib';
import pg from 'pg';
import {normalizeRelease} from './normalize-ocds.mjs';
import {parseOcdsText} from './parse-ocds-file.mjs';
const path=process.argv[2];
if(!path){console.error('Usage: node scripts/import-ocds-file.mjs /path/to/official-releases.json[.gz]');process.exit(2)}
if(!process.env.DATABASE_URL){console.error('DATABASE_URL required');process.exit(2)}
let releases;
try{
 const input=await readFile(path);
 if(input.length>150*1024*1024)throw new Error('File too large; split the official monthly archive into smaller files');
 const text=(path.endsWith('.gz')?gunzipSync(input):input).toString('utf8');
 releases=parseOcdsText(text);
 console.log('Found '+releases.length+' source releases in the supplied file');
}catch(err){console.error('Cannot read official OCDS file:',err);process.exit(1)}
const client=new pg.Client({connectionString:process.env.DATABASE_URL});
let runId=null,imported=0;
const upsert=String.raw`INSERT INTO tenders(id,reference,title,description,buyer,province,category,status,published_at,closing_at,release_at,source_url,documents,raw,synced_at)
VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13::jsonb,$14::jsonb,now())
ON CONFLICT(id) DO UPDATE SET
 reference=EXCLUDED.reference,title=EXCLUDED.title,description=EXCLUDED.description,buyer=EXCLUDED.buyer,
 province=EXCLUDED.province,category=EXCLUDED.category,status=EXCLUDED.status,published_at=EXCLUDED.published_at,
 closing_at=EXCLUDED.closing_at,release_at=EXCLUDED.release_at,source_url=EXCLUDED.source_url,
 documents=EXCLUDED.documents,raw=EXCLUDED.raw,synced_at=now()
WHERE COALESCE(tenders.release_at,'epoch'::timestamptz)<=COALESCE(EXCLUDED.release_at,'epoch'::timestamptz)`;
try{
 await client.connect();
 const r=await client.query("INSERT INTO sync_runs(source,success) VALUES($1,NULL) RETURNING id",['National Treasury official bulk OCDS JSON']);runId=r.rows[0].id;
 for(const row of releases){
  const t=normalizeRelease(row);if(!t)continue;
  await client.query(upsert,[t.id,t.reference,t.title,t.description,t.buyer,t.province,t.category,t.status,t.published_at,t.closing_at,t.release_at,t.source_url,JSON.stringify(t.documents),JSON.stringify(t.raw)]);
  imported++;
 }
 await client.query('UPDATE sync_runs SET success=true,finished_at=now(),pages=1,releases=$2 WHERE id=$1',[runId,imported]);
 console.log('Imported '+imported+' valid official OCDS releases. No mock records used.');
}catch(error){
 console.error('Import failed:',error);
 if(runId!==null)await client.query('UPDATE sync_runs SET success=false,finished_at=now(),releases=$2,error=$3 WHERE id=$1',[runId,imported,String(error).slice(0,1500)]).catch(()=>{});
 process.exitCode=1;
}finally{await client.end()}
