import { Router } from '../router';
import { jsonOk } from '../response';
import { Env, PostRow, PublicPost } from '../types';
import { toPublicPost } from '../mappers';

export function registerTaxonomyRoutes(router: Router): void {
  router.get('/api/categories', async (_req, env) => {
    const rows = await env.DB.prepare(
      'SELECT c.*, (SELECT COUNT(*) FROM posts p WHERE p.category = c.name_zh AND p.status = "published") AS count FROM categories c ORDER BY c.sort ASC'
    ).all();
    return jsonOk(rows.results);
  });

  router.get('/api/tags', async (_req, env) => {
    const rows = await env.DB.prepare(
      "SELECT tags FROM posts WHERE status = 'published'"
    ).all();
    const counts = new Map<string, number>();
    for (const row of rows.results) {
      try {
        const tags: string[] = JSON.parse(row.tags as string);
        for (const t of tags) counts.set(t, (counts.get(t) ?? 0) + 1);
      } catch { /* skip */ }
    }
    const tagList = [...counts.entries()]
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);
    return jsonOk(tagList);
  });

  router.get('/api/archives', async (_req, env) => {
    const rows = await env.DB.prepare(
      "SELECT * FROM posts WHERE status = 'published' ORDER BY created_at DESC"
    ).all<PostRow>();
    const posts = rows.results.map(toPublicPost);
    const map = new Map<string, { year: string; month: string; posts: PublicPost[] }>();
    for (const p of posts) {
      const key = p.createdAt.slice(0, 7);
      if (!map.has(key)) {
        map.set(key, { year: p.createdAt.slice(0, 4), month: p.createdAt.slice(5, 7), posts: [] });
      }
      map.get(key)!.posts.push(p);
    }
    return jsonOk([...map.values()]);
  });

  router.get('/api/search', async (req, env) => {
    const url = new URL(req.url);
    const q = (url.searchParams.get('q') ?? '').trim();
    if (!q) return jsonOk({ posts: [] });
    const kw = `%${q}%`;
    const rows = await env.DB.prepare(
      "SELECT * FROM posts WHERE status = 'published' AND (title_zh LIKE ? OR title_en LIKE ? OR summary LIKE ? OR content LIKE ?) ORDER BY created_at DESC LIMIT 50"
    ).bind(kw, kw, kw, kw).all<PostRow>();
    return jsonOk({ posts: rows.results.map(toPublicPost) });
  });
}