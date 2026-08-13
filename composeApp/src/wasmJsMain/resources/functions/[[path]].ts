const WORKER_ORIGIN = 'https://hyperos-blog-worker.tianlu4-5.workers.dev';
const BOT_REGEX = /(googlebot|bingbot|baiduspider|yandex|duckduckbot|facebookexternalhit|twitterbot|slurp|semrushbot|ahrefsbot|petalbot|bytespider|applebot|yisouspider)/i;

async function proxy(request: Request, pathname: string, search: string): Promise<Response> {
  const target = new URL(pathname + search, WORKER_ORIGIN);
  const init: RequestInit = {
    method: request.method,
    headers: request.headers,
    body: ['GET', 'HEAD'].includes(request.method) ? undefined : request.body,
    redirect: 'follow',
  };
  const upstream = await fetch(target.toString(), init);
  return new Response(upstream.body, {
    status: upstream.status,
    headers: upstream.headers,
  });
}

export async function onRequest(context: EventContext): Promise<Response> {
  const { request } = context;
  const url = new URL(request.url);
  const userAgent = request.headers.get('User-Agent') ?? '';

  if (url.pathname.startsWith('/api/')) {
    return proxy(request, url.pathname, url.search);
  }

  const isBot = BOT_REGEX.test(userAgent);
  const isArticleOrHome = /^\/(post\/[\w-]+|\/?$)/.test(url.pathname);

  if (isBot && isArticleOrHome) {
    return proxy(request, url.pathname, url.search);
  }

  return context.next();
}