'use client';
import {useCallback,useEffect,useState} from 'react';
import {Database,RefreshCcw,CheckCircle2,AlertTriangle,ExternalLink} from 'lucide-react';
import {useLocal} from '@/components/LocalProvider';
import {fmtDate} from '@/lib/format';
type Health={ok:boolean;count?:number;lastSync?:string|null;error?:string};
const provinces=['Eastern Cape','Free State','Gauteng','KwaZulu-Natal','Limpopo','Mpumalanga','North West','Northern Cape','Western Cape'];
export default function Settings(){
 const {data,ready,updateSettings}=useLocal();
 const [health,setHealth]=useState<Health|null>(null);const [checking,setChecking]=useState(false);
 const check=useCallback(async()=>{
  setChecking(true);
  try{const response=await fetch('/api/health',{cache:'no-store'});setHealth(await response.json())}
  catch{setHealth({ok:false,error:'Unable to reach the TenderBase server'})}
  finally{setChecking(false)}
 },[]);
 useEffect(()=>{void check()},[check]);
 return <main className="tb-v2-content">
  <p className="tb-v2-eyebrow">Personalise TenderBase</p><h1 className="tb-v2-heading mt-2">Settings</h1>
  <p className="tb-v2-muted mt-2">Preferences stay on this device. No sign-in required.</p>
  <section className="tb-v2-panel mt-6">
   <h2 className="font-semibold text-navy">Tender preferences</h2>
   {!ready?<p className="tb-v2-muted mt-2">Loading on-device settings…</p>:<div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-4">
    <label><span className="tb-v2-label">Default Find province</span><select className="tb-v2-field" value={data.settings.defaultProvince} onChange={e=>updateSettings({defaultProvince:e.target.value})}><option value="">All provinces</option>{provinces.map(p=><option key={p} value={p}>{p}</option>)}</select></label>
    <label><span className="tb-v2-label">In-app alert window</span><select className="tb-v2-field" value={data.settings.remindDays} onChange={e=>updateSettings({remindDays:Number(e.target.value)})}><option value={1}>1 day before closing</option><option value={3}>3 days</option><option value={7}>7 days</option><option value={14}>14 days</option><option value={30}>30 days</option></select></label>
   </div>}
   <p className="tb-v2-muted mt-3">Settings save immediately. Find applies your default province when it opens without existing filters.</p>
  </section>
  <section className="tb-v2-panel mt-4">
   <div className="tb-v2-topbar"><h2 className="font-semibold text-navy flex items-center gap-2"><Database size={18}/> Official data status</h2><button type="button" className="tb-v2-secondary" disabled={checking} onClick={()=>void check()}><RefreshCcw size={15}/>{checking?'Checking…':'Recheck'}</button></div>
   {!health?<p className="tb-v2-muted mt-3">Checking database connection…</p>:health.ok?<div className="mt-4">
    <p className="text-[13px] text-open flex items-center gap-2"><CheckCircle2 size={16}/> Database connected</p>
    <p className="tb-v2-muted mt-2">{(health.count??0).toLocaleString('en-ZA')} official records imported</p>
    <p className="tb-v2-muted mt-1">Last successful import: {health.lastSync?fmtDate(health.lastSync):'Never — run the import job'}</p>
   </div>:<div className="tb-v2-error mt-4"><AlertTriangle size={18}/><p className="mt-2">{health.error??'Data source unavailable'}</p></div>}
   <a href="https://data.etenders.gov.za/" target="_blank" rel="noreferrer" className="inline-flex gap-2 items-center text-blue text-[12px] font-semibold mt-4">National Treasury official portal <ExternalLink size={15}/></a>
  </section>
 </main>;
}
