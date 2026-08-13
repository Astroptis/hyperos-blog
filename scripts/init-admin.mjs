import { execSync } from 'node:child_process';

const [,, username, password, databaseId] = process.argv;
if (!username || !password) {
  console.error('Usage: node init-admin.mjs <username> <password> [databaseId]');
  process.exit(1);
}

async function hashPassword(password, iterations = 100000) {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const keyMaterial = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, ['deriveBits']);
  const bits = await crypto.subtle.deriveBits({ name: 'PBKDF2', salt, iterations, hash: 'SHA-256' }, keyMaterial, 256);
  const toHex = (b) => Array.from(b, (x) => x.toString(16).padStart(2, '0')).join('');
  return `pbkdf2:${iterations}:${toHex(salt)}:${toHex(new Uint8Array(bits))}`;
}

const hash = await hashPassword(password);
const escapedHash = hash.replace(/'/g, "''");
const now = new Date().toISOString();

const dbFlag = databaseId ? `--remote --database-id=${databaseId}` : '--local';
const sql = `INSERT INTO admin (username, password_hash, created_at) VALUES ('${username}', '${escapedHash}', '${now}') ON CONFLICT(username) DO UPDATE SET password_hash = excluded.password_hash;`;
execSync(`npx wrangler d1 execute hyperos-blog ${dbFlag} --command="${sql.replace(/"/g, '\\"')}"`, { stdio: 'inherit' });
console.log('Admin created/updated:', username);