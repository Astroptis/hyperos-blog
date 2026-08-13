import { Env } from './types';

const ITERATIONS = 100_000;

function toHex(bytes: Uint8Array): string {
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
}

function fromHex(hex: string): Uint8Array {
  const bytes = new Uint8Array(hex.length / 2);
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = parseInt(hex.slice(i * 2, i * 2 + 2), 16);
  }
  return bytes;
}

export async function hashPassword(password: string): Promise<string> {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(password),
    'PBKDF2',
    false,
    ['deriveBits']
  );
  const bits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', salt, iterations: ITERATIONS, hash: 'SHA-256' },
    keyMaterial,
    256
  );
  return `pbkdf2:${ITERATIONS}:${toHex(salt)}:${toHex(new Uint8Array(bits))}`;
}

export async function verifyPassword(password: string, stored: string): Promise<boolean> {
  const [algo, iterStr, saltHex, hashHex] = stored.split(':');
  if (algo !== 'pbkdf2') return false;
  const iterations = parseInt(iterStr, 10);
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(password),
    'PBKDF2',
    false,
    ['deriveBits']
  );
  const bits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', salt: fromHex(saltHex), iterations, hash: 'SHA-256' },
    keyMaterial,
    256
  );
  const actual = toHex(new Uint8Array(bits));
  let equal = actual.length === hashHex.length;
  for (let i = 0; i < actual.length; i++) {
    if (actual.charCodeAt(i) !== hashHex.charCodeAt(i)) equal = false;
  }
  return equal;
}

export async function createSession(env: Env, username: string): Promise<string> {
  const tokenBytes = crypto.getRandomValues(new Uint8Array(32));
  const token = toHex(tokenBytes);
  await env.KV.put(`session:${token}`, JSON.stringify({ username }), {
    expirationTtl: 7 * 24 * 3600,
  });
  return token;
}

export async function deleteSession(env: Env, token: string): Promise<void> {
  await env.KV.delete(`session:${token}`);
}

export async function requireAdmin(req: Request, env: Env): Promise<string | null> {
  const authHeader = req.headers.get('Authorization');
  if (!authHeader || !authHeader.startsWith('Bearer ')) return null;
  const token = authHeader.slice(7).trim();
  const raw = await env.KV.get(`session:${token}`);
  if (!raw) return null;
  try {
    return (JSON.parse(raw) as { username: string }).username;
  } catch {
    return null;
  }
}