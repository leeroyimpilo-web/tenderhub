'use client';
import Link from 'next/link';
import {usePathname} from 'next/navigation';
import {useEffect,useState} from 'react';
import {Home,Search,BarChart3,KanbanSquare,MoreHorizontal,Bookmark,CalendarDays,Bell,Settings,X} from 'lucide-react';
const tabs=[{href:'/',label:'Today',icon:Home},{href:'/search',label:'Find',icon:Search},{href:'/research',label:'Research',icon:BarChart3},{href:'/pipeline',label:'Pipeline',icon:KanbanSquare}];
const extras=[{href:'/saved',label:'Saved tenders',icon:Bookmark},{href:'/calendar',label:'Deadline calendar',icon:CalendarDays},{href:'/alerts',label:'Deadline alerts',icon:Bell},{href:'/settings',label:'Settings & data status',icon:Settings}];
export function Navigation(){
 const path=usePathname();const [open,setOpen]=useState(false);
 useEffect(()=>setOpen(false),[path]);
 return <>
  {open&&<><button type="button" aria-label="Close menu" onClick={()=>setOpen(false)} className="fixed inset-0 z-50 bg-black/20"/><div className="tb-v2-drawer" role="dialog" aria-label="More sections">
    <div className="flex items-center justify-between px-3 pb-2"><strong className="text-[13px] text-navy">More tools</strong><button type="button" aria-label="Close menu" onClick={()=>setOpen(false)}><X size={19}/></button></div>
    {extras.map(e=><Link key={e.href} href={e.href}><e.icon size={18}/>{e.label}</Link>)}
  </div></>}
  <nav className="tb-v2-nav" aria-label="Main navigation">
   {tabs.map(({href,label,icon:Icon})=><Link key={href} href={href} className={(href==='/'?path===href:path.startsWith(href))?'active':''}><Icon size={20}/><span>{label}</span></Link>)}
   <button type="button" onClick={()=>setOpen(v=>!v)} aria-expanded={open} aria-label="More sections" className={open||extras.some(e=>path===e.href)?'active':''}><MoreHorizontal size={20}/><span>More</span></button>
  </nav>
 </>;
}
