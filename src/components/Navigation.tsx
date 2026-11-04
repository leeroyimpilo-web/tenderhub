'use client';
import Link from 'next/link';
import {usePathname} from 'next/navigation';
import {Search,ClipboardCheck,CalendarDays,ChartNoAxesColumnIncreasing} from 'lucide-react';
const tabs=[{href:'/',label:'Discover',icon:Search},{href:'/pipeline',label:'My bids',icon:ClipboardCheck},{href:'/calendar',label:'Deadlines',icon:CalendarDays},{href:'/research',label:'Insights',icon:ChartNoAxesColumnIncreasing}];
export function Navigation(){
 const path=usePathname();
 if(path==='/onboarding'||path.startsWith('/tenders/'))return null;
 return <nav className="tb-v2-nav" aria-label="Main navigation">{tabs.map(({href,label,icon:Icon})=><Link key={href} href={href} className={(href==='/'?path==='/':path===href||path.startsWith(href+'/'))?'active':''}><span className="tb-nav-icon"><Icon size={21}/></span><span>{label}</span></Link>)}</nav>
}
