export interface Env {
  DB: D1Database;
  KV: KVNamespace;
}

export interface PostRow {
  id: number;
  slug: string;
  title_zh: string;
  title_en: string;
  summary: string;
  content: string;
  category: string;
  tags: string;
  status: 'published' | 'draft';
  pinned: number;
  featured: number;
  view_count: number;
  like_count: number;
  created_at: string;
  updated_at: string;
}

export interface PublicPost {
  id: number;
  slug: string;
  titleZh: string;
  titleEn: string;
  summary: string;
  content: string;
  category: string;
  tags: string[];
  pinned: boolean;
  featured: boolean;
  viewCount: number;
  likeCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CommentRow {
  id: number;
  post_id: number;
  nickname: string;
  email: string;
  content: string;
  created_at: string;
}

export interface MessageRow {
  id: number;
  nickname: string;
  email: string;
  content: string;
  created_at: string;
}

export interface FriendRow {
  id: number;
  name: string;
  url: string;
  avatar: string;
  description: string;
  sort: number;
}

export interface SiteStats {
  postCount: number;
  commentCount: number;
  messageCount: number;
  viewCount: number;
  totalVisits: number;
}