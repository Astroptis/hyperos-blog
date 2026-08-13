import { test } from 'node:test';
import assert from 'node:assert';
import { toPublicPost } from '../src/mappers';
import { PostRow } from '../src/types';

const baseRow: PostRow = {
  id: 1,
  slug: 'hello',
  title_zh: '你好',
  title_en: 'Hello',
  summary: '摘要',
  content: '# 标题\n正文',
  category: '默认',
  tags: '["测试","blog"]',
  status: 'published',
  pinned: 1,
  featured: 0,
  view_count: 10,
  like_count: 3,
  created_at: '2026-08-01 10:00:00',
  updated_at: '2026-08-01 10:00:00',
};

test('toPublicPost maps snake_case to camelCase', () => {
  const p = toPublicPost(baseRow);
  assert.equal(p.slug, 'hello');
  assert.equal(p.titleZh, '你好');
  assert.deepEqual(p.tags, ['测试', 'blog']);
  assert.equal(p.pinned, true);
  assert.equal(p.featured, false);
  assert.equal(p.viewCount, 10);
});

test('toPublicPost handles invalid tags JSON', () => {
  const p = toPublicPost({ ...baseRow, tags: 'not-json' });
  assert.deepEqual(p.tags, []);
});