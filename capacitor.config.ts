import type {CapacitorConfig} from '@capacitor/cli';
const appUrl=process.env.TENDERBASE_APP_URL;
if(!appUrl || !/^https:\/\/[^\s/]+(?:\/.*)?$/.test(appUrl)){
 throw new Error('Set a verified HTTPS TENDERBASE_APP_URL before Android packaging.');
}
const parsed=new URL(appUrl);
const config:CapacitorConfig={
 appId:'za.co.tenderbase.dev',
 appName:'TenderBase Dev',
 webDir:'capacitor-www',
 server:{url:parsed.origin,androidScheme:'https',cleartext:false},
 android:{allowMixedContent:false}
};
export default config;
