function asRecord(value){return value && typeof value==='object' && !Array.isArray(value)?value:{}}
function clean(value){return typeof value==='string'&&value.trim()?value.trim():null}
function date(value){if(!value)return null;const d=new Date(value);return Number.isNaN(d.getTime())?null:d.toISOString()}
function validHttpUrl(value){if(typeof value!=='string')return null;try{const u=new URL(value);return ['https:','http:'].includes(u.protocol)?u.toString():null}catch{return null}}
export function normalizeRelease(source){
  const release=asRecord(source);
  const ocid=clean(release.ocid);
  const tender=asRecord(release.tender);
  const title=clean(tender.title)??clean(tender.description);
  if(!ocid||!title)return null;
  const parties=Array.isArray(release.parties)?release.parties.map(asRecord):[];
  const explicitBuyer=asRecord(release.buyer);
  const buyerParty=parties.find(p=>Array.isArray(p.roles)&&p.roles.includes('buyer'))??{};
  const buyerName=clean(explicitBuyer.name)??clean(buyerParty.name)??clean(asRecord(tender.procuringEntity).name);
  const address=asRecord(asRecord(buyerParty).address);
  const period=asRecord(tender.tenderPeriod);
  const documents=(Array.isArray(tender.documents)?tender.documents:[]).map(asRecord).map((d,i)=>({
    id:String(d.id??i),
    title:clean(d.title)??clean(d.description)??('Document '+(i+1)),
    url:validHttpUrl(d.url),
    format:clean(d.format),
    publishedAt:date(d.datePublished)
  })).filter(d=>d.url);
  const classification=asRecord(tender.classification);
  return {
    id:ocid,reference:clean(tender.id),
    title,description:clean(tender.description),
    buyer:buyerName,
    province:clean(tender.province)??clean(address.region)??null,
    category:clean(tender.mainProcurementCategory)??clean(tender.category)??clean(classification.description),
    status:clean(tender.status),
    published_at:date(period.startDate)??date(release.date),
    closing_at:date(period.endDate),
    release_at:date(release.date),
    source_url:'https://ocds-api.etenders.gov.za/api/OCDSReleases/release/'+encodeURIComponent(ocid),
    documents,raw:release
  };
}
export function releaseArray(payload){
  if(payload && Array.isArray(payload.releases))return payload.releases;
  throw new Error('Unexpected OCDS response: releases array missing');
}
