'use client';
import {createContext,useCallback,useContext,useEffect,useRef,useState} from 'react';
import type {LocalData,LocalSettings,PipelineRecord,PipelineStage,Tender} from '@/lib/types';
const KEY='tenderbase-v2-local-v1';
const defaults:LocalData={saved:[],pipeline:[],dismissedAlerts:[],settings:{defaultProvince:'',remindDays:7}};
type Store={
 data:LocalData;ready:boolean;isSaved:(id:string)=>boolean;isTracked:(id:string)=>boolean;
 toggleSaved:(t:Tender)=>void;addPipeline:(t:Tender)=>void;removePipeline:(id:string)=>void;
 setStage:(id:string,stage:PipelineStage)=>void;setNotes:(id:string,notes:string)=>void;
 dismissAlert:(id:string)=>void;resetAlerts:()=>void;updateSettings:(value:Partial<LocalSettings>)=>void;
};
const Context=createContext<Store|null>(null);
function stripTender(t:Tender):Tender{return {...t,raw:{},documents:t.documents??[]}}
export function LocalProvider({children}:{children:React.ReactNode}){
 const [data,setData]=useState<LocalData>(defaults);
 const [ready,setReady]=useState(false);
 const ref=useRef<LocalData>(defaults);
 useEffect(()=>{
   try{
    const raw=localStorage.getItem(KEY);
    if(raw){
      const v=JSON.parse(raw) as Partial<LocalData>;
      const restored:LocalData={
        saved:Array.isArray(v.saved)?v.saved:[],
        pipeline:Array.isArray(v.pipeline)?v.pipeline:[],
        dismissedAlerts:Array.isArray(v.dismissedAlerts)?v.dismissedAlerts:[],
        settings:{...defaults.settings,...v.settings}
      };
      ref.current=restored;setData(restored);
    }
   }catch(error){console.error('Local saved data could not be read',error)}
   setReady(true);
 },[]);
 const change=useCallback((fn:(d:LocalData)=>LocalData)=>{
   const next=fn(ref.current);ref.current=next;setData(next);
   try{localStorage.setItem(KEY,JSON.stringify(next))}catch(error){console.error('Local storage unavailable',error);window.alert('Unable to save on this device. Check available storage.')}
 },[]);
 const value:Store={
  data,ready,isSaved:(id)=>data.saved.some(t=>t.id===id),isTracked:(id)=>data.pipeline.some(p=>p.tender.id===id),
  toggleSaved:(t)=>change(d=>({...d,saved:d.saved.some(v=>v.id===t.id)?d.saved.filter(v=>v.id!==t.id):[stripTender(t),...d.saved]})),
  addPipeline:(t)=>change(d=>({...d,pipeline:d.pipeline.some(v=>v.tender.id===t.id)?d.pipeline:[{tender:stripTender(t),stage:'New match',notes:'',updatedAt:new Date().toISOString()},...d.pipeline]})),
  removePipeline:(id)=>change(d=>({...d,pipeline:d.pipeline.filter(p=>p.tender.id!==id)})),
  setStage:(id,stage)=>change(d=>({...d,pipeline:d.pipeline.map(p=>p.tender.id===id?{...p,stage,updatedAt:new Date().toISOString()}:p)})),
  setNotes:(id,notes)=>change(d=>({...d,pipeline:d.pipeline.map(p=>p.tender.id===id?{...p,notes,updatedAt:new Date().toISOString()}:p)})),
  dismissAlert:(id)=>change(d=>({...d,dismissedAlerts:d.dismissedAlerts.includes(id)?d.dismissedAlerts:[...d.dismissedAlerts,id]})),
  resetAlerts:()=>change(d=>({...d,dismissedAlerts:[]})),
  updateSettings:(settings)=>change(d=>({...d,settings:{...d.settings,...settings}}))
 };
 return <Context.Provider value={value}>{children}</Context.Provider>;
}
export function useLocal(){const ctx=useContext(Context);if(!ctx)throw new Error('LocalProvider required');return ctx}
