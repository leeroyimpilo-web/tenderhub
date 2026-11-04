import Link from 'next/link';
import {CalendarClock,MapPin,Building2,Bookmark} from 'lucide-react';
import type {Tender} from '@/lib/types';
import {fmtDate,tenderStatus} from '@/lib/format';
import {SaveButton,TrackButton} from '@/components/TenderActions';
export function TenderCard({tender,highlighted=false,match}:{tender:Tender;highlighted?:boolean;match?:string}){
 const status=tenderStatus(tender.closing_at);
 return <article className={'tb-tender-card '+(highlighted?'highlighted':'')}>
  <div className="tb-card-category">{(tender.category??'Tender notice').toUpperCase()}</div><SaveButton tender={tender} compact/>
  <Link href={'/tenders/'+encodeURIComponent(tender.id)} className="tb-card-title">{tender.title}</Link>
  <p className="tb-card-buyer">{tender.buyer??'Buyer not supplied'}</p><p className="tb-card-province">{tender.province??'Province not supplied'}</p>
  <div className="tb-card-deadline"><strong>{tender.closing_at?new Date(tender.closing_at).toLocaleDateString('en-ZA',{day:'2-digit',month:'short'}).toUpperCase():'DATE TBC'}</strong><span>Closes {fmtDate(tender.closing_at)}</span></div>
  {match&&<div className="tb-card-match">Matches “{match}”{tender.province?' · '+tender.province:''}</div>}
  {!match&&<div className="tb-card-actions"><TrackButton tender={tender}/></div>}
 </article>;
}
