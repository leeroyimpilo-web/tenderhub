'use client';
import Link from 'next/link';
import {usePathname,useRouter} from 'next/navigation';
import {ArrowLeft,Settings,Share2} from 'lucide-react';
export function TopBar(){
 const path=usePathname(),router=useRouter();
 if(path==='/onboarding')return null;
 const back=path.startsWith('/tenders/')||path==='/saved-searches';
 return <header className="tb-topbar">
  {back?<><button className="tb-topback" type="button" onClick={()=>router.back()} aria-label="Go back"><ArrowLeft size={21}/><span>Discover</span></button>{path.startsWith('/tenders/')&&<button className="tb-settings" aria-label="Share tender" onClick={()=>navigator.share?.({url:location.href})}><Share2 size={21}/></button>}</>:<Link href="/" className="tb-brand" aria-label="TenderBase home"><span className="tb-brand-mark">[ ]</span><span>tenderbase</span></Link>}
  {!back&&<Link href="/settings" className="tb-settings" aria-label="Settings"><Settings size={23}/></Link>}
 </header>
}
