export interface TenderDocument { id:string; title:string; url:string; format:string|null; publishedAt:string|null }
export interface Tender { id:string; reference:string|null; title:string; description:string|null; buyer:string|null; province:string|null; category:string|null; status:string|null; published_at:string|null; closing_at:string|null; release_at:string|null; source_url:string; documents:TenderDocument[]; raw:Record<string,unknown>; synced_at:string }
export interface TenderFilter { q?:string;province?:string;category?:string;buyer?:string;status?:string;sort?:string;page?:number;pageSize?:number }
export interface TenderPage { items:Tender[];total:number;page:number;pages:number }
export type PipelineStage='New match'|'Review'|'Bid planning'|'Preparing'|'Ready to submit'|'Submitted'|'Award pending'|'Won'|'Lost';
export const STAGES:PipelineStage[]=['New match','Review','Bid planning','Preparing','Ready to submit','Submitted','Award pending','Won','Lost'];
export interface PipelineRecord { tender:Tender;stage:PipelineStage;notes:string;updatedAt:string }
export interface LocalSettings { defaultProvince:string;remindDays:number }
export interface LocalData {saved:Tender[];pipeline:PipelineRecord[];dismissedAlerts:string[];settings:LocalSettings}
