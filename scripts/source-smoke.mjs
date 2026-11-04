const url='https://ocds-api.etenders.gov.za/api/OCDSReleases?PageNumber=1&PageSize=1';
try{
 const response=await fetch(url,{headers:{accept:'application/json','user-agent':'TenderBaseV2/0.1 source-health-check'},signal:AbortSignal.timeout(35000)});
 if(!response.ok)throw new Error('Official eTenders API returned HTTP '+response.status);
 const data=await response.json();
 if(!data||!Array.isArray(data.releases))throw new Error('Unexpected official OCDS response: missing releases array');
 const sample=data.releases[0]??null;
 console.log(JSON.stringify({ok:true,status:response.status,source:'National Treasury OCDS',releasesReturned:data.releases.length,firstOcid:sample?.ocid??null}));
}catch(error){console.error('OFFICIAL SOURCE TEST FAILED:',String(error));process.exitCode=1}
