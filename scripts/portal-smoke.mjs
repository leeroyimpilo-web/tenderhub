const url=new URL('https://www.etenders.gov.za/Home/PaginatedTenderOpportunities');
Object.entries({draw:'1',start:'0',length:'2',status:'1','search[value]':'','search[regex]':'false'}).forEach(([k,v])=>url.searchParams.set(k,v));
try{
 const response=await fetch(url,{signal:AbortSignal.timeout(30000),headers:{accept:'application/json, text/javascript, */*; q=0.01','user-agent':'TenderBaseV2/0.1 official portal source test','x-requested-with':'XMLHttpRequest',referer:'https://www.etenders.gov.za/Home/opportunities?id=1'}});
 const body=await response.text();
 console.log('OFFICIAL PORTAL HTTP '+response.status);
 if(!response.ok)throw new Error('Official portal data endpoint HTTP '+response.status+': '+body.slice(0,500));
 const parsed=JSON.parse(body);if(!Array.isArray(parsed.data))throw new Error('Missing data list; top-level keys: '+Object.keys(parsed).join(','));
 console.log(JSON.stringify({ok:true,source:'Official eTenders Opportunities JSON',recordsTotal:parsed.recordsTotal,recordsFiltered:parsed.recordsFiltered,count:parsed.data.length,firstKeys:Object.keys(parsed.data[0]??{}).slice(0,20)}));
}catch(error){console.error('PORTAL PROBE FAILED:',String(error));process.exitCode=1}
