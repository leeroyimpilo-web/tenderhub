const url=new URL('https://www.etenders.gov.za/Home/PaginatedTenderOpportunities');
Object.entries({draw:'1',start:'0',length:'100',status:'1','search[value]':'','search[regex]':'false'}).forEach(([k,v])=>url.searchParams.set(k,v));
try{
 const response=await fetch(url,{signal:AbortSignal.timeout(30000),headers:{accept:'application/json, text/javascript, */*; q=0.01','user-agent':'TenderBaseV2/0.1 official portal source test','x-requested-with':'XMLHttpRequest',referer:'https://www.etenders.gov.za/Home/opportunities?id=1'}});
 const body=await response.text();
 console.log('OFFICIAL PORTAL HTTP '+response.status);
 if(!response.ok)throw new Error('Official portal data endpoint HTTP '+response.status+': '+body.slice(0,500));
 const parsed=JSON.parse(body);
 if(!Array.isArray(parsed.data))throw new Error('Missing data list; top-level keys: '+Object.keys(parsed).join(','));
 const docs=parsed.data.map(row=>({support:Array.isArray(row.supportDocument)?row.supportDocument.length:0,documents:Array.isArray(row.documents)?row.documents.length:0})); 
 const first=parsed.data.find(row=>Array.isArray(row.supportDocument)&&row.supportDocument.length>0);
 const firstSupport=first?.supportDocument[0];
 console.log(JSON.stringify({ok:true,source:'Official eTenders Opportunities JSON',recordsTotal:parsed.recordsTotal,recordsFiltered:parsed.recordsFiltered,count:parsed.data.length,rowsWithSupportDocuments:docs.filter(x=>x.support>0).length,firstSupportKeys:firstSupport&&typeof firstSupport==='object'?Object.keys(firstSupport):[],firstSupportTypes:firstSupport&&typeof firstSupport==='object'?Object.fromEntries(Object.entries(firstSupport).map(([k,v])=>[k,Array.isArray(v)?'array':typeof v])):{},otherDocumentFieldTypes:Object.fromEntries(['supportDocument','biddersListDocument','esubRequiredDocuments'].map(k=>[k,Array.isArray(parsed.data[0]?.[k])?'array':typeof parsed.data[0]?.[k]]))}));
 const firstId=parsed.data[0]?.id;
 if(Number.isInteger(firstId)){
  const detailUrl='https://www.etenders.gov.za/Home/?id='+encodeURIComponent(String(firstId));
  const detail=await fetch(detailUrl,{signal:AbortSignal.timeout(30000),headers:{accept:'text/html','user-agent':'TenderBaseV2/0.1 source-link-check'}});
  console.log(JSON.stringify({officialDetailLink:detail.status,contentType:detail.headers.get('content-type'),recordId:firstId}));
  if(!detail.ok)throw new Error('Official notice deep link HTTP '+detail.status);
 }
}catch(error){console.error('PORTAL PROBE FAILED:',String(error));process.exitCode=1}
