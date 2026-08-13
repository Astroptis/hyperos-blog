import { Router } from '../router';
import { jsonOk, jsonError } from '../response';
import { Env, PostRow, PublicPost } from '../types';
import { toPublicPost } from '../mappers';
import { requireAdmin } from '../auth';

interface PostInput {
  slug?: string;
  titleZh?: string;
  titleEn?: string;
  summary?: string;
  content?: string;
  category?: string;
  tags?: string[];
  status?: 'published' | 'draft';
  pinned?: boolean;
  featured?: boolean;
}

function toSlug(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9\u4e00-\u9fa5]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 100);
}

export function registerAdminRoutes(router: Router): void {
  router.get('/api/admin/posts', async (req, env) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const url = new URL(req.url);
    const page = Math.max(1, parseInt(url.searchParams.get('page') ?? '1', 10) || 1);
    const pageSize = Math.min(100, Math.max(1, parseInt(url.searchParams.get('pageSize') ?? '20', 10) || 20));
    const totalRow = await env.DB.prepare('SELECT COUNT(*) AS total FROM posts').first();
    const rows = await env.DB.prepare(
      'SELECT * FROM posts ORDER BY created_at DESC LIMIT ? OFFSET ?'
    ).bind(pageSize, (page - 1) * pageSize).all<PostRow>();
    return jsonOk({
      posts: rows.results.map(toPublicPost),
      total: totalRow?.total ?? 0,
      page,
      pageSize,
    });
  });

  router.post('/api/admin/posts', async (req, env) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    let body: PostInput;
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }
    const titleZh = (body.titleZh ?? '').trim().slice(0, 200);
    const content = body.content ?? '';
    if (!titleZh || !content) return jsonError(400, 'Title and content are required');

    const baseSlug = (body.slug ?? '').trim() || toSlug(titleZh);
    let slug = baseSlug;
    const existing = await env.DB.prepare('SELECT id FROM posts WHERE slug = ?').bind(slug).first();
    if (existing) slug = `${baseSlug}-${Date.now().toString(36)}`;

    const now = new Date().toISOString();
    const result = await env.DB.prepare(
      `INSERT INTO posts
        (slug, title_zh, title_en, summary, content, category, tags, status, pinned, featured, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      slug,
      titleZh,
      (body.titleEn ?? '').trim().slice(0, 200),
      (body.summary ?? '').trim().slice(0, 500),
      content,
      (body.category ?? '').trim().slice(0, 100),
      JSON.stringify(body.tags ?? []),
      body.status === 'draft' ? 'draft' : 'published',
      body.pinned ? 1 : 0,
      body.featured ? 1 : 0,
      now,
      now
    ).run();
    return jsonOk({ id: result.meta.last_row_id, slug }, { status: 201 });
  });

  router.put('/api/admin/posts/:id', async (req, env, _ctx, params) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const id = parseInt(params.id, 10);
    let body: PostInput;
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }
    const existing = await env.DB.prepare('SELECT * FROM posts WHERE id = ?').bind(id).first<PostRow>();
    if (!existing) return jsonError(404, 'Post not found');

    const titleZh = (body.titleZh ?? existing.title_zh).trim().slice(0, 200);
    const content = body.content ?? existing.content;
    const slug = (body.slug ?? existing.slug).trim() || existing.slug;
    await env.DB.prepare(
      `UPDATE posts SET
        slug = ?, title_zh = ?, title_en = ?, summary = ?, content = ?,
        category = ?, tags = ?, status = ?, pinned = ?, featured = ?, updated_at = ?
       WHERE id = ?`
    ).bind(
      slug,
      titleZh,
      (body.titleEn ?? existing.title_en).trim(),
      (body.summary ?? existing.summary).trim(),
      content,
      (body.category ?? existing.category).trim(),
      JSON.stringify(body.tags ?? JSON.parse(existing.tags)),
      body.status ?? existing.status,
      body.pinned === undefined ? existing.pinned : (body.pinned ? 1 : 0),
      body.featured === undefined ? existing.featured : (body.featured ? 1 : 0),
      new Date().toISOString(),
      id
    ).run();
    const updated = await env.DB.prepare('SELECT * FROM posts WHERE id = ?').bind(id).first<PostRow>();
    return jsonOk(toPublicPost(updated!));
  });

  router.delete('/api/admin/posts/:id', async (req, env, _ctx, params) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const id = parseInt(params.id, 10);
    await env.DB.prepare('DELETE FROM comments WHERE post_id = ?').bind(id).run();
    await env.DB.prepare('DELETE FROM posts WHERE id = ?').bind(id).run();
    return jsonOk({ deleted: true });
  });
}