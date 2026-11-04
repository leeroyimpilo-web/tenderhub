'use client';
import Link from 'next/link';
import {useState} from 'react';
import {CalendarDays} from 'lucide-react';
import {useLocal} from '@/components/LocalProvider';
import {CalendarButton} from '@/components/TenderActions';
import type {Tender} from '@/lib/types';
import {fmtDate} from '@/lib/format';
export function DeadlineDashboard({records}:{records:Tender[]}){
 const {data,dismissAlert,resetAlerts}=useLocal();const [tab,setTab]=useState<'upcoming'|'alerts'>('upcoming');const [day,setDay]=useState<number|null>(null);const days=Array.from({length:7},(_,i)=>{const d=new Date();d.setDate(d.getDate()+i);return d});const current=new Date();
 const alerts=data.saved.filter(t=>t.closing_at&&new Date(t.closing_at).getTime()>Date.now()&&new Date(t.closing_at).getTime()<Date.now()+data.settings.remindDays*86400000&&!data.dismissedAlerts.includes(t.id+'|'+t.closing_at));
 const rows=tab==='alerts'?alerts:records;
 return <main className="tb-page-content"><h1 className="tb-page-title">Deadlines</h1><p className="tb-page-subtitle">Keep the next date in view.</p><div className="tb-page-tabs"><button onClick={()=>setTab('upcoming')} className={tab==='upcoming'?'active':''}>Upcoming</button><button onClick={()=>setTab('alerts')} className={tab==='alerts'?'active':''}>Alerts {alerts.length}</button></div>
 {tab==='upcoming'&&<><div className="tb-month-band"><CalendarDays size={19}/>{current.toLocaleDateString('en-ZA',{month:'long',year:'numeric'})}</div><div className="tb-date-strip">{days.map((d,i)=><button key={i} className={day===d.getDate()?'selected':''} onClick={()=>setDay(day===d.getDate()?null:d.getDate())}><small>{d.toLocaleDateString('en-ZA',{weekday:'short'})}</small><b>{d.getDate()}</b></button>)}</div><p className="tb-helper">All official deadlines · next 90 days</p></>}
 {tab==='alerts'&&<div className="tb-helper tb-alert-helper"><span>Saved tenders closing within {data.settings.remindDays} days</span><button onClick={resetAlerts}>Restore alerts</button><small>Alerts are checked when you open the app.</small></div>}
 {!rows.length?<div className="tb-empty-inline">{tab==='alerts'?'No active deadline alerts. Save an open tender to track its closing date.':'There are no upcoming deadlines in the imported official dataset.'}</div>:<div className="tb-deadline-list">{rows.filter(t=>day===null||tab==='alerts'||(t.closing_at&&new Date(t.closing_at).getDate()===day)).map(t=><article className="tb-deadline-row" key={t.id}><div className="tb-deadline-date"><b>{t.closing_at?new Date(t.closing_at).toLocaleDateString('en-ZA',{day:'2-digit'}):'—'}</b><span>{t.closing_at?new Date(t.closing_at).toLocaleDateString('en-ZA',{month:'short'}).toUpperCase():'TBC'}</span></div><div className="tb-deadline-detail"><Link href={'/tenders/'+encodeURIComponent(t.id)}>{t.title}</Link><p>{t.closing_at?new Date(t.closing_at).toLocaleTimeString('en-ZA',{hour:'2-digit',minute:'2-digit'})+' · Closes '+fmtDate(t.closing_at):'Closing date not supplied'}</p><div className="tb-calendar-action"><CalendarButton tender={t}/>{tab==='alerts'&&<button onClick={()=>dismissAlert(t.id)}>Dismiss</button>}</div></div></article>)}</div>}
 </main>
}
