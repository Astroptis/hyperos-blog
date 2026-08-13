import { execSync } from 'node:child_process';

const db = 'hyperos-blog';
const now = new Date().toISOString();
const sqls = [
  `INSERT INTO categories (name_zh, name_en, sort) VALUES ('技术', 'Tech', 1), ('生活', 'Life', 2), ('随笔', 'Notes', 3) ON CONFLICT(name_zh) DO NOTHING;`,
  `INSERT INTO settings (key, value) VALUES ('title', 'HyperOS 博客'), ('description', '一个用 Compose Multiplatform + Miuix 构建的个人博客') ON CONFLICT(key) DO NOTHING;`,
  `INSERT INTO friends (name, url, description, sort) VALUES ('Miuix', 'https://compose-miuix-ui.github.io/miuix/', 'Miuix UI 组件库', 1) ON CONFLICT DO NOTHING;`,
  `INSERT INTO posts (slug, title_zh, title_en, summary, content, category, tags, status, pinned, featured, created_at, updated_at) VALUES ('welcome', '欢迎来到我的博客', 'Welcome', '第一篇博客，介绍这个站点。', '# 欢迎\n\n这是用 **Compose Multiplatform** 和 **Miuix** 构建的个人博客。\n\n- 支持 Markdown\n- 支持评论与点赞\n- 支持主题切换\n\n部署在 Cloudflare Pages + Workers。', '技术', '["博客","Kotlin"]', 'published', 1, 1, '${now}', '${now}') ON CONFLICT(slug) DO NOTHING;`
];
for (const sql of sqls) {
  const escaped = sql.replace(/"/g, '\\"');
  execSync(`npx wrangler d1 execute ${db} --remote --command="${escaped}"`, { stdio: 'inherit' });
}
console.log('Seed data inserted.');
