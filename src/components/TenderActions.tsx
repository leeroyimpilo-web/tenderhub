'use client';
import {useState} from 'react';
import {Bookmark,BookmarkCheck,KanbanSquare,CalendarDays,Share2,Check} from 'lucide-react';
import {useLocal} from '@/components/LocalProvider';
import {toICS} from '@/lib/format';
import type {Tender} from '@/lib/types';
export function SaveButton({tender,compact=false}:{tender:Tender;compact?:boolean}){
 const {ready,isSaved,toggleSaved}=useLocal();
 const saved=isSaved(tender.id);
 return <button type="button" disabled={!ready} onClick={()=>toggleSaved(tender)} className={compact?'tb-card-bookmark':'tb-v2-secondary'} aria-label={saved?'Remove saved tender':'Save tender'} aria-pressed={saved}>{saved?<BookmarkCheck size={19}/>:<Bookmark size={19}/>} {!compact&&(saved?'Saved':'Save')}</button>;
}
export function TrackButton({tender}:{tender:Tender}){
 const {ready,isTracked,addPipeline}=useLocal();const tracked=isTracked(tender.id);
 return <button type="button" disabled={!ready||tracked} onClick={()=>addPipeline(tender)} className="tb-v2-secondary">{tracked?<Check size={16}/>:<KanbanSquare size={16}/>} {tracked?'In pipeline':'Track bid'}</button>;
}
export function CalendarButton({tender}:{tender:Tender}){
 const download=()=>{
   const ics=toICS(tender);if(!ics)return;
   const url=URL.createObjectURL(new Blob([ics],{type:'text/calendar;charset=utf-8'}));
   const a=document.createElement('a');a.href=url;a.download='tender-'+tender.id.replace(/[^a-zA-Z0-9-]/g,'-')+'.ics';a.click();
   setTimeout(()=>URL.revokeObjectURL(url),1000);
 };
 return <button type="button" disabled={!tender.closing_at} onClick={download} className="tb-v2-secondary"><CalendarDays size={16}/> Add deadline</button>;
}
export function ShareButton({tender}:{tender:Tender}){
 const [copied,setCopied]=useState(false);
 const share=async()=>{
   const url=location.origin+'/tenders/'+encodeURIComponent(tender.id);
   try{
     if(navigator.share){await navigator.share({title:tender.title,text:tender.reference??tender.title,url});return;}
     if(navigator.clipboard){await navigator.clipboard.writeText(url);setCopied(true);return;}
     window.prompt('Copy tender link',url);
   }catch(err){if((err as Error)?.name!=='AbortError')window.prompt('Copy tender link',url)}
 };
 return <button type="button" onClick={share} className="tb-v2-secondary">{copied?<Check size={16}/>:<Share2 size={16}/>} {copied?'Copied':'Share'}</button>;
}
