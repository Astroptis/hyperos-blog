import { test } from 'node:test';
import assert from 'node:assert';
import { Router } from '../src/router';

const env = {} as any;
const ctx = {} as any;

test('router matches static route', async () => {
  const router = new Router();
  router.get('/api/health', async () => new Response('ok'));
  const res = await router.serve('GET', '/api/health', new Request('http://x/'), env, ctx);
  assert.ok(res);
  assert.equal(await res.text(), 'ok');
});

test('router matches param route', async () => {
  const router = new Router();
  router.get('/api/posts/:slug', async (_req, _env, _ctx, params) => {
    return new Response(params.slug);
  });
  const res = await router.serve('GET', '/api/posts/hello-world', new Request('http://x/'), env, ctx);
  assert.ok(res);
  assert.equal(await res.text(), 'hello-world');
});

test('router returns null on no match', async () => {
  const router = new Router();
  router.get('/api/health', async () => new Response('ok'));
  const res = await router.serve('GET', '/api/nope', new Request('http://x/'), env, ctx);
  assert.equal(res, null);
});