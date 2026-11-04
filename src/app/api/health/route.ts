import {NextResponse} from 'next/server';
import {getPool} from '@/lib/db';
export const dynamic='force-dynamic';
export async function GET(){
 try{
  const r=await getPool().query("SELECT (SELECT count(*)::int FROM tenders) count,(SELECT finished_at FROM sync_runs WHERE success=true ORDER BY finished_at DESC LIMIT 1) last_sync");
  return NextResponse.json({ok:true,source:'National Treasury eTenders OCDS',count:r.rows[0].count,lastSync:r.rows[0].last_sync??null},{headers:{'Cache-Control':'no-store'}});
 }catch(error){
  console.error('[health]',error);
  return NextResponse.json({ok:false,error:'Tender data is not available. Check database configuration and sync logs.'},{status:503});
 }
}
