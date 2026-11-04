import pg from 'pg';
import {readFile} from 'node:fs/promises';
if(!process.env.DATABASE_URL){console.error('DATABASE_URL is required');process.exit(1)}
const client=new pg.Client({connectionString:process.env.DATABASE_URL});
try{await client.connect();await client.query(await readFile(new URL('../sql/schema.sql',import.meta.url),'utf8'));console.log('TenderBase schema ready')}finally{await client.end()}
