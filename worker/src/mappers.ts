import { PostRow, PublicPost } from './types';

export function toPublicPost(row: PostRow): PublicPost {
  let tags: string[] = [];
  try {
    tags = JSON.parse(row.tags);
  } catch {
    tags = [];
  }
  return {
    id: row.id,
    slug: row.slug,
    titleZh: row.title_zh,
    titleEn: row.title_en,
    summary: row.summary,
    content: row.content,
    category: row.category,
    tags,
    pinned: row.pinned === 1,
    featured: row.featured === 1,
    viewCount: row.view_count,
    likeCount: row.like_count,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}