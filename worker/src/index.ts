import { Router } from './router';
import { jsonOk, jsonError, handleCors, withCors } from './response';
import { Env } from './types';
import { registerAuthRoutes } from './routes/auth';
import { registerPostRoutes } from './routes/posts';
import { registerTaxonomyRoutes } from './routes/taxonomy';

const router = new Router();

router.get('/api/health', async () => {
  return jsonOk({ status: 'ok', time: new Date().toISOString() });
});

registerAuthRoutes(router);
registerPostRoutes(router);
registerTaxonomyRoutes(router);

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const corsResponse = handleCors(request);
    if (corsResponse) return corsResponse;

    const url = new URL(request.url);

    if (url.pathname.startsWith('/api/')) {
      const res = await router.serve(request.method, url.pathname, request, env, ctx);
      if (res) return withCors(res);
      return withCors(jsonError(404, 'Not Found'));
    }

    return withCors(jsonError(404, 'Not Found'));
  },
} satisfies ExportedHandler<Env>;