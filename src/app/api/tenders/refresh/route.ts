import {NextRequest,NextResponse} from 'next/server';
import {getTendersByIds} from '@/lib/tenders';
export const dynamic='force-dynamic';
export async function POST(request:NextRequest){
 let ids:unknown;
 try{const body=await request.json();ids=body?.ids}catch{return NextResponse.json({error:'Invalid JSON'},{status:400})}
 if(!Array.isArray(ids)||ids.length>100||ids.some(id=>typeof id!=='string'||id.length<1||id.length>200)){
  return NextResponse.json({error:'Provide up to 100 valid tender IDs'},{status:400});
 }
 const unique=Array.from(new Set(ids as string[]));
 try{
  const rows=await getTendersByIds(unique);
  return NextResponse.json({items:rows.map(t=>({...t,raw:{}}))},{headers:{'Cache-Control':'no-store'}});
 }catch(error){
  console.error('[api/tenders/refresh]',error);
  return NextResponse.json({error:'Tender database unavailable'},{status:503});
 }
}
