import {NextResponse,NextRequest} from 'next/server';
import {searchTenders} from '@/lib/tenders';
export const dynamic='force-dynamic';
export async function GET(req:NextRequest){
 const p=req.nextUrl.searchParams;
 try{
  const result=await searchTenders({q:p.get('q')??'',province:p.get('province')??'',category:p.get('category')??'',buyer:p.get('buyer')??'',status:p.get('status')??'',sort:p.get('sort')??'',page:Number(p.get('page')??1),pageSize:Number(p.get('pageSize')??20)});
  return NextResponse.json(result,{headers:{'Cache-Control':'public, max-age=30'}});
 }catch(error){console.error('[api/tenders]',error);return NextResponse.json({error:'Tender database unavailable. Retry after synchronisation.'},{status:503})}
}
