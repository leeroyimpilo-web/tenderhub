'use client';
import {useEffect} from 'react';
import {usePathname,useRouter,useSearchParams} from 'next/navigation';
import {useLocal} from '@/components/LocalProvider';
export function SearchPreference(){
 const {ready,data}=useLocal();const router=useRouter();const search=useSearchParams();const path=usePathname();
 useEffect(()=>{
  if(!ready||!data.settings.defaultProvince||search.has('province'))return;
  const next=new URLSearchParams(search.toString());next.set('province',data.settings.defaultProvince);
  router.replace(path+'?'+next.toString());
 },[ready,data.settings.defaultProvince,search,path,router]);
 return null;
}
