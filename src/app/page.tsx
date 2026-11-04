import {getDashboard} from '@/lib/tenders';
import {DiscoveryHome} from '@/components/DiscoveryHome';
import {DataError} from '@/components/DataState';
export const dynamic='force-dynamic';
export default async function DiscoverPage(){
 try{const data=await getDashboard();return <DiscoveryHome latest={data.latest} closing={data.closing} count={Number(data.stats.open)} lastSync={data.lastSync}/>}
 catch(error){console.error('[discover]',error);return <main className="tb-discover"><h1>Opportunities<br/>for your business.</h1><p className="tb-discover-sub">National Treasury eTenders</p><DataError/></main>}
}
