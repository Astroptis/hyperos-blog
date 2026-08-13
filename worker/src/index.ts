import { Router } from './router';
import { jsonOk, jsonError, handleCors, withCors } from './response';
import { Env, PublicPost } from './types';
import { registerAuthRoutes } from './routes/auth';
import { registerPostRoutes } from './routes/posts';
import { registerTaxonomyRoutes } from './routes/taxonomy';
import { registerInteractionRoutes } from './routes/interactions';
import { registerSiteRoutes } from './routes/site';
import { registerAdminRoutes } from './routes/admin';
import { isBot, renderPostHTML, renderHomeHTML } from './seo';

const router = new Router();

router.get('/api/health', async () => {
  return jsonOk({ status: 'ok', time: new Date().toISOString() });
});

registerAuthRoutes(router);
registerPostRoutes(router);
registerTaxonomyRoutes(router);
registerInteractionRoutes(router);
registerSiteRoutes(router);
registerAdminRoutes(router);

async function loadSettings(env: Env): Promise<Record<string, string>> {
  const rows = await env.DB.prepare('SELECT key, value FROM settings').all();
  const settings: Record<string, string> = {};
  for (const row of rows.results) {
    settings[row.key as string] = row.value as string;
  }
  return settings;
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const corsResponse = handleCors(request);
    if (corsResponse) return corsResponse;

    const url = new URL(request.url);
    const userAgent = request.headers.get('User-Agent') ?? '';

    if (url.pathname.startsWith('/api/')) {
      const res = await router.serve(request.method, url.pathname, request, env, ctx);
      if (res) return withCors(res);
      return withCors(jsonError(404, 'Not Found'));
    }

    if (isBot(userAgent)) {
      const settings = await loadSettings(env);
      const siteTitle = settings.title ?? 'HyperOS 博客';

      const postMatch = /^\/post\/([^/]+)$/.exec(url.pathname);
      if (postMatch) {
        const slug = postMatch[1];
        const row = await env.DB.prepare(
          "SELECT * FROM posts WHERE slug = ? AND status = 'published'"
        ).bind(slug).first<PublicPost>();
        if (row) {
          return new Response(renderPostHTML(row, siteTitle), {
            headers: { 'Content-Type': 'text/html; charset=utf-8' },
          });
        }
        return new Response('<!DOCTYPE html><html><body><h1>404</h1></body></html>', {
          status: 404,
          headers: { 'Content-Type': 'text/html; charset=utf-8' },
        });
      }

      const homeMatch = /^\/$/.test(url.pathname);
      if (homeMatch) {
        const rows = await env.DB.prepare(
          "SELECT * FROM posts WHERE status = 'published' ORDER BY created_at DESC LIMIT 10"
        ).all<PublicPost>();
        return new Response(renderHomeHTML(siteTitle, rows.results, settings), {
          headers: { 'Content-Type': 'text/html; charset=utf-8' },
        });
      }
    }

    return new Response('SPA fallback', { status: 200 });
  },
} satisfies ExportedHandler<Env>;