export function parseOcdsPayload(value){
 if(Array.isArray(value))return value.flatMap(parseOcdsPayload);
 if(!value||typeof value!=='object')throw new Error('Invalid OCDS release data');
 if(Array.isArray(value.releases))return value.releases;
 if(value.ocid&&value.tender)return [value];
 throw new Error('File does not contain OCDS releases or an OCDS release package');
}
export function parseOcdsText(text){
 const body=text.trim();
 if(!body)throw new Error('Empty input file');
 try{return parseOcdsPayload(JSON.parse(body))}catch(err){
  if(!body.includes('\n'))throw err;
  const lines=body.split(/\r?\n/).filter(line=>line.trim());
  try{return lines.flatMap((line,i)=>{try{return parseOcdsPayload(JSON.parse(line))}catch(e){throw new Error('Invalid OCDS JSON line '+(i+1)+': '+String(e))}})}
  catch(lineErr){throw new Error('Unable to read OCDS JSON/JSONL: '+String(lineErr))}
 }
}
