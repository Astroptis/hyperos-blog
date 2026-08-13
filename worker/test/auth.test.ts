import { test } from 'node:test';
import assert from 'node:assert';
import { hashPassword, verifyPassword } from '../src/auth';

test('hash and verify password roundtrip', async () => {
  const hash = await hashPassword('secret123');
  assert.ok(hash.startsWith('pbkdf2:'));
  assert.equal(await verifyPassword('secret123', hash), true);
  assert.equal(await verifyPassword('wrong', hash), false);
});