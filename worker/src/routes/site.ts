import { Router } from '../router';
import { jsonOk, jsonError } from '../response';
import { Env, FriendRow } from '../types';
import { requireAdmin } from '../auth';

export function registerSiteRoutes(router: Router): void {
  router.get('/api/friends', async (_req, env) => {
    const rows = await env.DB.prepare('SELECT * FROM friends ORDER BY sort ASC, id ASC').all();
    return jsonOk(rows.results);
  });

  router.post('/api/friends', async (req, env) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    let body: Partial<FriendRow>;
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }
    const name = (body.name ?? '').trim().slice(0, 50);
    const url = (body.url ?? '').trim().slice(0, 200);
    if (!name || !url) return jsonError(400, 'Name and url are required');
    const avatar = (body.avatar ?? '').trim().slice(0, 500);
    const description = (body.description ?? '').trim().slice(0, 200);
    const sort = Number(body.sort ?? 0);
    const result = await env.DB.prepare(
      'INSERT INTO friends (name, url, avatar, description, sort) VALUES (?, ?, ?, ?, ?)'
    ).bind(name, url, avatar, description, sort).run();
    return jsonOk({ id: result.meta.last_row_id, name, url, avatar, description, sort }, { status: 201 });
  });

  router.put('/api/friends/:id', async (req, env, _ctx, params) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const id = parseInt(params.id, 10);
    let body: Partial<FriendRow>;
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }
    const name = (body.name ?? '').trim().slice(0, 50);
    const url = (body.url ?? '').trim().slice(0, 200);
    const avatar = (body.avatar ?? '').trim().slice(0, 500);
    const description = (body.description ?? '').trim().slice(0, 200);
    const sort = Number(body.sort ?? 0);
    await env.DB.prepare('UPDATE friends SET name=?, url=?, avatar=?, description=?, sort=? WHERE id=?')
      .bind(name, url, avatar, description, sort, id).run();
    return jsonOk({ id, name, url, avatar, description, sort });
  });

  router.delete('/api/friends/:id', async (req, env, _ctx, params) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const id = parseInt(params.id, 10);
    await env.DB.prepare('DELETE FROM friends WHERE id = ?').bind(id).run();
    return jsonOk({ deleted: true });
  });

  router.get('/api/stats', async (_req, env) => {
    const posts = await env.DB.prepare("SELECT COUNT(*) AS c FROM posts WHERE status = 'published'").first();
    const comments = await env.DB.prepare('SELECT COUNT(*) AS c FROM comments').first();
    const messages = await env.DB.prepare('SELECT COUNT(*) AS c FROM messages').first();
    const views = await env.DB.prepare('SELECT SUM(view_count) AS s FROM posts').first();
    let totalVisits = 0;
    const list = await env.KV.list({ prefix: 'visit:' });
    for (const key of list.keys) {
      totalVisits += parseInt((await env.KV.get(key.name)) ?? '0', 10);
    }
    return jsonOk({
      postCount: posts?.c ?? 0,
      commentCount: comments?.c ?? 0,
      messageCount: messages?.c ?? 0,
      viewCount: views?.s ?? 0,
      totalVisits,
    });
  });

  router.get('/api/settings', async (_req, env) => {
    const rows = await env.DB.prepare('SELECT key, value FROM settings').all();
    const settings: Record<string, string> = {};
    for (const row of rows.results) {
      settings[row.key as string] = row.value as string;
    }
    return jsonOk(settings);
  });

  router.put('/api/settings', async (req, env) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    let body: Record<string, unknown>;
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }
    const stmt = env.DB.prepare('INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value');
    const batch = Object.entries(body).map(([k, v]) => stmt.bind(k, String(v).slice(0, 2000)));
    await env.DB.batch(batch);
    return jsonOk({ updated: true });
  });
}