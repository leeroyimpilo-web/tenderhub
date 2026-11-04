const official='https://www.etenders.gov.za';
const portalUrl=official+'/Home/opportunities?id=1';
const record=value=>value&&typeof value==='object'&&!Array.isArray(value)?value:{};
const clean=value=>typeof value==='string'&&value.trim()?value.trim():null;
const asDate=value=>{
 if(!value)return null;
 const input=String(value);const ticks=input.match(/^\/Date\((\d+)/);
 const d=new Date(ticks?Number(ticks[1]):input);
 return Number.isNaN(d.getTime())?null:d.toISOString();
};
const safeHttp=value=>{
 if(typeof value!=='string')return null;
 try{const u=new URL(value,official);return ['http:','https:'].includes(u.protocol)?u.toString():null}catch{return null}
};
const docKeys=new Set(['supportdocument','supportdocuments','documents','files','attachments','esubrequireddocuments','questionsandanswers','bidderslistdocument','bidderslistdocuments']);
export function portalDocuments(row){
 const found=new Map();const seen=new Set();
 const visit=(value,hint='')=>{
  if(!value||typeof value!=='object'||seen.has(value))return;
  seen.add(value);
  if(Array.isArray(value)){value.forEach(v=>visit(v,hint));return}
  const obj=record(value);
  const keys=new Map(Object.entries(obj).map(([k,v])=>[k.replace(/[^a-z0-9]/gi,'').toLowerCase(),v]));
  const supportId=clean(keys.get('supportdocumentid'));
  const extension=clean(keys.get('extension'));
  const blob=clean(keys.get('blobname'))??clean(keys.get('blobfilename'))??(supportId&&extension?supportId+extension:null);
  const file=clean(keys.get('downloadedfilename'))??clean(keys.get('filename'))??clean(keys.get('originalfilename'))??clean(keys.get('documentname'));
  const direct=clean(keys.get('url'))??clean(keys.get('documenturl'))??clean(keys.get('downloadurl'));
  const identifier=clean(keys.get('supportdocumentid'))??clean(keys.get('id'))??blob;
  const title=clean(keys.get('title'))??clean(keys.get('description'))??file??blob??'Tender document';
  let url=direct?safeHttp(direct):null;
  if(!url&&blob){
   const path=new URL('/home/Download/',official);
   path.searchParams.set('blobName',blob);path.searchParams.set('downloadedFileName',file??blob);
   url=path.toString();
  }
  if(url && (hint||blob||keys.has('supportdocumentid'))){
   const id=identifier??url;
   found.set(id,{id,title,url,format:clean(keys.get('format'))??clean(keys.get('contenttype')),publishedAt:asDate(keys.get('datepublished')??keys.get('datemodified'))});
  }
  for(const [key,child] of Object.entries(obj)){
   const k=key.replace(/[^a-z0-9]/gi,'').toLowerCase();
   if(docKeys.has(k)||k.includes('document'))visit(child,k);
   else if(child&&typeof child==='object')visit(child,hint);
  }
 };
 visit(row);
 return [...found.values()];
}
export function normalizePortalOpportunity(value){
 const r=record(value);
 const number=clean(r.tender_No)??clean(r.tenderNumber)??clean(r.referenceNumber);
 const id=r.ocid&&typeof r.ocid==='string'?r.ocid:
  (r.id!==undefined && r.id!==null?'etenders-portal-'+String(r.id):null);
 const title=clean(r.description)??clean(r.title);
 if(!id||!title)return null;
 const buyer=clean(r.organ_of_State)??clean(r.department);
 return {
  id,reference:number,title,
  description:clean(r.conditions),
  buyer,province:clean(r.province),
  category:clean(r.category),
  status:clean(r.status),
  published_at:asDate(r.date_Published),
  closing_at:asDate(r.closing_Date),
  release_at:asDate(r.sysStartTime)??asDate(r.date_Published),
  source_url:(typeof r.id==='number' && Number.isInteger(r.id) ? official+'/Home/?id='+encodeURIComponent(String(r.id)) : portalUrl),
  documents:portalDocuments(r),raw:{...r,source:'National Treasury eTenders official opportunities'}
 };
}
export function validatePortalPage(body){
 if(!body||typeof body!=='object'||!Array.isArray(body.data))throw new Error('Official portal returned unexpected JSON (data array missing)');
 return {rows:body.data,total:Number(body.recordsFiltered??body.recordsTotal??0)};
}
