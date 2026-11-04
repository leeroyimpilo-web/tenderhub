import {test} from 'node:test';
import assert from 'node:assert/strict';
import {parseOcdsText,parseOcdsPayload} from '../scripts/parse-ocds-file.mjs';
const sample={ocid:'ocds-9t57fa-fixture',tender:{title:'Unit test only'}};
test('reads official-style release package',()=>assert.equal(parseOcdsText(JSON.stringify({releases:[sample]})).length,1));
test('reads JSON lines packages',()=>assert.equal(parseOcdsText([JSON.stringify(sample),JSON.stringify(sample)].join('\n')).length,2));
test('rejects a non-OCDS file',()=>assert.throws(()=>parseOcdsPayload({results:[sample]})));
