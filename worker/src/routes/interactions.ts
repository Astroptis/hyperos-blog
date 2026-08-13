import { Router } from '../router';
import { jsonOk, jsonError } from '../response';
import { Env } from '../types';
import { requireAdmin } from '../auth';

async function ipHash(req: Request): Promise<string> {
  const ip = req.headers.get('CF-Connecting-IP') ?? 'unknown';
  const data = new TextEncoder().encode(ip);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(digest).slice(0, 8), (b) => b.toString(16).padStart(2, '0')).join('');
}

function validateText(value: string, max: number): string {
  return value.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '').slice(0, max);
}

export function registerInteractionRoutes(router: Router): void {
  router.get('/api/posts/:id/comments', async (_req, env, _ctx, params) => {
    const id = parseInt(params.id, 10);
    if (Number.isNaN(id)) return jsonError(400, 'Invalid post id');
    const rows = await env.DB.prepare(
      'SELECT id, nickname, content, created_at AS createdAt FROM comments WHERE post_id = ? ORDER BY created_at ASC'
    ).bind(id).all();
    return jsonOk(rows.results);
  });

  router.post('/api/posts/:id/comments', async (req, env, _ctx, params) => {
    const id = parseInt(params.id, 10);
    if (Number.isNaN(id)) return jsonError(400, 'Invalid post id');
    const post = await env.DB.prepare('SELECT id FROM posts WHERE id = ? AND status = "published"').bind(id).first();
    if (!post) return jsonError(404, 'Post not found');

    let body: { nickname?: string; email?: string; content?: string };
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }

    const nickname = validateText((body.nickname ?? '').trim(), 50);
    const email = validateText((body.email ?? '').trim().toLowerCase(), 100);
    const content = validateText((body.content ?? '').trim(), 2000);
    if (!nickname || !content) return jsonError(400, 'Nickname and content are required');

    const result = await env.DB.prepare(
      'INSERT INTO comments (post_id, nickname, email, content, created_at) VALUES (?, ?, ?, ?, ?)'
    ).bind(id, nickname, email, content, new Date().toISOString()).run();
    const commentId = result.meta.last_row_id;
    return jsonOk({ id: commentId, nickname, content, createdAt: new Date().toISOString() }, { status: 201 });
  });

  router.post('/api/posts/:id/like', async (req, env, _ctx, params) => {
    const id = parseInt(params.id, 10);
    if (Number.isNaN(id)) return jsonError(400, 'Invalid post id');
    const h = await ipHash(req);
    const dedupeKey = `like:${id}:${h}`;
    const existing = await env.KV.get(dedupeKey);
    if (existing) return jsonOk({ liked: false, message: 'Already liked' });
    await env.KV.put(dedupeKey, '1', { expirationTtl: 24 * 3600 });
    await env.DB.prepare('UPDATE posts SET like_count = like_count + 1 WHERE id = ?').bind(id).run();
    const row = await env.DB.prepare('SELECT like_count FROM posts WHERE id = ?').bind(id).first();
    return jsonOk({ liked: true, likeCount: row?.like_count ?? 0 });
  });

  router.post('/api/posts/:id/view', async (req, env, _ctx, params) => {
    const id = parseInt(params.id, 10);
    if (Number.isNaN(id)) return jsonError(400, 'Invalid post id');
    const h = await ipHash(req);
    const dedupeKey = `view:${id}:${h}`;
    const existing = await env.KV.get(dedupeKey);
    if (!existing) {
      await env.KV.put(dedupeKey, '1', { expirationTtl: 24 * 3600 });
      await env.DB.prepare('UPDATE posts SET view_count = view_count + 1 WHERE id = ?').bind(id).run();
    }
    const row = await env.DB.prepare('SELECT view_count FROM posts WHERE id = ?').bind(id).first();
    return jsonOk({ viewCount: row?.view_count ?? 0 });
  });

  router.post('/api/visit', async (req, env) => {
    const h = await ipHash(req);
    const today = new Date().toISOString().slice(0, 10);
    const dedupeKey = `visit:${today}:${h}`;
    const existing = await env.KV.get(dedupeKey);
    if (!existing) {
      await env.KV.put(dedupeKey, '1', { expirationTtl: 2 * 24 * 3600 });
      await env.KV.put(`visit:${today}`, String((parseInt((await env.KV.get(`visit:${today}`)) ?? '0', 10)) + 1), {
        expirationTtl: 40 * 24 * 3600,
      });
    }
    const count = parseInt((await env.KV.get(`visit:${today}`)) ?? '0', 10);
    return jsonOk({ visits: count });
  });

  router.get('/api/messages', async (_req, env) => {
    const rows = await env.DB.prepare(
      'SELECT id, nickname, content, created_at AS createdAt FROM messages ORDER BY created_at DESC LIMIT 100'
    ).all();
    return jsonOk(rows.results);
  });

  router.post('/api/messages', async (req, env) => {
    let body: { nickname?: string; email?: string; content?: string };
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }
    const nickname = validateText((body.nickname ?? '').trim(), 50);
    const email = validateText((body.email ?? '').trim().toLowerCase(), 100);
    const content = validateText((body.content ?? '').trim(), 1000);
    if (!nickname || !content) return jsonError(400, 'Nickname and content are required');
    const result = await env.DB.prepare(
      'INSERT INTO messages (nickname, email, content, created_at) VALUES (?, ?, ?, ?)'
    ).bind(nickname, email, content, new Date().toISOString()).run();
    return jsonOk({ id: result.meta.last_row_id, nickname, content, createdAt: new Date().toISOString() }, { status: 201 });
  });

  router.delete('/api/messages/:id', async (req, env, _ctx, params) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const id = parseInt(params.id, 10);
    await env.DB.prepare('DELETE FROM messages WHERE id = ?').bind(id).run();
    return jsonOk({ deleted: true });
  });

  router.delete('/api/comments/:id', async (req, env, _ctx, params) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const id = parseInt(params.id, 10);
    await env.DB.prepare('DELETE FROM comments WHERE id = ?').bind(id).run();
    return jsonOk({ deleted: true });
  });
}