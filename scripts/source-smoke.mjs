const base='https://ocds-api.etenders.gov.za/api/OCDSReleases';
const now=new Date();
const to=now.toISOString().slice(0,10);
const prior=new Date(now.getTime()-3*86400000).toISOString().slice(0,10);
const tests=[
 {label:'window pageSize=100',params:{PageNumber:1,PageSize:100,dateFrom:prior,dateTo:to}},
 {label:'window default pageSize',params:{PageNumber:1,dateFrom:prior,dateTo:to}},
 {label:'window pageSize=500',params:{PageNumber:1,PageSize:500,dateFrom:prior,dateTo:to}},
 {label:'page only default size',params:{PageNumber:1}}
];
let success=false;
for(const spec of tests){
 const url=new URL(base);
 Object.entries(spec.params).forEach(([k,v])=>url.searchParams.set(k,String(v)));
 try{
  const response=await fetch(url,{headers:{accept:'application/json','user-agent':'TenderBaseV2/0.1 source-validation'},signal:AbortSignal.timeout(30000)});
  if(!response.ok){const body=await response.text();console.error(spec.label+' HTTP '+response.status+' : '+body.slice(0,500));continue}
  const data=await response.json();
  console.log(spec.label+' HTTP 200. Top-level keys: '+Object.keys(data).join(','));
  if(!data||!Array.isArray(data.releases)){console.error('Unexpected shape; no releases array');continue}
  console.log(JSON.stringify({ok:true,releasesReturned:data.releases.length,firstOcid:data.releases[0]?.ocid??null}));
  success=true;break;
 }catch(error){console.error(spec.label+' '+String(error).slice(0,500))}
}
if(!success)process.exitCode=1;
