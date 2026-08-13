CREATE TABLE IF NOT EXISTS posts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  slug TEXT UNIQUE NOT NULL,
  title_zh TEXT NOT NULL,
  title_en TEXT DEFAULT '',
  summary TEXT DEFAULT '',
  content TEXT NOT NULL,
  category TEXT DEFAULT '',
  tags TEXT DEFAULT '[]',
  status TEXT DEFAULT 'published',
  pinned INTEGER DEFAULT 0,
  featured INTEGER DEFAULT 0,
  view_count INTEGER DEFAULT 0,
  like_count INTEGER DEFAULT 0,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE INDEX idx_posts_slug ON posts(slug);
CREATE INDEX idx_posts_status_created ON posts(status, created_at DESC);

CREATE TABLE IF NOT EXISTS categories (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name_zh TEXT UNIQUE NOT NULL,
  name_en TEXT DEFAULT '',
  sort INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS comments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  post_id INTEGER NOT NULL,
  nickname TEXT NOT NULL,
  email TEXT DEFAULT '',
  content TEXT NOT NULL,
  created_at TEXT NOT NULL,
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_post ON comments(post_id, created_at DESC);

CREATE TABLE IF NOT EXISTS messages (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nickname TEXT NOT NULL,
  email TEXT DEFAULT '',
  content TEXT NOT NULL,
  created_at TEXT NOT NULL
);

CREATE INDEX idx_messages_created ON messages(created_at DESC);

CREATE TABLE IF NOT EXISTS friends (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  url TEXT NOT NULL,
  avatar TEXT DEFAULT '',
  description TEXT DEFAULT '',
  sort INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS admin (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  created_at TEXT NOT NULL
);