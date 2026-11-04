import 'server-only';
import {getPool} from '@/lib/db';
import type {Tender, TenderFilter, TenderPage} from '@/lib/types';

function plain(row:Record<string,any>):Tender {
  const date=(v:unknown)=>v ? new Date(String(v)).toISOString() : null;
  return {...row, published_at:date(row.published_at),closing_at:date(row.closing_at),release_at:date(row.release_at),synced_at:date(row.synced_at)??'',documents:Array.isArray(row.documents)?row.documents:[],raw:row.raw??{}} as Tender;
}
function whereFor(f:TenderFilter){
  const clauses:string[]=[];
  const values:unknown[]=[];
  const param=(value:unknown)=>{values.push(value);return '$'+values.length;};
  if(f.q?.trim()){const n=param('%'+f.q.trim().slice(0,100)+'%');clauses.push("(title ILIKE "+n+" OR COALESCE(description,'') ILIKE "+n+" OR COALESCE(reference,'') ILIKE "+n+" OR COALESCE(buyer,'') ILIKE "+n+")");}
  if(f.province?.trim()) clauses.push("province = "+param(f.province.trim()));
  if(f.category?.trim()) clauses.push("category = "+param(f.category.trim()));
  if(f.buyer?.trim()) clauses.push("buyer ILIKE "+param('%'+f.buyer.trim()+'%'));
  if(f.status==='open') clauses.push("closing_at >= now()");
  if(f.status==='closed') clauses.push("closing_at < now()");
  if(f.status==='undated') clauses.push("closing_at IS NULL");
  return {where:clauses.length?' WHERE '+clauses.join(' AND '):'',values,param};
}
export async function searchTenders(f:TenderFilter={}):Promise<TenderPage>{
  const page=Math.max(1,Math.floor(Number(f.page)||1));
  const pageSize=Math.min(50,Math.max(1,Math.floor(Number(f.pageSize)||20)));
  const q=whereFor(f);
  const order=f.sort==='closing'?"closing_at ASC NULLS LAST, published_at DESC":f.sort==='oldest'?"published_at ASC NULLS LAST":"COALESCE(published_at,release_at) DESC NULLS LAST";
  const pool=getPool();
  const count=await pool.query<{total:string}>("SELECT count(*)::text as total FROM tenders"+q.where,q.values);
  const total=Number(count.rows[0].total);
  const limit=q.param(pageSize);const offset=q.param((page-1)*pageSize);
  const result=await pool.query("SELECT * FROM tenders"+q.where+" ORDER BY "+order+", id ASC LIMIT "+limit+" OFFSET "+offset,q.values);
  return {items:result.rows.map(plain),total,page,pages:Math.max(1,Math.ceil(total/pageSize))};
}
export async function getTender(id:string):Promise<Tender|null>{
  const result=await getPool().query("SELECT * FROM tenders WHERE id=$1 LIMIT 1",[id]);
  return result.rows[0]?plain(result.rows[0]):null;
}
export async function getDashboard(){
  const pool=getPool();
  const [counts,latest,closing,sync]=await Promise.all([
    pool.query("SELECT count(*)::int total, count(*) FILTER (WHERE closing_at>=now())::int open, count(*) FILTER (WHERE closing_at>=now() AND closing_at<now()+interval '7 days')::int closing FROM tenders"),
    pool.query("SELECT * FROM tenders ORDER BY COALESCE(published_at,release_at) DESC NULLS LAST LIMIT 5"),
    pool.query("SELECT * FROM tenders WHERE closing_at>=now() ORDER BY closing_at ASC LIMIT 5"),
    pool.query("SELECT finished_at,releases,pages FROM sync_runs WHERE success=true ORDER BY finished_at DESC LIMIT 1")
  ]);
  const status=sync.rows[0];
  return {stats:counts.rows[0],latest:latest.rows.map(plain),closing:closing.rows.map(plain),lastSync:status?.finished_at?new Date(status.finished_at).toISOString():null};
}
export async function getResearch(){
  const pool=getPool();
  const group=async(column:'buyer'|'province'|'category')=>{
    const r=await pool.query("SELECT "+column+" AS name,count(*)::int AS count FROM tenders WHERE "+column+" IS NOT NULL AND "+column+"<>'' GROUP BY "+column+" ORDER BY count DESC LIMIT 15");
    return r.rows as {name:string;count:number}[];
  };
  const [buyers,provinces,categories,total]=await Promise.all([group('buyer'),group('province'),group('category'),pool.query("SELECT count(*)::int total FROM tenders")]);
  return {buyers,provinces,categories,total:Number(total.rows[0].total)};
}
export async function getUpcoming(days=90):Promise<Tender[]>{
  const d=Math.min(365,Math.max(1,Math.floor(days)));
  const r=await getPool().query("SELECT * FROM tenders WHERE closing_at>=now() AND closing_at < now()+make_interval(days=>$1) ORDER BY closing_at ASC LIMIT 300",[d]);
  return r.rows.map(plain);
}
export async function getFacets(){
  const pool=getPool();
  const r=await pool.query("SELECT array_remove(array_agg(DISTINCT province ORDER BY province),NULL) as provinces, array_remove(array_agg(DISTINCT category ORDER BY category),NULL) as categories FROM tenders");
  return {provinces:(r.rows[0]?.provinces??[]) as string[],categories:(r.rows[0]?.categories??[]) as string[]};
}

export async function getTendersByIds(ids:string[]):Promise<Tender[]>{
 if(ids.length===0)return [];
 const result=await getPool().query("SELECT * FROM tenders WHERE id=ANY($1::text[]) LIMIT 100",[ids]);
 return result.rows.map(plain);
}
