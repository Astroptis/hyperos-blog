# HyperOS 风格个人博客 — 设计文档

日期：2026-08-12

## 1. 目标

构建一个基于 Compose Multiplatform (wasmJs) + Miuix UI 的个人博客，部署到 Cloudflare，
使用 D1 数据库和 KV 存储（不使用 R2），提供完整的博客功能与在线管理后台。

## 2. 总体架构

```
┌─────────────────────────────────────────────┐
│  Cloudflare Pages：Compose wasmJs 前端       │
│  (Miuix UI，HyperOS 蓝白风格)               │
├─────────────────────────────────────────────┤
│  Cloudflare Worker：/api/* REST + SEO 预渲染  │
│  ├── D1：文章/评论/留言/友链/设置/后台账号      │
│  └── KV：会话token/访客计数/热门排行缓存/防重  │
└─────────────────────────────────────────────┘
```

- 前端构建产物（HTML + wasm + js）→ Pages 静态托管
- Worker 处理 `/api/*`；对爬虫 UA 返回预渲染 HTML（含 OG 标签 + JSON-LD），对浏览器返回 SPA 入口
- 同域部署（自定义域名由用户自行配置，路由到 Pages）

## 3. 技术选型

| 层 | 技术 | 说明 |
|---|---|---|
| 前端 | Kotlin + Compose Multiplatform (wasmJs) | 编译为 WebAssembly |
| UI | Miuix (`top.yukonga.miuix.kmp:miuix-ui`) | HyperOS 设计风格 |
| 额外依赖 | miuix-icons、miuix-preference、miuix-navigation3-ui | 图标/设置项/导航 |
| HTTP | ktor-client（wasmJs 目标） | 调用后端 API |
| JSON | kotlinx.serialization | 数据序列化 |
| Markdown | 前端渲染或后端转 HTML | 文章渲染 |
| 后端 | Cloudflare Worker (TypeScript) | REST API + SEO 预渲染 |
| 数据库 | Cloudflare D1 (SQLite) | 结构化数据 |
| 缓存/会话 | Cloudflare KV | 会话、统计、防重 |
| 部署 | Cloudflare Pages + Workers | 静态 + API |

## 4. D1 数据模型

```sql
-- 文章
CREATE TABLE posts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  slug TEXT UNIQUE NOT NULL,
  title_zh TEXT NOT NULL,
  title_en TEXT,
  summary TEXT,
  content TEXT NOT NULL,           -- Markdown
  category TEXT,
  tags TEXT DEFAULT '[]',          -- JSON 数组
  status TEXT DEFAULT 'published', -- published / draft
  pinned INTEGER DEFAULT 0,
  featured INTEGER DEFAULT 0,
  view_count INTEGER DEFAULT 0,
  like_count INTEGER DEFAULT 0,
  created_at TEXT,
  updated_at TEXT
);

-- 分类
CREATE TABLE categories (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT UNIQUE NOT NULL,
  sort INTEGER DEFAULT 0
);

-- 评论
CREATE TABLE comments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  post_id INTEGER NOT NULL,
  nickname TEXT NOT NULL,
  email TEXT,
  content TEXT NOT NULL,
  created_at TEXT
);

-- 留言板
CREATE TABLE messages (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nickname TEXT NOT NULL,
  email TEXT,
  content TEXT NOT NULL,
  created_at TEXT
);

-- 友链
CREATE TABLE friends (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  url TEXT NOT NULL,
  avatar TEXT,
  description TEXT,
  sort INTEGER DEFAULT 0
);

-- 站点设置（键值）
CREATE TABLE settings (
  key TEXT PRIMARY KEY,
  value TEXT
);

-- 后台账号（密码哈希）
CREATE TABLE admin (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL
);
```

## 5. KV 用途

| Key 模式 | 用途 | 说明 |
|---|---|---|
| `session:{token}` | 管理员会话 | 登录后写入，含过期时间 |
| `visit:{yyyy-mm-dd}` | 按天访客计数 | 每日 PV，可聚合为访客趋势 |
| `like:{postId}` | 点赞防重 | 存 IP 集合（哈希值） |
| `view:{postId}:{ipHash}` | 浏览量防重 | 防止刷新刷量 |
| `hot:{lang}` | 热门文章排行缓存 | 定期计算缓存 |

## 6. API 端点

### 公开接口
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/posts` | 文章列表（分页、分类、标签、搜索过滤） |
| GET | `/api/posts/{slug}` | 文章详情（含 markdown） |
| GET | `/api/posts/{id}/comments` | 文章评论列表 |
| POST | `/api/posts/{id}/comments` | 发表评论 |
| POST | `/api/posts/{id}/like` | 点赞 |
| POST | `/api/posts/{id}/view` | 浏览量 +1 |
| GET | `/api/categories` | 分类列表 |
| GET | `/api/tags` | 标签聚合（含文章数） |
| GET | `/api/archives` | 归档（按年月分组） |
| GET | `/api/messages` | 留言列表 |
| POST | `/api/messages` | 发表留言 |
| GET | `/api/friends` | 友链列表 |
| GET | `/api/stats` | 站点统计（文章数/评论数/访客数） |
| GET | `/api/settings` | 站点公开设置 |

### 需登录（会话 token 校验）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录（返回 token） |
| POST | `/api/auth/logout` | 退出 |
| GET | `/api/auth/me` | 校验会话 |
| POST | `/api/posts` | 新建文章 |
| PUT | `/api/posts/{id}` | 更新文章 |
| DELETE | `/api/posts/{id}` | 删除文章 |
| PUT | `/api/settings` | 更新站点设置 |
| DELETE | `/api/messages/{id}` | 删除留言 |
| POST | `/api/friends` | 添加友链 |
| PUT/DELETE | `/api/friends/{id}` | 更新/删除友链 |
| DELETE | `/api/comments/{id}` | 删除评论 |

## 7. 前端页面与 Miuix 组件覆盖

| 页面 | 主要 Miuix 组件 |
|---|---|
| 首页 | TopAppBar、SearchBar、Card、NavigationBar/Rail、PullToRefresh、Badge |
| 文章列表 | TabRow(分类)、Divider、Card、PullToRefresh |
| 文章详情 | Card、OverlayDialog(目录)、点赞按钮、评论列表、Text |
| 搜索页 | TextField、搜索结果列表 |
| 归档页 | 时间线、NumberPicker(年份)、月份分组卡片 |
| 留言板 | TextField、Card、OverlayDialog 添加留言 |
| 友链页 | Card 网格、OverlayDialog 申请友链 |
| 设置页 | SwitchPreference(深色/语言)、ColorPicker(主题色)、SliderPreference(字号)、RadioButtonPreference(语言)、OverlayListPopup |
| 后台-登录 | TextField、Button、ProgressIndicator |
| 后台-文章管理 | Card 列表、SwitchPreference(发布/草稿)、OverlayDialog(编辑器)、Tooltip、IconButton |
| 后台-统计 | ProgressIndicator、Card 仪表盘 |
| 关于页 | 头像卡片、IconButton(社交)、Tooltip、Divider |

## 8. 主题系统

- `MiuixTheme` 蓝白配色（HyperOS 风格）
- 深色/浅色模式：跟随系统 / 手动切换（Switch）
- 动态强调色：ColorPicker 选择主题色
- 中英双语切换（i18n）：语言存储于 KV/localStorage，默认跟随浏览器

## 9. 安全

- 密码使用 `PBKDF2/argon2` 哈希（Worker 环境采用 `crypto.subtle` PBKDF2）
- 登录 token 为随机 256bit，存 KV 带 TTL
- 评论/留言内容转义防止 XSS（前端渲染时转义，后端 JSON 序列化）
- 所有写接口校验 Content-Type 与参数长度限制
- KV 中不直接存 IP，存哈希值

## 10. SEO 预渲染

- Worker 检测 User-Agent 是否为搜索引擎爬虫（Googlebot、Bingbot、百度、Yandex 等）
- 对文章详情页返回完整预渲染 HTML（标题、摘要、OG 标签、JSON-LD 文章结构化数据）
- 对首页/归档等返回站点元信息 HTML
- 普通浏览器请求返回 SPA 入口页

## 11. 错误处理

- 所有 API 返回统一 JSON 结构：`{ ok, data?, error? }`
- 前端统一错误提示（Snackbar）
- D1 查询异常返回 500，参数校验失败返回 400，未授权返回 401

## 12. 测试

- Worker API：Vitest + wrangler dev（本地 D1/KV 模拟）
- 前端：Compose wasmJs 编译 + 本地 Pages 预览
- 部署验证：wrangler pages deploy / wrangler deploy

## 13. 部署流程

1. `wrangler d1 create` 创建数据库
2. 运行 schema.sql 初始化 D1
3. 创建 KV namespace
4. 配置 `wrangler.jsonc`（D1 绑定、KV 绑定）
5. `./gradlew :composeApp:wasmJsBrowserDistribution` 构建前端
6. `wrangler pages deploy` 部署前端
7. `wrangler deploy` 部署 Worker
8. 初始化后台管理员账号（脚本）
9. 用户自行配置自定义域名
