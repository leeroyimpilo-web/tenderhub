import {test} from 'node:test';
import assert from 'node:assert/strict';
import {normalizePortalOpportunity,validatePortalPage,portalDocuments} from '../scripts/normalize-portal.mjs';
test('normalizes a first-party eTenders opportunity fixture',()=>{
 const row={id:73,tender_No:'TEST/2026',description:'Fixture only',category:'services',organ_of_State:'Demo Department',province:'KwaZulu-Natal',date_Published:'2026-11-02',closing_Date:'2026-11-28',supportDocument:[{id:'doc1',blobName:'abc.pdf',downloadedFileName:'specification.pdf'}]};
 const t=normalizePortalOpportunity(row);
 assert.equal(t.id,'etenders-portal-73');
 assert.equal(t.reference,'TEST/2026');
 assert.equal(t.documents.length,1);
 assert.match(t.documents[0].url,/etenders.gov.za\/home\/Download/);
});
test('does not fabricate missing IDs',()=>assert.equal(normalizePortalOpportunity({description:'Missing ID'}),null));
test('rejects malformed official portal pages',()=>assert.throws(()=>validatePortalPage({releases:[]})));

test('resolves current portal document metadata into official download URL',()=>{
 const t=normalizePortalOpportunity({id:42,description:'Fixture',supportDocument:[{supportDocumentID:'e3b7c098-775a-4f54-bc18-fd4cef5b1da3',extension:'.pdf',fileName:'Specifications.pdf'}]});
 assert.equal(t.documents.length,1);
 assert.match(t.documents[0].url,/blobName=e3b7c098-775a-4f54-bc18-fd4cef5b1da3.pdf/);
 assert.match(t.documents[0].url,/downloadedFileName=Specifications.pdf/);
});

test('links official notice records to their portal URL',()=>{const notice=normalizePortalOpportunity({id:176624,description:'Fixture'});assert.equal(notice.source_url,'https://www.etenders.gov.za/Home/?id=176624')});
