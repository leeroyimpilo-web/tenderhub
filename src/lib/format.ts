export function fmtDate(value:string|null|undefined){
  if(!value) return 'Not supplied';
  const d=new Date(value);if(Number.isNaN(d.getTime()))return 'Not supplied';
  return new Intl.DateTimeFormat('en-ZA',{day:'numeric',month:'short',year:'numeric',timeZone:'Africa/Johannesburg'}).format(d);
}
export function daysUntil(value:string|null|undefined){
  if(!value) return null;
  const diff=new Date(value).getTime()-Date.now();
  return Number.isFinite(diff)?Math.ceil(diff/86400000):null;
}
export function tenderStatus(value:string|null|undefined){
  const days=daysUntil(value);
  return days===null?'Deadline unknown':days<0?'Closed':days<=7?'Closing soon':'Open';
}
export function fmtNumber(n:number){return new Intl.NumberFormat('en-ZA').format(n)}
export function toICS(t:{title:string;closing_at:string|null;reference:string|null;source_url:string}){
  if(!t.closing_at)return null;
  const start=new Date(t.closing_at);if(Number.isNaN(start.getTime()))return null;
  const utc=(d:Date)=>d.toISOString().replace(/[-:]/g,'').replace(/\.\d{3}Z$/,'Z');
  const escape=(s:string)=>s.replace(/\\/g,'\\\\').replace(/\n/g,'\\n').replace(/[,;]/g,c=>'\\'+c);
  return ['BEGIN:VCALENDAR','VERSION:2.0','PRODID:-//TenderBase//eTenders MVP//EN','BEGIN:VEVENT','UID:'+encodeURIComponent(t.source_url)+'@tenderbase','DTSTAMP:'+utc(new Date()),'DTSTART:'+utc(start),'DTEND:'+utc(new Date(start.getTime()+3600000)),'SUMMARY:'+escape('Tender closes: '+t.title),'DESCRIPTION:'+escape((t.reference??'')+' '+t.source_url),'END:VEVENT','END:VCALENDAR'].join('\r\n');
}
