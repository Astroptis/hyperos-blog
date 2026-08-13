import { Router } from '../router';
import { jsonOk, jsonError } from '../response';
import { Env, PostRow, PublicPost } from '../types';
import { toPublicPost } from '../mappers';
import { requireAdmin } from '../auth';

export function registerPostRoutes(router: Router): void {
  router.get('/api/posts', async (req, env) => {
    const url = new URL(req.url);
    const page = Math.max(1, parseInt(url.searchParams.get('page') ?? '1', 10) || 1);
    const pageSize = Math.min(50, Math.max(1, parseInt(url.searchParams.get('pageSize') ?? '10', 10) || 10));
    const category = url.searchParams.get('category') ?? '';
    const tag = url.searchParams.get('tag') ?? '';
    const keyword = url.searchParams.get('keyword') ?? '';

    const where: string[] = ["status = 'published'"];
    const params: unknown[] = [];
    if (category) { where.push('category = ?'); params.push(category); }
    if (tag) { where.push('tags LIKE ?'); params.push(`%"${tag}"%`); }
    if (keyword) {
      where.push('(title_zh LIKE ? OR title_en LIKE ? OR summary LIKE ? OR content LIKE ?)');
      const kw = `%${keyword}%`;
      params.push(kw, kw, kw, kw);
    }
    const whereSql = where.join(' AND ');
    params.push(pageSize, (page - 1) * pageSize);

    const totalRow = await env.DB.prepare(`SELECT COUNT(*) AS total FROM posts WHERE ${whereSql}`)
      .bind(...params.slice(0, params.length - 2))
      .first();
    const rows = await env.DB.prepare(
      `SELECT * FROM posts WHERE ${whereSql}
       ORDER BY pinned DESC, created_at DESC LIMIT ? OFFSET ?`
    ).bind(...params).all();

    const posts: PublicPost[] = (rows.results as unknown as PostRow[]).map(toPublicPost);
    const total = Number(totalRow?.total ?? 0);
    return jsonOk({
      posts,
      total,
      page,
      pageSize,
      totalPages: Math.ceil(total / pageSize),
    });
  });

  router.get('/api/posts/:slug', async (req, env) => {
    const slug = req.url.split('/').pop()!.split('?')[0];
    const username = await requireAdmin(req, env);
    let row: PostRow | null = null;
    if (username) {
      row = await env.DB.prepare('SELECT * FROM posts WHERE slug = ?').bind(slug).first<PostRow>();
    } else {
      row = await env.DB.prepare("SELECT * FROM posts WHERE slug = ? AND status = 'published'").bind(slug).first<PostRow>();
    }
    if (!row) return jsonError(404, 'Post not found');
    return jsonOk(toPublicPost(row));
  });
}