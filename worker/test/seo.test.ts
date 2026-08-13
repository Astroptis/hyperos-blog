import { test } from 'node:test';
import assert from 'node:assert';
import { isBot, renderPostHTML, renderHomeHTML } from '../src/seo';

test('isBot detects search engine crawlers', () => {
  assert.equal(isBot('Mozilla/5.0 Googlebot/2.1'), true);
  assert.equal(isBot('Mozilla/5.0 Bingbot'), true);
  assert.equal(isBot('Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0'), false);
});

test('renderPostHTML contains OG tags and JSON-LD', () => {
  const html = renderPostHTML({
    id: 1, slug: 'hello', titleZh: '你好', titleEn: 'Hello', summary: '摘要',
    content: '# 标题\n\n正文内容', category: '技术', tags: ['kotlin'],
    pinned: false, featured: false, viewCount: 5, likeCount: 2,
    createdAt: '2026-01-01T00:00:00.000Z', updatedAt: '2026-01-01T00:00:00.000Z',
  } as any, '我的博客');
  assert.ok(html.includes('og:title'));
  assert.ok(html.includes('BlogPosting'));
  assert.ok(html.includes('<h1>你好</h1>'));
});

test('renderHomeHTML lists posts', () => {
  const html = renderHomeHTML('我的博客', [{
    id: 1, slug: 'hello', titleZh: '你好', titleEn: '', summary: '', content: '',
    category: '', tags: [], pinned: false, featured: false, viewCount: 0, likeCount: 0,
    createdAt: '2026-01-01T00:00:00.000Z', updatedAt: '2026-01-01T00:00:00.000Z',
  } as any], { description: '技术博客' });
  assert.ok(html.includes('/post/hello'));
});