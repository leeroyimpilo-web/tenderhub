import {test} from 'node:test';
import assert from 'node:assert/strict';
import {normalizeRelease,releaseArray} from '../scripts/normalize-ocds.mjs';
test('normalises an OCDS fixture and preserves official document links',()=>{
 const r=normalizeRelease({ocid:'ocds-9t57fa-fixture',date:'2026-11-01T09:00:00Z',buyer:{name:'Test Department'},tender:{id:'RFQ-EXAMPLE',title:'Fixture test tender',mainProcurementCategory:'services',tenderPeriod:{endDate:'2026-11-20T12:00:00Z'},documents:[{id:'d1',title:'Terms',url:'https://www.etenders.gov.za/abc.pdf'}]}});
 assert.equal(r.id,'ocds-9t57fa-fixture');assert.equal(r.buyer,'Test Department');assert.equal(r.documents.length,1);
});
test('does not invent records when data invalid',()=>{assert.equal(normalizeRelease({tender:{title:'Missing OCID'}}),null);assert.throws(()=>releaseArray({data:[]}));});
test('ignores unsafe document schemes',()=>{const r=normalizeRelease({ocid:'test',tender:{title:'Test',documents:[{url:'javascript:alert(1)'}]}});assert.equal(r.documents.length,0)});
