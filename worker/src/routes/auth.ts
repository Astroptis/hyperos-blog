import { Router } from '../router';
import { jsonOk, jsonError } from '../response';
import { Env } from '../types';
import { hashPassword, verifyPassword, createSession, deleteSession, requireAdmin } from '../auth';

export function registerAuthRoutes(router: Router): void {
  router.post('/api/auth/login', async (req, env) => {
    let body: { username?: string; password?: string };
    try {
      body = await req.json();
    } catch {
      return jsonError(400, 'Invalid JSON body');
    }
    const username = (body.username ?? '').trim();
    const password = body.password ?? '';
    if (!username || !password) {
      return jsonError(400, 'Username and password are required');
    }
    const row = await env.DB.prepare('SELECT * FROM admin WHERE username = ?').bind(username).first();
    if (!row) return jsonError(401, 'Invalid credentials');
    const valid = await verifyPassword(password, row.password_hash as string);
    if (!valid) return jsonError(401, 'Invalid credentials');
    const token = await createSession(env, username);
    return jsonOk({ token, username });
  });

  router.post('/api/auth/logout', async (req, env) => {
    const authHeader = req.headers.get('Authorization');
    if (authHeader && authHeader.startsWith('Bearer ')) {
      await deleteSession(env, authHeader.slice(7).trim());
    }
    return jsonOk({ loggedOut: true });
  });

  router.get('/api/auth/me', async (req, env) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    return jsonOk({ username });
  });
}