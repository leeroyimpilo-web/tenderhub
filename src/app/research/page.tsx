import type {Metadata} from 'next';
import {getResearch} from '@/lib/tenders';
import {InsightsDashboard} from '@/components/InsightsDashboard';
export const metadata:Metadata={title:'Insights'};
export const dynamic='force-dynamic';
export default async function Research(){try{const data=await getResearch();return <InsightsDashboard total={data.total} provinces={data.provinces} buyers={data.buyers} categories={data.categories}/>}catch(error){console.error('[insights]',error);return <InsightsDashboard total={0} provinces={[]} buyers={[]} categories={[]}/>}}
