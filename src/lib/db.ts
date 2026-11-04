import 'server-only';
import {Pool} from 'pg';
declare global {var tenderhubPool:Pool|undefined}
export function getPool():Pool{
  if(!process.env.DATABASE_URL) throw new Error('Database not configured: set DATABASE_URL and run npm run db:init.');
  if(!globalThis.tenderhubPool) globalThis.tenderhubPool=new Pool({connectionString:process.env.DATABASE_URL,max:5,connectionTimeoutMillis:10000,idleTimeoutMillis:30000});
  return globalThis.tenderhubPool;
}
