import type {Metadata,Viewport} from 'next';
import './globals.css';
import {AppShell} from '@/components/AppShell';
export const metadata:Metadata={title:{default:'TenderBase — eTenders intelligence',template:'%s | TenderBase'},description:'Search and track South African National Treasury eTenders data. No account required.'};
export const viewport:Viewport={width:'device-width',initialScale:1,viewportFit:'cover',themeColor:'#071a30'};
export default function RootLayout({children}:{children:React.ReactNode}){
 return <html lang="en-ZA"><body><AppShell>{children}</AppShell></body></html>;
}
