import Link from 'next/link';
export function DataError(){
 return <div className="tb-v2-error"><strong>Tender data is not available.</strong><p className="mt-2">Check database configuration, run the official OCDS sync, and retry. No example tenders are shown.</p><div className="mt-3"><Link className="underline font-semibold" href="/settings">View data status</Link></div></div>;
}
export function Empty({message='No matching official tenders were found.'}:{message?:string}){
 return <div className="tb-v2-nodata">{message}</div>;
}
