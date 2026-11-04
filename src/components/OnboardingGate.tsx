'use client';
import {useEffect} from 'react';
import {usePathname,useRouter} from 'next/navigation';
import {useLocal} from '@/components/LocalProvider';
export function OnboardingGate(){const {data,ready}=useLocal();const path=usePathname();const router=useRouter();useEffect(()=>{if(ready&&!data.settings.onboardingComplete&&path!=='/onboarding')router.replace('/onboarding')},[ready,data.settings.onboardingComplete,path,router]);return null}
