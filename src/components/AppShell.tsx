import {LocalProvider} from '@/components/LocalProvider';
import {Navigation} from '@/components/Navigation';
import {TopBar} from '@/components/TopBar';
import {OnboardingGate} from '@/components/OnboardingGate';
export function AppShell({children}:{children:React.ReactNode}){
 return <LocalProvider><OnboardingGate/><div className="tb-v2-shell"><TopBar/>{children}<Navigation/></div></LocalProvider>;
}
