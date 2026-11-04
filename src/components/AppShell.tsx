import {LocalProvider} from '@/components/LocalProvider';
import {Navigation} from '@/components/Navigation';
export function AppShell({children}:{children:React.ReactNode}){
 return <LocalProvider><div className="tb-v2-shell">{children}<Navigation/></div></LocalProvider>;
}
