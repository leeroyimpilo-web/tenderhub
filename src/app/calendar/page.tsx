import type {Metadata} from 'next';
import {getUpcoming} from '@/lib/tenders';
import {DeadlineDashboard} from '@/components/DeadlineDashboard';
export const dynamic='force-dynamic';
export const metadata:Metadata={title:'Deadlines'};
export default async function CalendarPage(){try{return <DeadlineDashboard records={await getUpcoming(90)}/>}catch(error){console.error('[calendar]',error);return <DeadlineDashboard records={[]}/>}}
