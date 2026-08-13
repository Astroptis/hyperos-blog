import { PublicPost } from './types';

const BOT_REGEX = /(googlebot|bingbot|baiduspider|yandex|duckduckbot|facebookexternalhit|twitterbot|slurp|semrushbot|ahrefsbot|petalbot|bytespider|applebot|yisouspider)/i;

export function isBot(userAgent: string): boolean {
  return BOT_REGEX.test(userAgent);
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function renderMarkdownSimple(md: string): string {
  const lines = md.split('\n');
  const out: string[] = [];
  for (const line of lines) {
    const heading = /^(#{1,6})\s+(.*)$/.exec(line);
    if (heading) {
      const level = heading[1].length;
      out.push(`<h${level}>${escapeHtml(heading[2])}</h${level}>`);
      continue;
    }
    if (/^\s*```/.test(line)) {
      out.push('<pre><code>');
      continue;
    }
    if (line.trim() === '') {
      out.push('');
      continue;
    }
    if (/^\s*[-*]\s+/.test(line)) {
      out.push(`<ul><li>${escapeHtml(line.replace(/^\s*[-*]\s+/, ''))}</li></ul>`);
      continue;
    }
    if (/^\s*\d+\.\s+/.test(line)) {
      out.push(`<li>${escapeHtml(line.replace(/^\s*\d+\.\s+/, ''))}</li>`);
      continue;
    }
    out.push(`<p>${escapeHtml(line)}</p>`);
  }
  return out.join('\n');
}

export function renderPostHTML(post: PublicPost, siteTitle: string): string {
  const title = post.titleZh || post.titleEn || siteTitle;
  const summary = post.summary || `${post.content.slice(0, 150)}...`;
  const jsonLd = JSON.stringify({
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    headline: title,
    datePublished: post.createdAt,
    dateModified: post.updatedAt,
    description: summary,
    articleBody: post.content.slice(0, 2000),
  });
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${escapeHtml(title)} - ${escapeHtml(siteTitle)}</title>
  <meta name="description" content="${escapeHtml(summary)}">
  <meta property="og:type" content="article">
  <meta property="og:title" content="${escapeHtml(title)}">
  <meta property="og:description" content="${escapeHtml(summary)}">
  <meta property="og:site_name" content="${escapeHtml(siteTitle)}">
  <meta name="twitter:card" content="summary">
  <script type="application/ld+json">${jsonLd}</script>
</head>
<body>
  <h1>${escapeHtml(title)}</h1>
  <p class="meta">发布于 ${escapeHtml(post.createdAt)} · 阅读 ${post.viewCount}</p>
  ${renderMarkdownSimple(post.content)}
</body>
</html>`;
}

export function renderHomeHTML(siteTitle: string, posts: PublicPost[], settings: Record<string, string>): string {
  const items = posts.map((p) =>
    `<li><a href="/post/${escapeHtml(p.slug)}">${escapeHtml(p.titleZh || p.titleEn)}</a> <small>${escapeHtml(p.createdAt.slice(0, 10))}</small></li>`
  ).join('\n');
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${escapeHtml(siteTitle)}</title>
  <meta name="description" content="${escapeHtml(settings.description ?? '')}">
  <meta property="og:title" content="${escapeHtml(siteTitle)}">
  <meta property="og:description" content="${escapeHtml(settings.description ?? '')}">
</head>
<body>
  <h1>${escapeHtml(siteTitle)}</h1>
  <p>${escapeHtml(settings.description ?? '')}</p>
  <ul>${items}</ul>
</body>
</html>`;
}