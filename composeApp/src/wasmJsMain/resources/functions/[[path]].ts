const WORKER_ORIGIN = 'https://hyperos-blog-worker.tianlu4-5.workers.dev';
const BOT_REGEX = /(googlebot|bingbot|baiduspider|yandex|duckduckbot|facebookexternalhit|twitterbot|slurp|semrushbot|ahrefsbot|petalbot|bytespider|applebot|yisouspider)/i;

export async function onRequest(context: EventContext): Promise<Response> {
  const { request } = context;
  const url = new URL(request.url);
  const userAgent = request.headers.get('User-Agent') ?? '';

  const isBot = BOT_REGEX.test(userAgent);
  const isArticleOrHome = /^\/(post\/[\w-]+|\/?$)/.test(url.pathname);

  if (isBot && isArticleOrHome) {
    const target = new URL(url.pathname + url.search, WORKER_ORIGIN);
    const upstream = await fetch(target.toString(), {
      headers: request.headers,
      redirect: 'follow',
    });
    return new Response(upstream.body, {
      status: upstream.status,
      headers: upstream.headers,
    });
  }

  return context.next();
}