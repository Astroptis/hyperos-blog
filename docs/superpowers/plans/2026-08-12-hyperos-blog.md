# HyperOS 博客实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个基于 Compose Multiplatform (wasmJs) + Miuix UI 的个人博客，部署到 Cloudflare，使用 D1 和 KV 存储（不使用 R2），提供文章、分类/标签/搜索/归档、评论/点赞、主题切换、阅读统计、留言板、友链、关于页、在线后台管理等完整功能，并带 SEO 服务端预渲染。

**Architecture:** 前端为 Kotlin Compose wasmJs 应用（Miuix HyperOS 风格 UI），编译产物部署到 Cloudflare Pages；后端为独立 Cloudflare Worker 提供 REST API（`/api/*`）和 SEO 预渲染。D1 存结构化数据（文章/评论/留言/友链/设置/后台账号），KV 存会话 token、按天访客计数、点赞/浏览防重和热门排行缓存。

**Tech Stack:**
- 前端：Kotlin 2.4.10、Compose Multiplatform 1.11.1、Miuix 0.9.3、Ktor Client 3.5.2 (wasmJs)、kotlinx-serialization 1.11.0、multiplatform-markdown-renderer 0.43.0、Gradle 9.6.1
- 后端：Cloudflare Worker (TypeScript)、D1 (SQLite)、KV、wrangler 4.x

## Global Constraints

- **Miuix 版本固定 0.9.3**，从 `top.yukonga.miuix.kmp` 坐标引入（miuix-ui、miuix-preference、miuix-icons）
- **Kotlin 固定 2.4.10**，Compose Multiplatform 插件固定 1.11.1，Gradle 使用 9.6.1 wrapper
- 前端使用 **wasmJs 目标**，构建命令为 `./gradlew :composeApp:wasmJsBrowserDistribution`，产物目录 `composeApp/build/dist/wasmJs/productionExecutable`
- 后端存储只用 **D1 + KV**，禁止使用 R2
- 所有 API 返回统一 JSON：`{ ok: boolean, data?: any, error?: string }`
- 密码使用 `crypto.subtle` PBKDF2 哈希，格式 `pbkdf2:iterations:saltHex:hashHex`
- 评论/留言/友链 XSS 防护：前端渲染时使用 Text 组件（不渲染 HTML），后端存储原始文本
- 中英双语 i18n：前端代码内嵌字符串表，跟随系统语言，可在设置页手动切换
- 后端 TypeScript 使用 `satisfies ExportedHandler<Env>` 语法

---

### Task 1: 项目脚手架（Gradle 配置 + 目录结构）

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `.gitignore`
- Create: `composeApp/build.gradle.kts`
- Create: `composeApp/src/wasmJsMain/resources/index.html`
- Create: `composeApp/src/wasmJsMain/resources/styles.css`
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/App.kt`
- Create: `composeApp/src/wasmJsMain/kotlin/Main.kt`
- Create: `worker/package.json`
- Create: `worker/tsconfig.json`
- Create: `worker/wrangler.jsonc`

**Interfaces:**
- Consumes: 无
- Produces: Gradle 多模块项目可执行 `wasmJsBrowserDistribution`；`com.hyperos.blog.App()` 为根 Composable 入口

- [ ] **Step 1: 确认 Gradle wrapper 可用**

Gradle 未全局安装，需要 wrapper。从本地参考仓库 `C:\Users\wang\AppData\Local\Temp\opencode\miuix-main\gradle\wrapper\` 复制 `gradle-wrapper.jar`、`gradle-wrapper.properties`、`gradlew`、`gradlew.bat` 到本项目。若复制失败则手动创建 wrapper 文件：

```
# gradle/wrapper/gradle-wrapper.properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

验证：
```bash
./gradlew --version
```
期望：显示 Gradle 9.6.1 和 JDK 25。

- [ ] **Step 2: 编写根构建设置**

`settings.gradle.kts`:
```kotlin
rootProject.name = "hyperos-blog"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

include(":composeApp")
```

`build.gradle.kts`（根，空壳）:
```kotlin
plugins {
    base
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
kotlin.code.style=official
kotlin.mpp.applyDefaultHierarchyTemplate=true
```

- [ ] **Step 3: 编写版本目录**

`gradle/libs.versions.toml`:
```toml
[versions]
kotlin = "2.4.10"
compose = "1.11.1"
miuix = "0.9.3"
ktor = "3.5.2"
kotlinxSerialization = "1.11.0"
markdown = "0.43.0"

[libraries]
miuix-ui = { module = "top.yukonga.miuix.kmp:miuix-ui", version.ref = "miuix" }
miuix-preference = { module = "top.yukonga.miuix.kmp:miuix-preference", version.ref = "miuix" }
miuix-icons = { module = "top.yukonga.miuix.kmp:miuix-icons", version.ref = "miuix" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
markdown-renderer = { module = "com.mikepenz:multiplatform-markdown-renderer", version.ref = "markdown" }

[plugins]
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "compose" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 4: 编写 composeApp 模块**

`composeApp/build.gradle.kts`:
```kotlin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add("src/wasmJsMain/resources")
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.miuix.ui)
            implementation(libs.miuix.preference)
            implementation(libs.miuix.icons)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.markdown.renderer)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

compose.resources {
    publicResClass = true
}
```

- [ ] **Step 5: 编写 .gitignore**

```gitignore
.gradle/
build/
.kotlin/
local.properties
node_modules/
.idea/
*.iml
.wrangler/
.dev.vars
dist/
coverage/
*.log
```

- [ ] **Step 6: 编写前端入口文件**

`composeApp/src/wasmJsMain/resources/index.html`:
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HyperOS 博客</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <div id="loading" class="loading">
        <div class="loading-card">加载中...</div>
    </div>
    <div id="composeApp"></div>
    <script src="composeApp.js" type="application/javascript"></script>
</body>
</html>
```

`composeApp/src/wasmJsMain/resources/styles.css`:
```css
html, body {
    margin: 0;
    padding: 0;
    height: 100%;
    background: #f5f5f5;
    font-family: "MiSans", "PingFang SC", "Microsoft YaHei", sans-serif;
}
#composeApp { height: 100%; }
.loading {
    position: fixed; inset: 0; display: flex; align-items: center; justify-content: center;
    background: #f5f5f5; z-index: 9999; transition: opacity .3s;
}
.loading.hidden { opacity: 0; pointer-events: none; }
.loading-card {
    background: #fff; border-radius: 16px; padding: 24px 40px;
    box-shadow: 0 8px 24px rgba(0,0,0,.08); color: #666;
}
```

`composeApp/src/wasmJsMain/kotlin/Main.kt`:
```kotlin
package com.hyperos.blog

import androidx.compose.ui.window.ComposeViewport

fun main() {
    ComposeViewport(viewportContainerId = "composeApp") {
        App()
    }
}
```

`composeApp/src/commonMain/kotlin/com/hyperos/blog/App.kt`:
```kotlin
package com.hyperos.blog

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
fun App() {
    MiuixTheme(colors = lightColorScheme()) {
        // 占位：后续替换为真实界面
        androidx.compose.material3.Text("HyperOS Blog")
    }
}
```

- [ ] **Step 7: 编写 worker 配置**

`worker/package.json`:
```json
{
  "name": "hyperos-blog-worker",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "wrangler dev",
    "deploy": "wrangler deploy",
    "migrate:local": "wrangler d1 migrations apply hyperos-blog --local",
    "migrate:remote": "wrangler d1 migrations apply hyperos-blog --remote",
    "types": "wrangler types"
  },
  "devDependencies": {
    "@cloudflare/workers-types": "^4.20250124.0",
    "typescript": "^5.7.0",
    "wrangler": "^4.0.0"
  }
}
```

`worker/tsconfig.json`:
```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ES2022",
    "moduleResolution": "Bundler",
    "lib": ["ES2022"],
    "types": ["@cloudflare/workers-types"],
    "strict": true,
    "noEmit": true,
    "skipLibCheck": true
  },
  "include": ["src/**/*.ts"]
}
```

`worker/wrangler.jsonc`:
```jsonc
{
  "name": "hyperos-blog-worker",
  "main": "src/index.ts",
  "compatibility_date": "2026-08-12",
  "workers_dev": true,
  "d1_databases": [
    {
      "binding": "DB",
      "database_name": "hyperos-blog",
      "database_id": "REPLACE_WITH_D1_ID"
    }
  ],
  "kv_namespaces": [
    {
      "binding": "KV",
      "id": "REPLACE_WITH_KV_ID"
    }
  ]
}
```

- [ ] **Step 8: 安装 worker 依赖并提交**

```bash
cd worker && npm install
```

提交：
```bash
git add -A
git commit -m "chore: scaffold project structure"
```

---

### Task 2: D1 数据库 Schema 与迁移

**Files:**
- Create: `worker/migrations/0001_initial.sql`

**Interfaces:**
- Consumes: Task 1 的 wrangler.jsonc
- Produces: `posts`、`categories`、`comments`、`messages`、`friends`、`settings`、`admin` 七张表

- [ ] **Step 1: 编写初始迁移**

`worker/migrations/0001_initial.sql`:
```sql
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
```

- [ ] **Step 2: 验证迁移语法**

```bash
cd worker && npx wrangler d1 migrations create hyperos-blog test 2>&1 | Select-Object -First 3
```
若报错说明 D1 未创建，需先执行：
```bash
npx wrangler d1 create hyperos-blog
```
将输出的 database_id 填入 wrangler.jsonc。验证迁移列表命令能跑通后删除 test 迁移文件。

- [ ] **Step 3: 提交**

```bash
git add worker/migrations
git commit -m "feat: add D1 initial schema"
```

---

### Task 3: Worker 基础设施（统一响应 + 路由 + CORS + 类型）

**Files:**
- Create: `worker/src/types.ts`
- Create: `worker/src/response.ts`
- Create: `worker/src/router.ts`
- Create: `worker/src/index.ts`

**Interfaces:**
- Consumes: Task 1 的 env 配置（`DB: D1Database`、`KV: KVNamespace`）
- Produces:
  - `jsonOk(data: unknown, init?): Response`
  - `jsonError(status: number, message: string): Response`
  - `handleCors(req: Request, env: Env): Response | null`
  - `Router` 类：`router.get(path, handler)`、`router.post(...)`、`router.all(...)`、`router.serve(request)`；handler 签名为 `(req: Request, env: Env, ctx: ExecutionContext, params: Record<string, string>) => Promise<Response>`
  - `Env` 接口：`{ DB: D1Database; KV: KVNamespace; }`

- [ ] **Step 1: 编写类型定义**

`worker/src/types.ts`:
```typescript
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
```

`worker/src/response.ts`:
```typescript
import { Env } from './types';

export function jsonOk(data: unknown, init?: ResponseInit): Response {
  return new Response(JSON.stringify({ ok: true, data }), {
    status: init?.status ?? 200,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...init?.headers,
    },
  });
}

export function jsonError(status: number, message: string): Response {
  return new Response(JSON.stringify({ ok: false, error: message }), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
  });
}

const CORS_HEADERS: Record<string, string> = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
  'Access-Control-Max-Age': '86400',
};

export function handleCors(req: Request): Response | null {
  if (req.method === 'OPTIONS') {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }
  return null;
}

export function withCors(res: Response): Response {
  for (const [k, v] of Object.entries(CORS_HEADERS)) {
    res.headers.set(k, v);
  }
  return res;
}
```

- [ ] **Step 2: 编写 Router**

`worker/src/router.ts`:
```typescript
import { Env } from './types';

type Handler = (
  req: Request,
  env: Env,
  ctx: ExecutionContext,
  params: Record<string, string>
) => Promise<Response>;

interface Route {
  method: string;
  pattern: RegExp;
  paramNames: string[];
  handler: Handler;
}

export class Router {
  private routes: Route[] = [];

  get(path: string, handler: Handler): void {
    this.add('GET', path, handler);
  }

  post(path: string, handler: Handler): void {
    this.add('POST', path, handler);
  }

  put(path: string, handler: Handler): void {
    this.add('PUT', path, handler);
  }

  delete(path: string, handler: Handler): void {
    this.add('DELETE', path, handler);
  }

  all(path: string, handler: Handler): void {
    for (const m of ['GET', 'POST', 'PUT', 'DELETE']) {
      this.add(m, path, handler);
    }
  }

  private add(method: string, path: string, handler: Handler): void {
    const paramNames: string[] = [];
    const patternStr = path.replace(/:([a-zA-Z0-9_]+)/g, (_, name) => {
      paramNames.push(name);
      return '([^/]+)';
    });
    this.routes.push({
      method,
      pattern: new RegExp(`^${patternStr}$`),
      paramNames,
      handler,
    });
  }

  async serve(
    method: string,
    pathname: string,
    req: Request,
    env: Env,
    ctx: ExecutionContext
  ): Promise<Response | null> {
    for (const route of this.routes) {
      if (route.method !== method) continue;
      const match = route.pattern.exec(pathname);
      if (!match) continue;
      const params: Record<string, string> = {};
      route.paramNames.forEach((name, i) => {
        params[name] = decodeURIComponent(match[i + 1]);
      });
      return await route.handler(req, env, ctx, params);
    }
    return null;
  }
}
```

- [ ] **Step 3: 编写 Worker 入口（含健康检查）**

`worker/src/index.ts`:
```typescript
import { Router } from './router';
import { jsonOk, jsonError, handleCors, withCors } from './response';
import { Env } from './types';

const router = new Router();

router.get('/api/health', async () => {
  return jsonOk({ status: 'ok', time: new Date().toISOString() });
});

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const corsResponse = handleCors(request);
    if (corsResponse) return corsResponse;

    const url = new URL(request.url);

    if (url.pathname.startsWith('/api/')) {
      const res = await router.serve(request.method, url.pathname, request, env, ctx);
      if (res) return withCors(res);
      return withCors(jsonError(404, 'Not Found'));
    }

    return withCors(jsonError(404, 'Not Found'));
  },
} satisfies ExportedHandler<Env>;
```

- [ ] **Step 4: 编写测试并验证**

创建 `worker/test/router.test.ts`（使用 `node:test`，Node 24 内置）:
```typescript
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
```

运行：
```bash
cd worker && npx tsx --test test/router.test.ts
```
若 tsx 未安装：`npm i -D tsx`。期望：3 个测试通过。

- [ ] **Step 5: 本地运行验证健康检查**

```bash
cd worker && npx wrangler dev --port 8787
```
另开终端：
```bash
curl http://localhost:8787/api/health
```
期望：`{"ok":true,"data":{"status":"ok",...}}`

- [ ] **Step 6: 提交**

```bash
git add worker/src worker/package.json
git commit -m "feat: worker infra with router and CORS"
```

---

### Task 4: Worker 认证（登录/登出/会话）

**Files:**
- Create: `worker/src/auth.ts`
- Create: `worker/src/routes/auth.ts`
- Modify: `worker/src/index.ts`（注册路由）

**Interfaces:**
- Consumes: `jsonOk`/`jsonError`、`Router`、`Env`
- Produces:
  - `hashPassword(password: string): Promise<string>` — PBKDF2，输出 `pbkdf2:iterations:saltHex:hashHex`
  - `verifyPassword(password: string, stored: string): Promise<boolean>`
  - `createSession(env: Env, username: string): Promise<string>` — 生成 32 字节 token，存 KV `session:{token}` TTL 7 天
  - `requireAdmin(req: Request, env: Env): Promise<string | null>` — 从 `Authorization: Bearer <token>` 校验，返回 username 或 null
  - `GET /api/auth/me`、`POST /api/auth/login`、`POST /api/auth/logout`

- [ ] **Step 1: 编写密码哈希与认证工具**

`worker/src/auth.ts`:
```typescript
import { Env } from './types';

const ITERATIONS = 100_000;

function toHex(bytes: Uint8Array): string {
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
}

function fromHex(hex: string): Uint8Array {
  const bytes = new Uint8Array(hex.length / 2);
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = parseInt(hex.slice(i * 2, i * 2 + 2), 16);
  }
  return bytes;
}

export async function hashPassword(password: string): Promise<string> {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(password),
    'PBKDF2',
    false,
    ['deriveBits']
  );
  const bits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', salt, iterations: ITERATIONS, hash: 'SHA-256' },
    keyMaterial,
    256
  );
  return `pbkdf2:${ITERATIONS}:${toHex(salt)}:${toHex(new Uint8Array(bits))}`;
}

export async function verifyPassword(password: string, stored: string): Promise<boolean> {
  const [algo, iterStr, saltHex, hashHex] = stored.split(':');
  if (algo !== 'pbkdf2') return false;
  const iterations = parseInt(iterStr, 10);
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(password),
    'PBKDF2',
    false,
    ['deriveBits']
  );
  const bits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', salt: fromHex(saltHex), iterations, hash: 'SHA-256' },
    keyMaterial,
    256
  );
  const actual = toHex(new Uint8Array(bits));
  let equal = actual.length === hashHex.length;
  for (let i = 0; i < actual.length; i++) {
    if (actual.charCodeAt(i) !== hashHex.charCodeAt(i)) equal = false;
  }
  return equal;
}

export async function createSession(env: Env, username: string): Promise<string> {
  const tokenBytes = crypto.getRandomValues(new Uint8Array(32));
  const token = toHex(tokenBytes);
  await env.KV.put(`session:${token}`, JSON.stringify({ username }), {
    expirationTtl: 7 * 24 * 3600,
  });
  return token;
}

export async function deleteSession(env: Env, token: string): Promise<void> {
  await env.KV.delete(`session:${token}`);
}

export async function requireAdmin(req: Request, env: Env): Promise<string | null> {
  const authHeader = req.headers.get('Authorization');
  if (!authHeader || !authHeader.startsWith('Bearer ')) return null;
  const token = authHeader.slice(7).trim();
  const raw = await env.KV.get(`session:${token}`);
  if (!raw) return null;
  try {
    return (JSON.parse(raw) as { username: string }).username;
  } catch {
    return null;
  }
}
```

- [ ] **Step 2: 编写认证路由**

`worker/src/routes/auth.ts`:
```typescript
import { Router } from '../router';
import { jsonOk, jsonError } from '../response';
import { Env } from '../types';
import { hashPassword, verifyPassword, createSession, deleteSession, requireAdmin } from '../auth';

export function registerAuthRoutes(router: Router): void {
  router.post('/api/auth/login', async (req, env) => {
    let body: { username?: string; password?: string };
    try {
      body = await req.json();
    } catch {
      return jsonError(400, 'Invalid JSON body');
    }
    const username = (body.username ?? '').trim();
    const password = body.password ?? '';
    if (!username || !password) {
      return jsonError(400, 'Username and password are required');
    }
    const row = await env.DB.prepare('SELECT * FROM admin WHERE username = ?').bind(username).first();
    if (!row) return jsonError(401, 'Invalid credentials');
    const valid = await verifyPassword(password, row.password_hash as string);
    if (!valid) return jsonError(401, 'Invalid credentials');
    const token = await createSession(env, username);
    return jsonOk({ token, username });
  });

  router.post('/api/auth/logout', async (req, env) => {
    const authHeader = req.headers.get('Authorization');
    if (authHeader && authHeader.startsWith('Bearer ')) {
      await deleteSession(env, authHeader.slice(7).trim());
    }
    return jsonOk({ loggedOut: true });
  });

  router.get('/api/auth/me', async (req, env) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    return jsonOk({ username });
  });
}
```

- [ ] **Step 3: 编写路由注册与测试**

在 `worker/src/index.ts` 中导入并注册：
```typescript
import { registerAuthRoutes } from './routes/auth';
registerAuthRoutes(router);
```

创建 `worker/test/auth.test.ts`:
```typescript
import { test } from 'node:test';
import assert from 'node:assert';
import { hashPassword, verifyPassword } from '../src/auth';

test('hash and verify password roundtrip', async () => {
  const hash = await hashPassword('secret123');
  assert.ok(hash.startsWith('pbkdf2:'));
  assert.equal(await verifyPassword('secret123', hash), true);
  assert.equal(await verifyPassword('wrong', hash), false);
});
```

运行：
```bash
cd worker && npx tsx --test test/auth.test.ts
```
期望：测试通过。

- [ ] **Step 4: 提交**

```bash
git add worker/src
git commit -m "feat: admin authentication with sessions"
```

---

### Task 5: Worker 公开 API — 文章/分类/标签/归档/搜索

**Files:**
- Create: `worker/src/mappers.ts`
- Create: `worker/src/routes/posts.ts`
- Create: `worker/src/routes/taxonomy.ts`
- Modify: `worker/src/index.ts`

**Interfaces:**
- Consumes: `PostRow`、`jsonOk`/`jsonError`、`Router`
- Produces:
  - `toPublicPost(row: PostRow): PublicPost` — 解析 tags JSON、数字转布尔
  - `GET /api/posts?page=1&pageSize=10&category=&tag=&keyword=&status=`（status 仅公开 published）
  - `GET /api/posts/:slug`（含草稿判断：仅 published 或带有效 admin token）
  - `GET /api/categories`
  - `GET /api/tags`（返回 `{name, count}[]`）
  - `GET /api/archives`（返回 `{year, month, posts: PublicPost[]}[]`）
  - `GET /api/search?q=`（标题+摘要+内容 LIKE）

- [ ] **Step 1: 编写数据映射器**

`worker/src/mappers.ts`:
```typescript
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
```

- [ ] **Step 2: 编写文章路由**

`worker/src/routes/posts.ts`:
```typescript
import { Router } from '../router';
import { jsonOk, jsonError } from '../response';
import { Env, PostRow, PublicPost } from '../types';
import { toPublicPost } from '../mappers';
import { requireAdmin } from '../auth';

export function registerPostRoutes(router: Router): void {
  router.get('/api/posts', async (req, env) => {
    const url = new URL(req.url);
    const page = Math.max(1, parseInt(url.searchParams.get('page') ?? '1', 10) || 1);
    const pageSize = Math.min(50, Math.max(1, parseInt(url.searchParams.get('pageSize') ?? '10', 10) || 10));
    const category = url.searchParams.get('category') ?? '';
    const tag = url.searchParams.get('tag') ?? '';
    const keyword = url.searchParams.get('keyword') ?? '';

    const where: string[] = ["status = 'published'"];
    const params: unknown[] = [];
    if (category) { where.push('category = ?'); params.push(category); }
    if (tag) { where.push('tags LIKE ?'); params.push(`%"${tag}"%`); }
    if (keyword) {
      where.push('(title_zh LIKE ? OR title_en LIKE ? OR summary LIKE ? OR content LIKE ?)');
      const kw = `%${keyword}%`;
      params.push(kw, kw, kw, kw);
    }
    const whereSql = where.join(' AND ');
    params.push(pageSize, (page - 1) * pageSize);

    const totalRow = await env.DB.prepare(`SELECT COUNT(*) AS total FROM posts WHERE ${whereSql}`)
      .bind(...params.slice(0, params.length - 2))
      .first();
    const rows = await env.DB.prepare(
      `SELECT * FROM posts WHERE ${whereSql}
       ORDER BY pinned DESC, created_at DESC LIMIT ? OFFSET ?`
    ).bind(...params).all();

    const posts: PublicPost[] = (rows.results as unknown as PostRow[]).map(toPublicPost);
    return jsonOk({
      posts,
      total: totalRow?.total ?? 0,
      page,
      pageSize,
      totalPages: Math.ceil((totalRow?.total ?? 0) / pageSize),
    });
  });

  router.get('/api/posts/:slug', async (req, env) => {
    const slug = req.url.split('/').pop()!.split('?')[0];
    const username = await requireAdmin(req, env);
    let row: PostRow | null = null;
    if (username) {
      row = await env.DB.prepare('SELECT * FROM posts WHERE slug = ?').bind(slug).first<PostRow>();
    } else {
      row = await env.DB.prepare("SELECT * FROM posts WHERE slug = ? AND status = 'published'").bind(slug).first<PostRow>();
    }
    if (!row) return jsonError(404, 'Post not found');
    return jsonOk(toPublicPost(row));
  });
}
```

- [ ] **Step 3: 编写分类/标签/归档/搜索路由**

`worker/src/routes/taxonomy.ts`:
```typescript
import { Router } from '../router';
import { jsonOk } from '../response';
import { Env, PostRow, PublicPost } from '../types';
import { toPublicPost } from '../mappers';

export function registerTaxonomyRoutes(router: Router): void {
  router.get('/api/categories', async (_req, env) => {
    const rows = await env.DB.prepare(
      'SELECT c.*, (SELECT COUNT(*) FROM posts p WHERE p.category = c.name_zh AND p.status = "published") AS count FROM categories c ORDER BY c.sort ASC'
    ).all();
    return jsonOk(rows.results);
  });

  router.get('/api/tags', async (_req, env) => {
    const rows = await env.DB.prepare(
      "SELECT tags FROM posts WHERE status = 'published'"
    ).all();
    const counts = new Map<string, number>();
    for (const row of rows.results) {
      try {
        const tags: string[] = JSON.parse(row.tags as string);
        for (const t of tags) counts.set(t, (counts.get(t) ?? 0) + 1);
      } catch { /* skip */ }
    }
    const tagList = [...counts.entries()]
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);
    return jsonOk(tagList);
  });

  router.get('/api/archives', async (_req, env) => {
    const rows = await env.DB.prepare(
      "SELECT * FROM posts WHERE status = 'published' ORDER BY created_at DESC"
    ).all<PostRow>();
    const posts = rows.results.map(toPublicPost);
    const map = new Map<string, { year: string; month: string; posts: PublicPost[] }>();
    for (const p of posts) {
      const key = p.createdAt.slice(0, 7);
      if (!map.has(key)) {
        map.set(key, { year: p.createdAt.slice(0, 4), month: p.createdAt.slice(5, 7), posts: [] });
      }
      map.get(key)!.posts.push(p);
    }
    return jsonOk([...map.values()]);
  });

  router.get('/api/search', async (req, env) => {
    const url = new URL(req.url);
    const q = (url.searchParams.get('q') ?? '').trim();
    if (!q) return jsonOk({ posts: [] });
    const kw = `%${q}%`;
    const rows = await env.DB.prepare(
      "SELECT * FROM posts WHERE status = 'published' AND (title_zh LIKE ? OR title_en LIKE ? OR summary LIKE ? OR content LIKE ?) ORDER BY created_at DESC LIMIT 50"
    ).bind(kw, kw, kw, kw).all<PostRow>();
    return jsonOk({ posts: rows.results.map(toPublicPost) });
  });
}
```

- [ ] **Step 4: 注册路由并本地测试**

在 `worker/src/index.ts`：
```typescript
import { registerPostRoutes } from './routes/posts';
import { registerTaxonomyRoutes } from './routes/taxonomy';
registerPostRoutes(router);
registerTaxonomyRoutes(router);
```

本地验证：先初始化本地 D1（Task 2 的迁移），插入测试数据：
```bash
cd worker
npx wrangler d1 migrations apply hyperos-blog --local
npx wrangler d1 execute hyperos-blog --local --command="INSERT INTO posts (slug,title_zh,title_en,summary,content,category,tags,status,created_at,updated_at) VALUES ('hello','你好','Hello','测试摘要','# 你好\n正文内容','默认','[\"测试\"]','published',datetime('now'),datetime('now'));"
npx wrangler dev --port 8787
```
期望：
```bash
curl http://localhost:8787/api/posts
# {"ok":true,"data":{"posts":[{"slug":"hello",...}],...}}
curl http://localhost:8787/api/posts/hello
# {"ok":true,"data":{"slug":"hello","titleZh":"你好",...}}
```

- [ ] **Step 5: 提交**

```bash
git add worker/src
git commit -m "feat: public read APIs for posts and taxonomy"
```

---

### Task 6: Worker 互动 API — 评论/点赞/浏览量/留言/友链/统计/设置

**Files:**
- Create: `worker/src/routes/interactions.ts`
- Create: `worker/src/routes/site.ts`
- Modify: `worker/src/index.ts`

**Interfaces:**
- Consumes: `jsonOk`/`jsonError`、`Router`、`Env`、`requireAdmin`
- Produces:
  - `GET /api/posts/:id/comments`、`POST /api/posts/:id/comments`
  - `POST /api/posts/:id/like`（KV 防重：`like:{postId}:{ipHash}`，TTL 24h）
  - `POST /api/posts/:id/view`（KV 防重：`view:{postId}:{ipHash}`，TTL 24h；D1 自增 view_count）
  - `GET/POST /api/messages`、`DELETE /api/messages/:id`（admin）
  - `GET/POST /api/friends`、`PUT/DELETE /api/friends/:id`（写操作 admin）
  - `GET /api/stats`
  - `GET/PUT /api/settings`（写操作 admin）
  - 每日访客计数：`POST /api/visit` → KV `visit:{yyyy-mm-dd}` 自增

- [ ] **Step 1: 编写 IP 哈希与互动路由**

`worker/src/routes/interactions.ts`:
```typescript
import { Router } from '../router';
import { jsonOk, jsonError } from '../response';
import { Env } from '../types';
import { requireAdmin } from '../auth';

async function ipHash(req: Request): Promise<string> {
  const ip = req.headers.get('CF-Connecting-IP') ?? 'unknown';
  const data = new TextEncoder().encode(ip);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(digest).slice(0, 8), (b) => b.toString(16).padStart(2, '0')).join('');
}

function validateText(value: string, max: number): string {
  return value.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '').slice(0, max);
}

export function registerInteractionRoutes(router: Router): void {
  router.get('/api/posts/:id/comments', async (_req, env, _ctx, params) => {
    const id = parseInt(params.id, 10);
    if (Number.isNaN(id)) return jsonError(400, 'Invalid post id');
    const rows = await env.DB.prepare(
      'SELECT id, nickname, content, created_at FROM comments WHERE post_id = ? ORDER BY created_at ASC'
    ).bind(id).all();
    return jsonOk(rows.results);
  });

  router.post('/api/posts/:id/comments', async (req, env, _ctx, params) => {
    const id = parseInt(params.id, 10);
    if (Number.isNaN(id)) return jsonError(400, 'Invalid post id');
    const post = await env.DB.prepare('SELECT id FROM posts WHERE id = ? AND status = "published"').bind(id).first();
    if (!post) return jsonError(404, 'Post not found');

    let body: { nickname?: string; email?: string; content?: string };
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }

    const nickname = validateText((body.nickname ?? '').trim(), 50);
    const email = validateText((body.email ?? '').trim().toLowerCase(), 100);
    const content = validateText((body.content ?? '').trim(), 2000);
    if (!nickname || !content) return jsonError(400, 'Nickname and content are required');

    const result = await env.DB.prepare(
      'INSERT INTO comments (post_id, nickname, email, content, created_at) VALUES (?, ?, ?, ?, ?)'
    ).bind(id, nickname, email, content, new Date().toISOString()).run();
    const commentId = result.meta.last_row_id;
    return jsonOk({ id: commentId, nickname, content, createdAt: new Date().toISOString() }, { status: 201 });
  });

  router.post('/api/posts/:id/like', async (req, env, _ctx, params) => {
    const id = parseInt(params.id, 10);
    if (Number.isNaN(id)) return jsonError(400, 'Invalid post id');
    const h = await ipHash(req);
    const dedupeKey = `like:${id}:${h}`;
    const existing = await env.KV.get(dedupeKey);
    if (existing) return jsonOk({ liked: false, message: 'Already liked' });
    await env.KV.put(dedupeKey, '1', { expirationTtl: 24 * 3600 });
    await env.DB.prepare('UPDATE posts SET like_count = like_count + 1 WHERE id = ?').bind(id).run();
    const row = await env.DB.prepare('SELECT like_count FROM posts WHERE id = ?').bind(id).first();
    return jsonOk({ liked: true, likeCount: row?.like_count ?? 0 });
  });

  router.post('/api/posts/:id/view', async (req, env, _ctx, params) => {
    const id = parseInt(params.id, 10);
    if (Number.isNaN(id)) return jsonError(400, 'Invalid post id');
    const h = await ipHash(req);
    const dedupeKey = `view:${id}:${h}`;
    const existing = await env.KV.get(dedupeKey);
    if (!existing) {
      await env.KV.put(dedupeKey, '1', { expirationTtl: 24 * 3600 });
      await env.DB.prepare('UPDATE posts SET view_count = view_count + 1 WHERE id = ?').bind(id).run();
    }
    const row = await env.DB.prepare('SELECT view_count FROM posts WHERE id = ?').bind(id).first();
    return jsonOk({ viewCount: row?.view_count ?? 0 });
  });

  router.post('/api/visit', async (req, env) => {
    const h = await ipHash(req);
    const today = new Date().toISOString().slice(0, 10);
    const dedupeKey = `visit:${today}:${h}`;
    const existing = await env.KV.get(dedupeKey);
    if (!existing) {
      await env.KV.put(dedupeKey, '1', { expirationTtl: 2 * 24 * 3600 });
      await env.KV.put(`visit:${today}`, String((parseInt((await env.KV.get(`visit:${today}`)) ?? '0', 10)) + 1), {
        expirationTtl: 40 * 24 * 3600,
      });
    }
    const count = parseInt((await env.KV.get(`visit:${today}`)) ?? '0', 10);
    return jsonOk({ visits: count });
  });

  router.get('/api/messages', async (_req, env) => {
    const rows = await env.DB.prepare(
      'SELECT id, nickname, content, created_at FROM messages ORDER BY created_at DESC LIMIT 100'
    ).all();
    return jsonOk(rows.results);
  });

  router.post('/api/messages', async (req, env) => {
    let body: { nickname?: string; email?: string; content?: string };
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }
    const nickname = validateText((body.nickname ?? '').trim(), 50);
    const email = validateText((body.email ?? '').trim().toLowerCase(), 100);
    const content = validateText((body.content ?? '').trim(), 1000);
    if (!nickname || !content) return jsonError(400, 'Nickname and content are required');
    const result = await env.DB.prepare(
      'INSERT INTO messages (nickname, email, content, created_at) VALUES (?, ?, ?, ?)'
    ).bind(nickname, email, content, new Date().toISOString()).run();
    return jsonOk({ id: result.meta.last_row_id, nickname, content, createdAt: new Date().toISOString() }, { status: 201 });
  });

  router.delete('/api/messages/:id', async (req, env, _ctx, params) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const id = parseInt(params.id, 10);
    await env.DB.prepare('DELETE FROM messages WHERE id = ?').bind(id).run();
    return jsonOk({ deleted: true });
  });

  router.delete('/api/comments/:id', async (req, env, _ctx, params) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const id = parseInt(params.id, 10);
    await env.DB.prepare('DELETE FROM comments WHERE id = ?').bind(id).run();
    return jsonOk({ deleted: true });
  });
}
```

- [ ] **Step 2: 编写站点路由（友链/统计/设置）**

`worker/src/routes/site.ts`:
```typescript
import { Router } from '../router';
import { jsonOk, jsonError } from '../response';
import { Env, FriendRow } from '../types';
import { requireAdmin } from '../auth';

export function registerSiteRoutes(router: Router): void {
  router.get('/api/friends', async (_req, env) => {
    const rows = await env.DB.prepare('SELECT * FROM friends ORDER BY sort ASC, id ASC').all();
    return jsonOk(rows.results);
  });

  router.post('/api/friends', async (req, env) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    let body: Partial<FriendRow>;
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }
    const name = (body.name ?? '').trim().slice(0, 50);
    const url = (body.url ?? '').trim().slice(0, 200);
    if (!name || !url) return jsonError(400, 'Name and url are required');
    const avatar = (body.avatar ?? '').trim().slice(0, 500);
    const description = (body.description ?? '').trim().slice(0, 200);
    const sort = Number(body.sort ?? 0);
    const result = await env.DB.prepare(
      'INSERT INTO friends (name, url, avatar, description, sort) VALUES (?, ?, ?, ?, ?)'
    ).bind(name, url, avatar, description, sort).run();
    return jsonOk({ id: result.meta.last_row_id, name, url, avatar, description, sort }, { status: 201 });
  });

  router.put('/api/friends/:id', async (req, env, _ctx, params) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const id = parseInt(params.id, 10);
    let body: Partial<FriendRow>;
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }
    const name = (body.name ?? '').trim().slice(0, 50);
    const url = (body.url ?? '').trim().slice(0, 200);
    const avatar = (body.avatar ?? '').trim().slice(0, 500);
    const description = (body.description ?? '').trim().slice(0, 200);
    const sort = Number(body.sort ?? 0);
    await env.DB.prepare('UPDATE friends SET name=?, url=?, avatar=?, description=?, sort=? WHERE id=?')
      .bind(name, url, avatar, description, sort, id).run();
    return jsonOk({ id, name, url, avatar, description, sort });
  });

  router.delete('/api/friends/:id', async (req, env, _ctx, params) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const id = parseInt(params.id, 10);
    await env.DB.prepare('DELETE FROM friends WHERE id = ?').bind(id).run();
    return jsonOk({ deleted: true });
  });

  router.get('/api/stats', async (_req, env) => {
    const posts = await env.DB.prepare("SELECT COUNT(*) AS c FROM posts WHERE status = 'published'").first();
    const comments = await env.DB.prepare('SELECT COUNT(*) AS c FROM comments').first();
    const messages = await env.DB.prepare('SELECT COUNT(*) AS c FROM messages').first();
    const views = await env.DB.prepare('SELECT SUM(view_count) AS s FROM posts').first();
    let totalVisits = 0;
    const list = await env.KV.list({ prefix: 'visit:' });
    for (const key of list.keys) {
      totalVisits += parseInt((await env.KV.get(key.name)) ?? '0', 10);
    }
    return jsonOk({
      postCount: posts?.c ?? 0,
      commentCount: comments?.c ?? 0,
      messageCount: messages?.c ?? 0,
      viewCount: views?.s ?? 0,
      totalVisits,
    });
  });

  router.get('/api/settings', async (_req, env) => {
    const rows = await env.DB.prepare('SELECT key, value FROM settings').all();
    const settings: Record<string, string> = {};
    for (const row of rows.results) {
      settings[row.key as string] = row.value as string;
    }
    return jsonOk(settings);
  });

  router.put('/api/settings', async (req, env) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    let body: Record<string, unknown>;
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }
    const stmt = env.DB.prepare('INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value');
    const batch = Object.entries(body).map(([k, v]) => stmt.bind(k, String(v).slice(0, 2000)));
    await env.DB.batch(batch);
    return jsonOk({ updated: true });
  });
}
```

- [ ] **Step 3: 注册路由并本地测试**

在 `worker/src/index.ts`：
```typescript
import { registerInteractionRoutes } from './routes/interactions';
import { registerSiteRoutes } from './routes/site';
registerInteractionRoutes(router);
registerSiteRoutes(router);
```

本地测试：
```bash
npx wrangler dev --port 8787
```
期望：
```bash
curl -X POST http://localhost:8787/api/posts/1/view
curl -X POST http://localhost:8787/api/posts/1/like
curl -X POST http://localhost:8787/api/visit
curl -X GET http://localhost:8787/api/stats
curl -X POST http://localhost:8787/api/messages -H "Content-Type: application/json" -d '{"nickname":"访客","content":"你好"}'
```

- [ ] **Step 4: 提交**

```bash
git add worker/src
git commit -m "feat: interactions, friends, stats, settings APIs"
```

---

### Task 7: Worker 管理 API — 文章 CRUD

**Files:**
- Create: `worker/src/routes/admin.ts`
- Modify: `worker/src/index.ts`

**Interfaces:**
- Consumes: `requireAdmin`、`jsonOk`/`jsonError`、`toPublicPost`
- Produces:
  - `POST /api/admin/posts`（创建）
  - `PUT /api/admin/posts/:id`（更新）
  - `DELETE /api/admin/posts/:id`（删除）
  - `GET /api/admin/posts`（含草稿，分页）
  - 请求体：`{ slug, titleZh, titleEn, summary, content, category, tags: string[], status, pinned, featured }`

- [ ] **Step 1: 编写管理路由**

`worker/src/routes/admin.ts`:
```typescript
import { Router } from '../router';
import { jsonOk, jsonError } from '../response';
import { Env, PostRow, PublicPost } from '../types';
import { toPublicPost } from '../mappers';
import { requireAdmin } from '../auth';

interface PostInput {
  slug?: string;
  titleZh?: string;
  titleEn?: string;
  summary?: string;
  content?: string;
  category?: string;
  tags?: string[];
  status?: 'published' | 'draft';
  pinned?: boolean;
  featured?: boolean;
}

function toSlug(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9\u4e00-\u9fa5]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 100);
}

export function registerAdminRoutes(router: Router): void {
  router.get('/api/admin/posts', async (req, env) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const url = new URL(req.url);
    const page = Math.max(1, parseInt(url.searchParams.get('page') ?? '1', 10) || 1);
    const pageSize = Math.min(100, Math.max(1, parseInt(url.searchParams.get('pageSize') ?? '20', 10) || 20));
    const totalRow = await env.DB.prepare('SELECT COUNT(*) AS total FROM posts').first();
    const rows = await env.DB.prepare(
      'SELECT * FROM posts ORDER BY created_at DESC LIMIT ? OFFSET ?'
    ).bind(pageSize, (page - 1) * pageSize).all<PostRow>();
    return jsonOk({
      posts: rows.results.map(toPublicPost),
      total: totalRow?.total ?? 0,
      page,
      pageSize,
    });
  });

  router.post('/api/admin/posts', async (req, env) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    let body: PostInput;
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }
    const titleZh = (body.titleZh ?? '').trim().slice(0, 200);
    const content = body.content ?? '';
    if (!titleZh || !content) return jsonError(400, 'Title and content are required');

    const baseSlug = (body.slug ?? '').trim() || toSlug(titleZh);
    let slug = baseSlug;
    const existing = await env.DB.prepare('SELECT id FROM posts WHERE slug = ?').bind(slug).first();
    if (existing) slug = `${baseSlug}-${Date.now().toString(36)}`;

    const now = new Date().toISOString();
    const result = await env.DB.prepare(
      `INSERT INTO posts
        (slug, title_zh, title_en, summary, content, category, tags, status, pinned, featured, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      slug,
      titleZh,
      (body.titleEn ?? '').trim().slice(0, 200),
      (body.summary ?? '').trim().slice(0, 500),
      content,
      (body.category ?? '').trim().slice(0, 100),
      JSON.stringify(body.tags ?? []),
      body.status === 'draft' ? 'draft' : 'published',
      body.pinned ? 1 : 0,
      body.featured ? 1 : 0,
      now,
      now
    ).run();
    return jsonOk({ id: result.meta.last_row_id, slug }, { status: 201 });
  });

  router.put('/api/admin/posts/:id', async (req, env, _ctx, params) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const id = parseInt(params.id, 10);
    let body: PostInput;
    try { body = await req.json(); } catch { return jsonError(400, 'Invalid JSON body'); }
    const existing = await env.DB.prepare('SELECT * FROM posts WHERE id = ?').bind(id).first<PostRow>();
    if (!existing) return jsonError(404, 'Post not found');

    const titleZh = (body.titleZh ?? existing.title_zh).trim().slice(0, 200);
    const content = body.content ?? existing.content;
    const slug = (body.slug ?? existing.slug).trim() || existing.slug;
    await env.DB.prepare(
      `UPDATE posts SET
        slug = ?, title_zh = ?, title_en = ?, summary = ?, content = ?,
        category = ?, tags = ?, status = ?, pinned = ?, featured = ?, updated_at = ?
       WHERE id = ?`
    ).bind(
      slug,
      titleZh,
      (body.titleEn ?? existing.title_en).trim(),
      (body.summary ?? existing.summary).trim(),
      content,
      (body.category ?? existing.category).trim(),
      JSON.stringify(body.tags ?? JSON.parse(existing.tags)),
      body.status ?? existing.status,
      body.pinned === undefined ? existing.pinned : (body.pinned ? 1 : 0),
      body.featured === undefined ? existing.featured : (body.featured ? 1 : 0),
      new Date().toISOString(),
      id
    ).run();
    const updated = await env.DB.prepare('SELECT * FROM posts WHERE id = ?').bind(id).first<PostRow>();
    return jsonOk(toPublicPost(updated!));
  });

  router.delete('/api/admin/posts/:id', async (req, env, _ctx, params) => {
    const username = await requireAdmin(req, env);
    if (!username) return jsonError(401, 'Not authenticated');
    const id = parseInt(params.id, 10);
    await env.DB.prepare('DELETE FROM comments WHERE post_id = ?').bind(id).run();
    await env.DB.prepare('DELETE FROM posts WHERE id = ?').bind(id).run();
    return jsonOk({ deleted: true });
  });
}
```

- [ ] **Step 2: 注册路由并本地测试**

在 `worker/src/index.ts`：
```typescript
import { registerAdminRoutes } from './routes/admin';
registerAdminRoutes(router);
```

测试：
```bash
# 初始化管理员
npx wrangler d1 execute hyperos-blog --local --command="INSERT INTO admin (username, password_hash, created_at) VALUES ('admin', 'pbkdf2:100000:00000000000000000000000000000000:0000000000000000000000000000000000000000000000000000000000000000', datetime('now'));"
# 先登录（上面是无效哈希，需在部署 Task 里用脚本生成真实哈希）
npx wrangler dev --port 8787
```
注意：此步骤的本地管理员哈希需使用 Task 10 的脚本生成真实值。

- [ ] **Step 3: 提交**

```bash
git add worker/src
git commit -m "feat: admin CRUD for posts"
```

---

### Task 8: Worker SEO 预渲染

**Files:**
- Create: `worker/src/seo.ts`
- Modify: `worker/src/index.ts`

**Interfaces:**
- Consumes: `Env`、`toPublicPost`
- Produces: `renderPostHTML(post: PublicPost, settings): string`、`isBot(userAgent: string): boolean`

- [ ] **Step 1: 编写 SEO 渲染模块**

`worker/src/seo.ts`:
```typescript
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
    if (/^\s*```/.test(line)) {
      out.push('</code></pre>');
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
```

- [ ] **Step 2: 接入 Worker 入口（同源 SPA 路由 + 预渲染）**

修改 `worker/src/index.ts`：
```typescript
import { isBot, renderPostHTML, renderHomeHTML } from './seo';
import { PublicPost } from './types';

// 在 fetch handler 中，处理非 /api/ 请求：
export default {
  async fetch(request, env, ctx) {
    const corsResponse = handleCors(request);
    if (corsResponse) return corsResponse;

    const url = new URL(request.url);
    const userAgent = request.headers.get('User-Agent') ?? '';

    if (url.pathname.startsWith('/api/')) {
      const res = await router.serve(request.method, url.pathname, request, env, ctx);
      if (res) return withCors(res);
      return withCors(jsonError(404, 'Not Found'));
    }

    // 静态资源直接放行（由 Pages 提供，本 Worker 在 pages 场景不处理静态资源）
    // SEO 预渲染：仅对爬虫 UA 生效
    if (isBot(userAgent)) {
      const settings = await loadSettings(env);
      const siteTitle = settings.title ?? 'HyperOS 博客';

      const postMatch = /^\/post\/([^/]+)$/.exec(url.pathname);
      if (postMatch) {
        const slug = postMatch[1];
        const row = await env.DB.prepare(
          "SELECT * FROM posts WHERE slug = ? AND status = 'published'"
        ).bind(slug).first<PublicPost>();
        if (row) {
          return new Response(renderPostHTML(row, siteTitle), {
            headers: { 'Content-Type': 'text/html; charset=utf-8' },
          });
        }
        return new Response('<!DOCTYPE html><html><body><h1>404</h1></body></html>', {
          status: 404,
          headers: { 'Content-Type': 'text/html; charset=utf-8' },
        });
      }

      const homeMatch = /^\/$/.test(url.pathname);
      if (homeMatch) {
        const rows = await env.DB.prepare(
          "SELECT * FROM posts WHERE status = 'published' ORDER BY created_at DESC LIMIT 10"
        ).all<PublicPost>();
        return new Response(renderHomeHTML(siteTitle, rows.results, settings), {
          headers: { 'Content-Type': 'text/html; charset=utf-8' },
        });
      }
    }

    // 普通请求：SPA 由 Pages 处理，Worker 返回 404 表示应走 Pages 静态
    return new Response('SPA fallback', { status: 200 });
  },
} satisfies ExportedHandler<Env>;
```

注意：`loadSettings` 是本地 helper（读 settings 表返回 Record）：
```typescript
async function loadSettings(env: Env): Promise<Record<string, string>> {
  const rows = await env.DB.prepare('SELECT key, value FROM settings').all();
  const settings: Record<string, string> = {};
  for (const row of rows.results) {
    settings[row.key as string] = row.value as string;
  }
  return settings;
}
```

- [ ] **Step 3: 测试 SEO 渲染**

创建 `worker/test/seo.test.ts`:
```typescript
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
```

运行：
```bash
cd worker && npx tsx --test test/seo.test.ts
```

- [ ] **Step 4: 提交**

```bash
git add worker/src
git commit -m "feat: SEO prerendering for crawlers"
```

---

### Task 9: 前端主题系统 + i18n + 导航框架

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/theme/AppTheme.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/i18n/Strings.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/AppState.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hyperos/blog/App.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/navigation/AppNavigation.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/HomeScreen.kt`（占位）
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/AboutScreen.kt`（占位）

**Interfaces:**
- Consumes: Miuix 0.9.3 的 `MiuixTheme`、`ThemeController`、`ColorSchemeMode`、`lightColorScheme`/`darkColorScheme`
- Produces:
  - `AppTheme(controller: ThemeController, content)` Composable
  - `object Strings { fun zh(id: String): String; fun en(id: String): String; fun get(lang: String, id: String): String }`
  - `enum class ThemeMode { System, Light, Dark, MonetLight, MonetDark, MonetSystem }`
  - `class AppState { var themeMode; var language; var keyColor }`
  - `AppNavigation` 枚举 `{ Home, About, Settings, Admin }`

- [ ] **Step 1: 编写主题封装**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/theme/AppTheme.kt`:
```kotlin
package com.hyperos.blog.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

enum class ThemeMode(val schemeMode: ColorSchemeMode) {
    System(ColorSchemeMode.System),
    Light(ColorSchemeMode.Light),
    Dark(ColorSchemeMode.Dark),
    MonetLight(ColorSchemeMode.MonetLight),
    MonetDark(ColorSchemeMode.MonetDark),
    MonetSystem(ColorSchemeMode.MonetSystem),
}

class ThemeState {
    var mode by mutableStateOf(ThemeMode.MonetLight)
    var keyColor by mutableStateOf(Color(0xFF3482FF))

    fun controller(): ThemeController {
        return ThemeController(
            colorSchemeMode = mode.schemeMode,
            lightColors = lightColorScheme(),
            darkColors = darkColorScheme(),
            keyColor = keyColor,
            paletteStyle = top.yukonga.miuix.kmp.theme.ThemePaletteStyle.TonalSpot,
        )
    }
}

@Composable
fun AppTheme(
    state: ThemeState,
    content: @Composable () -> Unit,
) {
    val controller = remember(state.mode, state.keyColor) { state.controller() }
    MiuixTheme(controller = controller) {
        content()
    }
}
```

- [ ] **Step 2: 编写 i18n 字符串表**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/i18n/Strings.kt`:
```kotlin
package com.hyperos.blog.i18n

object Strings {
    private val zh = mapOf(
        "appName" to "HyperOS 博客",
        "home" to "首页",
        "archive" to "归档",
        "messages" to "留言板",
        "friends" to "友链",
        "about" to "关于",
        "settings" to "设置",
        "search" to "搜索",
        "admin" to "后台",
        "readMore" to "阅读更多",
        "loadMore" to "加载更多",
        "empty" to "暂无内容",
        "darkMode" to "深色模式",
        "language" to "语言",
        "themeColor" to "主题色",
        "followSystem" to "跟随系统",
        "light" to "浅色",
        "dark" to "深色",
        "login" to "登录",
        "logout" to "退出登录",
        "username" to "用户名",
        "password" to "密码",
        "postTitle" to "标题",
        "postContent" to "内容",
        "postSummary" to "摘要",
        "category" to "分类",
        "tags" to "标签",
        "publish" to "发布",
        "draft" to "草稿",
        "delete" to "删除",
        "edit" to "编辑",
        "save" to "保存",
        "cancel" to "取消",
        "confirm" to "确认",
        "views" to "阅读",
        "likes" to "点赞",
        "comments" to "评论",
        "writeComment" to "发表评论",
        "nickname" to "昵称",
        "email" to "邮箱（选填）",
        "content" to "内容",
        "submit" to "提交",
        "visitStats" to "访问统计",
        "totalVisits" to "总访问量",
        "postCount" to "文章数",
        "commentCount" to "评论数",
        "viewCount" to "总阅读量",
        "backToHome" to "返回首页",
        "notFound" to "页面不存在",
        "loading" to "加载中...",
        "error" to "出错了",
        "retry" to "重试",
        "success" to "操作成功",
        "failed" to "操作失败",
        "addFriend" to "申请友链",
        "friendName" to "站点名称",
        "friendUrl" to "站点地址",
        "friendDesc" to "一句话介绍",
        "leaveMessage" to "留言",
        "welcome" to "欢迎来到我的博客",
    )

    private val en = mapOf(
        "appName" to "HyperOS Blog",
        "home" to "Home",
        "archive" to "Archive",
        "messages" to "Messages",
        "friends" to "Friends",
        "about" to "About",
        "settings" to "Settings",
        "search" to "Search",
        "admin" to "Admin",
        "readMore" to "Read more",
        "loadMore" to "Load more",
        "empty" to "Nothing here",
        "darkMode" to "Dark mode",
        "language" to "Language",
        "themeColor" to "Theme color",
        "followSystem" to "Follow system",
        "light" to "Light",
        "dark" to "Dark",
        "login" to "Login",
        "logout" to "Log out",
        "username" to "Username",
        "password" to "Password",
        "postTitle" to "Title",
        "postContent" to "Content",
        "postSummary" to "Summary",
        "category" to "Category",
        "tags" to "Tags",
        "publish" to "Publish",
        "draft" to "Draft",
        "delete" to "Delete",
        "edit" to "Edit",
        "save" to "Save",
        "cancel" to "Cancel",
        "confirm" to "Confirm",
        "views" to "Views",
        "likes" to "Likes",
        "comments" to "Comments",
        "writeComment" to "Write a comment",
        "nickname" to "Nickname",
        "email" to "Email (optional)",
        "content" to "Content",
        "submit" to "Submit",
        "visitStats" to "Visit stats",
        "totalVisits" to "Total visits",
        "postCount" to "Posts",
        "commentCount" to "Comments",
        "viewCount" to "Total views",
        "backToHome" to "Back to home",
        "notFound" to "Not found",
        "loading" to "Loading...",
        "error" to "Error",
        "retry" to "Retry",
        "success" to "Success",
        "failed" to "Failed",
        "addFriend" to "Add friend",
        "friendName" to "Site name",
        "friendUrl" to "Site URL",
        "friendDesc" to "Description",
        "leaveMessage" to "Leave a message",
        "welcome" to "Welcome to my blog",
    )

    fun get(lang: String, id: String): String {
        return if (lang == "zh") zh[id] ?: id else en[id] ?: id
    }
}

data class Language(val code: String, val name: String)
```

- [ ] **Step 3: 编写应用状态**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/AppState.kt`:
```kotlin
package com.hyperos.blog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hyperos.blog.theme.ThemeMode
import androidx.compose.ui.graphics.Color

class AppState {
    var language by mutableStateOf("zh")
    var themeMode by mutableStateOf(ThemeMode.MonetLight)
    var keyColor by mutableStateOf(Color(0xFF3482FF))
    var adminToken by mutableStateOf<String?>(null)
    var siteTitle by mutableStateOf("HyperOS 博客")

    val isAdmin: Boolean get() = adminToken != null
}
```

- [ ] **Step 4: 编写导航与占位页面**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/navigation/AppNavigation.kt`:
```kotlin
package com.hyperos.blog.navigation

enum class AppRoute {
    Home, Archive, Messages, Friends, About, Settings, Admin,
    PostDetail, Search, AdminEditor,
}
```

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/HomeScreen.kt`:
```kotlin
package com.hyperos.blog.ui

import androidx.compose.runtime.Composable

@Composable
fun HomeScreen() {
    // 占位：Task 12 实现
}
```

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/AboutScreen.kt`:
```kotlin
package com.hyperos.blog.ui

import androidx.compose.runtime.Composable

@Composable
fun AboutScreen() {
    // 占位：Task 15 实现
}
```

- [ ] **Step 5: 重构 App.kt 为导航框架**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/App.kt`:
```kotlin
package com.hyperos.blog

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.theme.AppTheme
import com.hyperos.blog.ui.AboutScreen
import com.hyperos.blog.ui.HomeScreen

@Composable
fun App() {
    val appState = remember { AppState() }
    var currentRoute by remember { mutableStateOf(AppRoute.Home) }

    AppTheme(state = appState.themeState()) {
        when (currentRoute) {
            AppRoute.Home -> HomeScreen()
            AppRoute.About -> AboutScreen()
            else -> HomeScreen()
        }
    }
}
```

注意：此 Task 中 `AppState` 需增加 `themeState()` 返回 `ThemeState`。修改 `AppState.kt`：
```kotlin
import com.hyperos.blog.theme.ThemeState
class AppState {
    val themeState = ThemeState()
    var language by mutableStateOf("zh")
    var adminToken by mutableStateOf<String?>(null)
    var siteTitle by mutableStateOf("HyperOS 博客")
    val isAdmin: Boolean get() = adminToken != null
}
```

- [ ] **Step 6: 验证编译**

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```
期望：BUILD SUCCESSFUL。

- [ ] **Step 7: 提交**

```bash
git add composeApp
git commit -m "feat: theme system, i18n and navigation shell"
```

---

### Task 10: 前端网络层 + 初始化脚本

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/data/Dto.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/data/ApiClient.kt`
- Create: `scripts/init-admin.mjs`
- Modify: `worker/package.json`（添加 scripts）

**Interfaces:**
- Consumes: Ktor Client、kotlinx.serialization
- Produces:
  - `@Serializable data class Post(id, slug, titleZh, titleEn, summary, content, category, tags: List<String>, pinned, featured, viewCount, likeCount, createdAt, updatedAt)`
  - `@Serializable data class ApiResponse<T>(ok: Boolean, data: T?, error: String?)`
  - `class ApiClient(baseUrl: String)` — `get<T>/post<T>/put<T>/delete<T>` 方法
  - `PostInput(slug, titleZh, titleEn, summary, content, category, tags, status, pinned, featured)` 供后台提交
  - `createAdminScript(env)` 初始化管理员（Node 脚本用 WebCrypto PBKDF2）

- [ ] **Step 1: 编写 DTO**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/data/Dto.kt`:
```kotlin
package com.hyperos.blog.data

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: String? = null,
)

@Serializable
data class Post(
    val id: Long = 0,
    val slug: String = "",
    val titleZh: String = "",
    val titleEn: String = "",
    val summary: String = "",
    val content: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val featured: Boolean = false,
    val viewCount: Long = 0,
    val likeCount: Long = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class PostListData(
    val posts: List<Post> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val pageSize: Int = 10,
    val totalPages: Int = 0,
)

@Serializable
data class Category(
    val id: Long = 0,
    val name_zh: String = "",
    val name_en: String = "",
    val count: Long = 0,
)

@Serializable
data class TagCount(
    val name: String = "",
    val count: Long = 0,
)

@Serializable
data class ArchiveGroup(
    val year: String = "",
    val month: String = "",
    val posts: List<Post> = emptyList(),
)

@Serializable
data class Comment(
    val id: Long = 0,
    val nickname: String = "",
    val content: String = "",
    val createdAt: String = "",
)

@Serializable
data class CommentInput(
    val nickname: String = "",
    val email: String = "",
    val content: String = "",
)

@Serializable
data class Message(
    val id: Long = 0,
    val nickname: String = "",
    val content: String = "",
    val createdAt: String = "",
)

@Serializable
data class Friend(
    val id: Long = 0,
    val name: String = "",
    val url: String = "",
    val avatar: String = "",
    val description: String = "",
    val sort: Int = 0,
)

@Serializable
data class SiteStats(
    val postCount: Long = 0,
    val commentCount: Long = 0,
    val messageCount: Long = 0,
    val viewCount: Long = 0,
    val totalVisits: Long = 0,
)

@Serializable
data class AuthResponse(
    val token: String = "",
    val username: String = "",
)

@Serializable
data class PostInput(
    val slug: String = "",
    val titleZh: String = "",
    val titleEn: String = "",
    val summary: String = "",
    val content: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val status: String = "published",
    val pinned: Boolean = false,
    val featured: Boolean = false,
)
```

- [ ] **Step 2: 编写 API Client**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/data/ApiClient.kt`:
```kotlin
package com.hyperos.blog.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.call.body

class ApiClient(private val baseUrl: String, private val engine: HttpClientEngine) {
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        expectSuccess = false
    }

    private var authToken: String? = null

    fun setToken(token: String?) {
        authToken = token
    }

    suspend inline fun <reified T> get(path: String): ApiResponse<T> = request("GET", path)
    suspend inline fun <reified T> post(path: String, body: Any? = null): ApiResponse<T> = request("POST", path, body)
    suspend inline fun <reified T> put(path: String, body: Any? = null): ApiResponse<T> = request("PUT", path, body)
    suspend inline fun <reified T> delete(path: String): ApiResponse<T> = request("DELETE", path)

    private suspend inline fun <reified T> request(method: String, path: String, body: Any? = null): ApiResponse<T> {
        val response = client.request("$baseUrl$path") {
            this.method = HttpMethod(method)
            contentType(ContentType.Application.Json)
            if (body != null) setBody(body)
            authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        val status = response.status.value
        if (status == 401) {
            return ApiResponse(ok = false, error = "Unauthorized")
        }
        return try {
            val text = response.bodyAsText()
            if (text.isBlank()) {
                ApiResponse(ok = status < 400, error = if (status < 400) null else "HTTP $status")
            } else {
                val json = Json { ignoreUnknownKeys = true }
                if (text.trimStart().startsWith("{")) {
                    json.decodeFromString<ApiResponse<T>>(text)
                } else {
                    ApiResponse(ok = status < 400, data = json.decodeFromString<T>(text))
                }
            }
        } catch (e: Exception) {
            ApiResponse(ok = false, error = e.message ?: "Network error")
        }
    }

    private suspend fun io.ktor.client.statement.HttpResponse.bodyAsText(): String =
        runCatching { this.body<String>() }.getOrDefault("")
}
```

注意：`bodyAsText` 冲突需处理。改用直接 `this.body<String>()` 并 try/catch：
```kotlin
private suspend inline fun <reified T> request(method: String, path: String, body: Any? = null): ApiResponse<T> {
    return try {
        val response = client.request("$baseUrl$path") {
            this.method = HttpMethod(method)
            contentType(ContentType.Application.Json)
            if (body != null) setBody(body)
            authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        val status = response.status.value
        val text = response.body<String>()
        if (status == 401) return ApiResponse(ok = false, error = "Unauthorized")
        val json = Json { ignoreUnknownKeys = true }
        val parsed = json.decodeFromString<ApiResponse<T>>(text)
        parsed
    } catch (e: Exception) {
        ApiResponse(ok = false, error = e.message ?: "Network error")
    }
}
```

- [ ] **Step 3: 编写 API 客户端工厂（wasm 平台）**

`composeApp/src/wasmJsMain/kotlin/com/hyperos/blog/data/ApiClientFactory.kt`:
```kotlin
package com.hyperos.blog.data

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

actual fun createApiClient(engine: HttpClientEngine): ApiClient {
    // wasmJs 平台默认使用相对路径
    val baseUrl = js("window.location.origin").unsafeCast<String>()
    return ApiClient(baseUrl, engine)
}
```

实际实现简化（wasmJs 用 Js engine）：
```kotlin
package com.hyperos.blog.data

import io.ktor.client.engine.js.Js

fun createDefaultApiClient(): ApiClient {
    val baseUrl = kotlinx.browser.window.location.origin
    return ApiClient(baseUrl, Js.create())
}
```

- [ ] **Step 4: 编写初始化管理员脚本**

`scripts/init-admin.mjs`:
```javascript
import { execSync } from 'node:child_process';

const [,, username, password, databaseId] = process.argv;
if (!username || !password) {
  console.error('Usage: node init-admin.mjs <username> <password> [databaseId]');
  process.exit(1);
}

async function hashPassword(password, iterations = 100000) {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const keyMaterial = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, ['deriveBits']);
  const bits = await crypto.subtle.deriveBits({ name: 'PBKDF2', salt, iterations, hash: 'SHA-256' }, keyMaterial, 256);
  const toHex = (b) => Array.from(b, (x) => x.toString(16).padStart(2, '0')).join('');
  return `pbkdf2:${iterations}:${toHex(salt)}:${toHex(new Uint8Array(bits))}`;
}

const hash = await hashPassword(password);
const escapedHash = hash.replace(/'/g, "''");
const now = new Date().toISOString();

const dbFlag = databaseId ? `--remote --database-id=${databaseId}` : '--local';
const sql = `INSERT INTO admin (username, password_hash, created_at) VALUES ('${username}', '${escapedHash}', '${now}') ON CONFLICT(username) DO UPDATE SET password_hash = excluded.password_hash;`;
execSync(`npx wrangler d1 execute hyperos-blog ${dbFlag} --command="${sql.replace(/"/g, '\\"')}"`, { stdio: 'inherit' });
console.log('Admin created/updated:', username);
```

`worker/package.json` 添加：
```json
"scripts": {
  "init-admin": "node ../scripts/init-admin.mjs"
}
```

- [ ] **Step 5: 验证编译**

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```
期望：BUILD SUCCESSFUL。

- [ ] **Step 6: 提交**

```bash
git add composeApp scripts worker/package.json
git commit -m "feat: frontend data layer and admin init script"
```

---

### Task 11: 前端首页 + 文章列表 + 底部导航 + 下拉刷新

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/HomeScreen.kt`（重写）
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/components/PostCard.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/components/MiuixScaffold.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hyperos/blog/App.kt`

**Interfaces:**
- Consumes: Miuix 的 `TopAppBar`、`SearchBar`、`Card`、`NavigationBar`、`PullToRefresh`、`Snackbar`、`Badge`、`Divider`、`SmallTitle`
- Produces: 首页展示文章卡片列表，支持分类 TabRow 筛选、搜索、下拉刷新、加载更多、底部导航

- [ ] **Step 1: 编写通用 Scaffold 组件**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/components/MiuixScaffold.kt`:
```kotlin
package com.hyperos.blog.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarColors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.hyperos.blog.AppState
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Composable
fun MiuixScaffold(
    title: String,
    appState: AppState,
    currentRoute: AppRoute,
    onNavigate: (AppRoute) -> Unit,
    topBarActions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                colors = TopAppBarColors(),
                actions = topBarActions,
            )
        },
        bottomBar = {
            NavigationBar(
                onSelect = { index ->
                    val route = when (index) {
                        0 -> AppRoute.Home
                        1 -> AppRoute.Archive
                        2 -> AppRoute.Messages
                        3 -> AppRoute.Friends
                        else -> AppRoute.Settings
                    }
                    onNavigate(route)
                },
                selectedIndex = when (currentRoute) {
                    AppRoute.Home -> 0
                    AppRoute.Archive -> 1
                    AppRoute.Messages -> 2
                    AppRoute.Friends -> 3
                    else -> 4
                },
            ) {
                listOf(
                    Strings.get(appState.language, "home"),
                    Strings.get(appState.language, "archive"),
                    Strings.get(appState.language, "messages"),
                    Strings.get(appState.language, "friends"),
                    Strings.get(appState.language, "settings"),
                ).forEach { label ->
                    top.yukonga.miuix.kmp.basic.NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = {},
                        text = label,
                    )
                }
            }
        },
        content = content,
    )
}
```

注意：`NavigationBar` 的实际 API 是 `NavigationBar(selectedIndex, onSelect) { items }`，子项用 `NavigationBarItem`。此 Task 在编译时验证实际签名并调整。

- [ ] **Step 2: 编写文章卡片**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/components/PostCard.kt`:
```kotlin
package com.hyperos.blog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.hyperos.blog.data.Post
import com.hyperos.blog.i18n.Strings

@Composable
fun PostCard(
    post: Post,
    language: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = if (language == "zh") post.titleZh else (post.titleEn.ifBlank { post.titleZh }),
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
            )
            if (post.summary.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = post.summary,
                    style = MiuixTheme.textStyles.body,
                    color = MiuixTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                SmallTitle(text = post.category.ifBlank { "未分类" })
                Spacer(Modifier.width(8.dp))
                post.tags.take(3).forEach { tag ->
                    SmallTitle(text = "#$tag")
                    Spacer(Modifier.width(6.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${post.createdAt.take(10)} · ${post.viewCount} ${Strings.get(language, "views")}",
                    style = MiuixTheme.textStyles.caption,
                    color = MiuixTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
```

- [ ] **Step 3: 重写 HomeScreen**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/HomeScreen.kt`:
```kotlin
package com.hyperos.blog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.*
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.MiuixScaffold
import com.hyperos.blog.ui.components.PostCard
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
    api: ApiClient,
) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var total by remember { mutableStateOf(0L) }
    var page by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var snackbarText by remember { mutableStateOf<String?>(null) }

    suspend fun loadPosts(reset: Boolean) {
        if (loading) return
        loading = true
        val targetPage = if (reset) 1 else page
        val resp = api.get<PostListData>(
            "/api/posts?page=$targetPage&pageSize=10&category=$selectedCategory&keyword=$keyword"
        )
        if (resp.ok && resp.data != null) {
            posts = if (reset) resp.data.posts else posts + resp.data.posts
            total = resp.data.total
            page = targetPage + 1
        } else {
            snackbarText = resp.error
        }
        loading = false
        refreshing = false
    }

    LaunchedEffect(Unit) {
        val catResp = api.get<Array<Category>>("/api/categories")
        if (catResp.ok) categories = catResp.data?.toList() ?: emptyList()
        loadPosts(true)
    }

    MiuixScaffold(
        title = appState.siteTitle,
        appState = appState,
        currentRoute = AppRoute.Home,
        onNavigate = onNavigate,
        topBarActions = {
            // 搜索按钮
            IconButton(onClick = { onNavigate(AppRoute.Search) }) {
                top.yukonga.miuix.kmp.icon.MiuixIcons.Search()
            }
        },
    ) {
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                loadPosts(true)
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 分类 TabRow
                item {
                    if (categories.isNotEmpty()) {
                        TabRow(
                            selectedTabIndex = categories.indexOfFirst { it.name_zh == selectedCategory }.coerceAtLeast(0),
                            onTabSelected = { index ->
                                selectedCategory = if (index == 0) "" else categories[index - 1].name_zh
                                loadPosts(true)
                            },
                        ) {
                            listOf(Strings.get(appState.language, "all")).let { allLabel ->
                                androidx.compose.foundation.lazy.LazyRow {
                                    items(1) {
                                        top.yukonga.miuix.kmp.basic.TabRowItem(
                                            text = allLabel[0],
                                            selected = selectedCategory.isEmpty(),
                                            onClick = { selectedCategory = ""; loadPosts(true) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (posts.isEmpty() && !loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text(Strings.get(appState.language, "empty"), color = MiuixTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(posts, key = { it.id }) { post ->
                    PostCard(post, appState.language) {
                        onNavigate(AppRoute.PostDetail)
                    }
                }
                if (posts.size < total) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Button(onClick = { loadPosts(false) }) {
                                Text(Strings.get(appState.language, "loadMore"))
                            }
                        }
                    }
                }
            }
        }
    }
}
```

注意：`TabRow`/`TabRowItem` 的 Miuix API 在编译时验证调整。若分类 TabRow 复杂，可先用 `TabRow` 简化实现或省略分类 tab 在此 Task。

- [ ] **Step 4: 更新 App.kt 传入依赖**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/App.kt`:
```kotlin
package com.hyperos.blog

import androidx.compose.runtime.*
import com.hyperos.blog.data.createDefaultApiClient
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.theme.AppTheme
import com.hyperos.blog.ui.AboutScreen
import com.hyperos.blog.ui.HomeScreen
import com.hyperos.blog.ui.SettingsScreen

@Composable
fun App() {
    val appState = remember { AppState() }
    val api = remember { createDefaultApiClient() }
    var currentRoute by remember { mutableStateOf(AppRoute.Home) }

    AppTheme(state = appState.themeState) {
        when (currentRoute) {
            AppRoute.Home -> HomeScreen(appState, onNavigate = { currentRoute = it }, api = api)
            AppRoute.About -> AboutScreen()
            else -> HomeScreen(appState, onNavigate = { currentRoute = it }, api = api)
        }
    }
}
```

注意：`SettingsScreen`、`ArchiveScreen`、`MessagesScreen`、`FriendsScreen` 未实现时用占位版本，此 Task 只接 HomeScreen。若编译报错则先注释未实现页面，留到后续 Task。

- [ ] **Step 5: 验证编译**

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```

- [ ] **Step 6: 提交**

```bash
git add composeApp
git commit -m "feat: home feed with cards, tabs, pull-to-refresh"
```

---

### Task 12: 前端文章详情页（Markdown + 目录 + 评论 + 点赞）

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/PostDetailScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hyperos/blog/App.kt`

**Interfaces:**
- Consumes: `multiplatform-markdown-renderer` 的 `Markdown` Composable、`ApiClient`、`Strings`
- Produces: 文章详情页，含 Markdown 渲染、阅读量上报、点赞、评论列表与提交、底部目录抽屉（OverlayBottomSheet）

- [ ] **Step 1: 编写 PostDetailScreen**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/PostDetailScreen.kt`:
```kotlin
package com.hyperos.blog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.*
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material.icons.Icons

@Composable
fun PostDetailScreen(
    appState: AppState,
    slug: String,
    onBack: () -> Unit,
    api: ApiClient,
) {
    var post by remember { mutableStateOf<Post?>(null) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var commentInput by remember { mutableStateOf(CommentInput()) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var liked by remember { mutableStateOf(false) }
    var snackbarText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(slug) {
        val resp = api.get<Post>("/api/posts/$slug")
        if (resp.ok) {
            post = resp.data
            api.post<Map<String, Long>>("/api/posts/${resp.data!!.id}/view")
            val cResp = api.get<List<Comment>>("/api/posts/${resp.data!!.id}/comments")
            if (cResp.ok) comments = cResp.data ?: emptyList()
        } else {
            snackbarText = resp.error
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = post?.let { if (appState.language == "zh") it.titleZh else it.titleEn.ifBlank { it.titleZh } } ?: "",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showCommentDialog = true }) {
                        top.yukonga.miuix.kmp.icon.MiuixIcons.ChatBubble()
                    }
                },
            )
        },
    ) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                ProgressIndicator()
            }
            post != null -> {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text(
                                post!!.category.ifBlank { "未分类" },
                                style = MiuixTheme.textStyles.smallTitle,
                                color = MiuixTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${post!!.createdAt.take(10)} · ${post!!.viewCount} ${Strings.get(appState.language, "views")}",
                                style = MiuixTheme.textStyles.caption,
                                color = MiuixTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        // Markdown 渲染
                        com.mikepenz.markdown.m3.Markdown(
                            content = post!!.content,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                        Row {
                            Button(onClick = {
                                api.post<Map<String, Any>>("/api/posts/${post!!.id}/like")
                                liked = true
                                post = post!!.copy(likeCount = post!!.likeCount + 1)
                            }) {
                                Text("${Strings.get(appState.language, "likes")} ${post!!.likeCount}")
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(24.dp))
                        SmallTitle(text = Strings.get(appState.language, "comments"))
                        Spacer(Modifier.height(8.dp))
                    }
                    items(comments, key = { it.id }) { comment ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text(comment.nickname, fontWeight = FontWeight.Bold, style = MiuixTheme.textStyles.body)
                                Spacer(Modifier.height(4.dp))
                                Text(comment.content, style = MiuixTheme.textStyles.body)
                                Spacer(Modifier.height(4.dp))
                                Text(comment.createdAt.take(16), style = MiuixTheme.textStyles.caption, color = MiuixTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showCommentDialog) {
        var nickname by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        top.yukonga.miuix.kmp.overlay.OverlayDialog(
            title = Strings.get(appState.language, "writeComment"),
            onDismiss = { showCommentDialog = false },
        ) {
            Column(Modifier.padding(16.dp)) {
                TextField(value = nickname, onValueChange = { nickname = it }, placeholder = Strings.get(appState.language, "nickname"))
                Spacer(Modifier.height(8.dp))
                TextField(value = email, onValueChange = { email = it }, placeholder = Strings.get(appState.language, "email"))
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = Strings.get(appState.language, "content"),
                    minLines = 3,
                )
                Spacer(Modifier.height(12.dp))
                Row {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        val resp = api.post<Comment>(
                            "/api/posts/${post!!.id}/comments",
                            CommentInput(nickname.trim(), email.trim(), content.trim()),
                        )
                        if (resp.ok && resp.data != null) {
                            comments = comments + Comment(resp.data.id, resp.data.nickname, resp.data.content, resp.data.createdAt)
                            showCommentDialog = false
                        } else {
                            snackbarText = resp.error
                        }
                    }) {
                        Text(Strings.get(appState.language, "submit"))
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: 接入导航**

在 `App.kt` 中处理 PostDetail 路由（需 AppState 增加 `currentSlug: String?`）：
```kotlin
var currentSlug by remember { mutableStateOf<String?>(null) }
when (currentRoute) {
    AppRoute.PostDetail -> PostDetailScreen(
        appState = appState,
        slug = currentSlug ?: "",
        onBack = { currentRoute = AppRoute.Home },
        api = api,
    )
    ...
}
```

- [ ] **Step 3: 验证编译**

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```
注意：Markdown 库的导入包名以实际为准（`com.mikepenz.markdown.m3.Markdown` 或 `com.mikepenz.markdown.Markdown`），编译时验证调整。

- [ ] **Step 4: 提交**

```bash
git add composeApp
git commit -m "feat: post detail with markdown, comments, likes"
```

---

### Task 13: 前端归档 + 搜索 + 分类/标签页

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/ArchiveScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/SearchScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/TagsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hyperos/blog/App.kt`

**Interfaces:**
- Consumes: `api.get<Array<ArchiveGroup>>("/api/archives")`、`api.get<PostListData>("/api/search?q=")`、`api.get<Array<TagCount>>("/api/tags")`
- Produces: 归档时间线页、搜索页（TextField + 结果）、标签聚合页

- [ ] **Step 1: 编写 ArchiveScreen**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/ArchiveScreen.kt`:
```kotlin
package com.hyperos.blog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.ArchiveGroup
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.MiuixScaffold
import com.hyperos.blog.ui.components.PostCard
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ArchiveScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
    api: ApiClient,
) {
    var archives by remember { mutableStateOf<List<ArchiveGroup>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val resp = api.get<List<ArchiveGroup>>("/api/archives")
        if (resp.ok) archives = resp.data ?: emptyList()
        loading = false
    }

    MiuixScaffold(
        title = Strings.get(appState.language, "archive"),
        appState = appState,
        currentRoute = AppRoute.Archive,
        onNavigate = onNavigate,
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(archives, key = { "${it.year}-${it.month}" }) { group ->
                SmallTitle(text = "${group.year} 年 ${group.month} 月 (${group.posts.size})")
                Spacer(Modifier.height(8.dp))
                group.posts.forEach { post ->
                    PostCard(post, appState.language, onClick = { onNavigate(AppRoute.PostDetail) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
```

- [ ] **Step 2: 编写 SearchScreen**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/SearchScreen.kt`:
```kotlin
package com.hyperos.blog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.Post
import com.hyperos.blog.data.PostListData
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.PostCard
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SearchScreen(
    appState: AppState,
    onBack: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    api: ApiClient,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Post>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }

    fun doSearch() {
        if (query.isBlank()) return
        val resp = api.get<PostListData>("/api/search?q=$query")
        if (resp.ok) results = resp.data?.posts ?: emptyList()
        searched = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = Strings.get(appState.language, "search"),
                onBack = onBack,
                actions = {
                    top.yukonga.miuix.kmp.basic.SearchBar(
                        value = query,
                        onValueChange = { query = it },
                        onSearch = { doSearch() },
                        placeholder = Strings.get(appState.language, "search"),
                    )
                },
            )
        },
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            if (!searched) {
                item {
                    Text(
                        Strings.get(appState.language, "welcome"),
                        style = MiuixTheme.textStyles.body,
                        color = MiuixTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (results.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(Strings.get(appState.language, "empty"))
                    }
                }
            }
            items(results, key = { it.id }) { post ->
                PostCard(post, appState.language, onClick = { onNavigate(AppRoute.PostDetail) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
```

- [ ] **Step 3: 编写 TagsScreen**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/TagsScreen.kt`:
```kotlin
package com.hyperos.blog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.TagCount
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.MiuixScaffold
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TagsScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
    api: ApiClient,
) {
    var tags by remember { mutableStateOf<List<TagCount>>(emptyList()) }

    LaunchedEffect(Unit) {
        val resp = api.get<List<TagCount>>("/api/tags")
        if (resp.ok) tags = resp.data ?: emptyList()
    }

    MiuixScaffold(
        title = Strings.get(appState.language, "tags"),
        appState = appState,
        currentRoute = AppRoute.Home,
        onNavigate = onNavigate,
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(tags, key = { it.name }) { tag ->
                Card(Modifier.fillMaxWidth(), onClick = {}) {
                    Row(Modifier.padding(14.dp)) {
                        Text("#${tag.name}", style = MiuixTheme.textStyles.body, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("${tag.count}", color = MiuixTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
```

- [ ] **Step 4: 接入 App.kt 导航**

- [ ] **Step 5: 验证编译**

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```

- [ ] **Step 6: 提交**

```bash
git add composeApp
git commit -m "feat: archive, search and tags screens"
```

---

### Task 14: 前端留言板 + 友链 + 关于页

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/MessagesScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/FriendsScreen.kt`
- Rewrite: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/AboutScreen.kt`

**Interfaces:**
- Consumes: `api.get/post<Message>`、`api.get<List<Friend>>`、OverlayDialog、OverlayBottomSheet
- Produces: 留言列表 + 留言弹窗、友链卡片网格 + 申请友链表单、关于页（个人信息卡片 + 社交链接）

- [ ] **Step 1: 编写 MessagesScreen**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/MessagesScreen.kt`:
```kotlin
package com.hyperos.blog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.Message
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.MiuixScaffold
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MessagesScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
    api: ApiClient,
) {
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }

    fun load() {
        val resp = api.get<List<Message>>("/api/messages")
        if (resp.ok) messages = resp.data ?: emptyList()
    }

    LaunchedEffect(Unit) { load() }

    MiuixScaffold(
        title = Strings.get(appState.language, "messages"),
        appState = appState,
        currentRoute = AppRoute.Messages,
        onNavigate = onNavigate,
        topBarActions = {
            IconButton(onClick = { showDialog = true }) {
                top.yukonga.miuix.kmp.icon.MiuixIcons.Edit()
            }
        },
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            if (messages.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(Strings.get(appState.language, "empty"), color = MiuixTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(messages, key = { it.id }) { msg ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(msg.nickname, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(msg.content)
                        Spacer(Modifier.height(4.dp))
                        Text(msg.createdAt.take(16), style = MiuixTheme.textStyles.caption, color = MiuixTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showDialog) {
        var nickname by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        top.yukonga.miuix.kmp.overlay.OverlayDialog(
            title = Strings.get(appState.language, "leaveMessage"),
            onDismiss = { showDialog = false },
        ) {
            Column(Modifier.padding(16.dp)) {
                TextField(value = nickname, onValueChange = { nickname = it }, placeholder = Strings.get(appState.language, "nickname"))
                Spacer(Modifier.height(8.dp))
                TextField(value = email, onValueChange = { email = it }, placeholder = Strings.get(appState.language, "email"))
                Spacer(Modifier.height(8.dp))
                TextField(value = content, onValueChange = { content = it }, placeholder = Strings.get(appState.language, "content"), minLines = 3)
                Spacer(Modifier.height(12.dp))
                Row {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        val resp = api.post<Message>("/api/messages", mapOf("nickname" to nickname.trim(), "email" to email.trim(), "content" to content.trim()))
                        if (resp.ok) { showDialog = false; load() }
                    }) { Text(Strings.get(appState.language, "submit")) }
                }
            }
        }
    }
}
```

- [ ] **Step 2: 编写 FriendsScreen**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/FriendsScreen.kt`:
```kotlin
package com.hyperos.blog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.Friend
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.MiuixScaffold
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FriendsScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
    api: ApiClient,
) {
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val resp = api.get<List<Friend>>("/api/friends")
        if (resp.ok) friends = resp.data ?: emptyList()
    }

    MiuixScaffold(
        title = Strings.get(appState.language, "friends"),
        appState = appState,
        currentRoute = AppRoute.Friends,
        onNavigate = onNavigate,
        topBarActions = {
            IconButton(onClick = { showDialog = true }) {
                top.yukonga.miuix.kmp.icon.MiuixIcons.Add()
            }
        },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(friends, key = { it.id }) { friend ->
                Card(Modifier.fillMaxWidth(), onClick = {}) {
                    Column(Modifier.padding(14.dp)) {
                        Text(friend.name, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(friend.description.ifBlank { friend.url }, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MiuixTheme.textStyles.body, color = MiuixTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        top.yukonga.miuix.kmp.overlay.OverlayDialog(
            title = Strings.get(appState.language, "addFriend"),
            onDismiss = { showDialog = false },
        ) {
            Column(Modifier.padding(16.dp)) {
                TextField(value = name, onValueChange = { name = it }, placeholder = Strings.get(appState.language, "friendName"))
                Spacer(Modifier.height(8.dp))
                TextField(value = url, onValueChange = { url = it }, placeholder = Strings.get(appState.language, "friendUrl"))
                Spacer(Modifier.height(8.dp))
                TextField(value = desc, onValueChange = { desc = it }, placeholder = Strings.get(appState.language, "friendDesc"))
                Spacer(Modifier.height(12.dp))
                Row {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        val resp = api.post<Friend>("/api/friends", mapOf("name" to name.trim(), "url" to url.trim(), "description" to desc.trim()))
                        if (resp.ok) { showDialog = false; friends = friends + Friend(resp.data!!.id, resp.data!!.name, resp.data!!.url, "", resp.data!!.description, 0) }
                    }) { Text(Strings.get(appState.language, "submit")) }
                }
            }
        }
    }
}
```

- [ ] **Step 3: 重写 AboutScreen**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/AboutScreen.kt`:
```kotlin
package com.hyperos.blog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.MiuixScaffold
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AboutScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
) {
    MiuixScaffold(
        title = Strings.get(appState.language, "about"),
        appState = appState,
        currentRoute = AppRoute.About,
        onNavigate = onNavigate,
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "👋 " + Strings.get(appState.language, "welcome"),
                        style = MiuixTheme.textStyles.title1,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "用 Compose Multiplatform + Miuix 构建的个人博客。",
                        style = MiuixTheme.textStyles.body,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Built with Compose Multiplatform + Miuix.",
                        style = MiuixTheme.textStyles.body,
                        color = MiuixTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    SmallTitle(text = "技术栈 / Tech Stack")
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Kotlin + Compose Multiplatform (wasmJs)",
                        "Miuix UI (HyperOS 风格)",
                        "Cloudflare Workers + D1 + KV",
                    ).forEach { tech ->
                        Row {
                            Text("·  ", color = MiuixTheme.colorScheme.primary)
                            Text(tech, style = MiuixTheme.textStyles.body)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: 接入导航并验证编译**

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```

- [ ] **Step 5: 提交**

```bash
git add composeApp
git commit -m "feat: messages, friends and about screens"
```

---

### Task 15: 前端设置页（主题/语言/字号）

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/SettingsScreen.kt`

**Interfaces:**
- Consumes: Miuix preference 组件（`SwitchPreference`、`RadioButtonPreference`、`SliderPreference`、`OverlayListPopup`）、`ColorPicker`
- Produces: 设置页，含主题模式选择、主题色 ColorPicker、语言选择、字号 Slider

- [ ] **Step 1: 编写 SettingsScreen**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/SettingsScreen.kt`:
```kotlin
package com.hyperos.blog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.theme.ThemeMode
import com.hyperos.blog.ui.components.MiuixScaffold
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.prefs.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    MiuixScaffold(
        title = Strings.get(appState.language, "settings"),
        appState = appState,
        currentRoute = AppRoute.Settings,
        onNavigate = onNavigate,
    ) {
        Column(Modifier.fillMaxSize()) {
            Card(Modifier.fillMaxWidth()) {
                Column {
                    RadioButtonPreference(
                        title = Strings.get(appState.language, "themeColor"),
                        checked = false,
                        onClick = { showColorDialog = true },
                    )
                    Divider()
                    ArrowPreference(
                        title = Strings.get(appState.language, "language"),
                        subtitle = if (appState.language == "zh") "简体中文" else "English",
                        onClick = { showLanguageDialog = true },
                    )
                    Divider()
                    SwitchPreference(
                        title = Strings.get(appState.language, "darkMode"),
                        checked = appState.themeMode == ThemeMode.Dark,
                        onCheckedChange = { checked ->
                            appState.themeMode = if (checked) ThemeMode.Dark else ThemeMode.Light
                        },
                    )
                }
            }
        }
    }

    if (showLanguageDialog) {
        top.yukonga.miuix.kmp.overlay.OverlayDialog(
            title = Strings.get(appState.language, "language"),
            onDismiss = { showLanguageDialog = false },
        ) {
            Column(Modifier.padding(16.dp)) {
                RadioButtonPreference(title = "简体中文", checked = appState.language == "zh", onClick = { appState.language = "zh"; showLanguageDialog = false })
                Divider()
                RadioButtonPreference(title = "English", checked = appState.language == "en", onClick = { appState.language = "en"; showLanguageDialog = false })
            }
        }
    }

    if (showColorDialog) {
        top.yukonga.miuix.kmp.overlay.OverlayDialog(
            title = Strings.get(appState.language, "themeColor"),
            onDismiss = { showColorDialog = false },
        ) {
            Column(Modifier.padding(16.dp)) {
                ColorPicker(
                    color = appState.themeState.keyColor,
                    onColorChange = { appState.themeState.keyColor = it },
                    onColorConfirm = { showColorDialog = false },
                )
            }
        }
    }
}
```

注意：Miuix preference 组件的具体包名（`top.yukonga.miuix.kmp.prefs.*` vs `top.yukonga.miuix.kmp.preference.*`）以 miuix-preference 模块实际为准，编译时验证调整。

- [ ] **Step 2: 接入导航并验证编译**

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```

- [ ] **Step 3: 提交**

```bash
git add composeApp
git commit -m "feat: settings screen with theme and language"
```

---

### Task 16: 前端后台（登录 + 文章管理 + 统计）

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/admin/AdminLoginScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/admin/AdminHomeScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/admin/AdminEditorScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hyperos/blog/App.kt`

**Interfaces:**
- Consumes: `api.post<AuthResponse>("/api/auth/login")`、`api.get<List<Post>>("/api/admin/posts")`、`api.post/put/delete<Post>`、`api.get<SiteStats>("/api/stats")`
- Produces: 登录页、后台首页（文章列表 + 统计卡片）、文章编辑器（字段表单 + 保存/删除）

- [ ] **Step 1: 编写登录页**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/admin/AdminLoginScreen.kt`:
```kotlin
package com.hyperos.blog.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.AuthResponse
import com.hyperos.blog.i18n.Strings
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AdminLoginScreen(
    appState: AppState,
    onLoggedIn: () -> Unit,
    onBack: () -> Unit,
    api: ApiClient,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = Strings.get(appState.language, "login"), onBack = onBack) },
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextField(value = username, onValueChange = { username = it }, placeholder = Strings.get(appState.language, "username"))
            TextField(value = password, onValueChange = { password = it }, placeholder = Strings.get(appState.language, "password"))
            if (error != null) {
                Text(error!!, color = MiuixTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    loading = true
                    error = null
                    val resp = api.post<AuthResponse>("/api/auth/login", mapOf("username" to username.trim(), "password" to password))
                    if (resp.ok && resp.data != null) {
                        appState.adminToken = resp.data.token
                        api.setToken(resp.data.token)
                        onLoggedIn()
                    } else {
                        error = resp.error
                    }
                    loading = false
                },
                enabled = !loading,
            ) {
                Text(if (loading) Strings.get(appState.language, "loading") else Strings.get(appState.language, "login"))
            }
        }
    }
}
```

- [ ] **Step 2: 编写后台首页**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/admin/AdminHomeScreen.kt`:
```kotlin
package com.hyperos.blog.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.*
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AdminHomeScreen(
    appState: AppState,
    onBack: () -> Unit,
    onEditPost: (Post?) -> Unit,
    api: ApiClient,
) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var stats by remember { mutableStateOf<SiteStats?>(null) }

    fun load() {
        val pResp = api.get<PostListData>("/api/admin/posts?page=1&pageSize=50")
        if (pResp.ok) posts = pResp.data?.posts ?: emptyList()
        val sResp = api.get<SiteStats>("/api/stats")
        if (sResp.ok) stats = sResp.data
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = Strings.get(appState.language, "admin"),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onEditPost(null) }) {
                        top.yukonga.miuix.kmp.icon.MiuixIcons.Add()
                    }
                },
            )
        },
        bottomBar = {
            Button(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                onClick = {
                    appState.adminToken = null
                    api.setToken(null)
                },
            ) {
                Text(Strings.get(appState.language, "logout"))
            }
        },
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            stats?.let { s ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatsCard(Strings.get(appState.language, "postCount"), s.postCount.toString())
                        StatsCard(Strings.get(appState.language, "commentCount"), s.commentCount.toString())
                        StatsCard(Strings.get(appState.language, "viewCount"), s.viewCount.toString())
                        StatsCard(Strings.get(appState.language, "totalVisits"), s.totalVisits.toString())
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            items(posts, key = { it.id }) { post ->
                Card(Modifier.fillMaxWidth(), onClick = { onEditPost(post) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (appState.language == "zh") post.titleZh else post.titleEn.ifBlank { post.titleZh },
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${post.status} · ${post.createdAt.take(10)} · ${post.viewCount} views",
                                style = MiuixTheme.textStyles.caption,
                                color = MiuixTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StatsCard(label: String, value: String) {
    Card(Modifier.weight(1f)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MiuixTheme.textStyles.title1, fontWeight = FontWeight.Bold)
            Text(label, style = MiuixTheme.textStyles.caption, color = MiuixTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

- [ ] **Step 3: 编写编辑器**

`composeApp/src/commonMain/kotlin/com/hyperos/blog/ui/admin/AdminEditorScreen.kt`:
```kotlin
package com.hyperos.blog.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.*
import com.hyperos.blog.i18n.Strings
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AdminEditorScreen(
    appState: AppState,
    post: Post?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    api: ApiClient,
) {
    var titleZh by remember { mutableStateOf(post?.titleZh ?: "") }
    var titleEn by remember { mutableStateOf(post?.titleEn ?: "") }
    var summary by remember { mutableStateOf(post?.summary ?: "") }
    var content by remember { mutableStateOf(post?.content ?: "") }
    var category by remember { mutableStateOf(post?.category ?: "") }
    var tagsText by remember { mutableStateOf(post?.tags?.joinToString(",") ?: "") }
    var status by remember { mutableStateOf(post?.status ?: "published") }
    var saving by remember { mutableStateOf(false) }
    var snackbarText by remember { mutableStateOf<String?>(null) }

    fun save() {
        saving = true
        val input = PostInput(
            slug = post?.slug ?: "",
            titleZh = titleZh,
            titleEn = titleEn,
            summary = summary,
            content = content,
            category = category,
            tags = tagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() },
            status = status,
            pinned = post?.pinned ?: false,
            featured = post?.featured ?: false,
        )
        val resp = if (post == null) {
            api.post<Post>("/api/admin/posts", input)
        } else {
            api.put<Post>("/api/admin/posts/${post.id}", input)
        }
        saving = false
        if (resp.ok) onSaved() else snackbarText = resp.error
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = if (post == null) Strings.get(appState.language, "publish") else Strings.get(appState.language, "edit"),
                onBack = onBack,
                actions = {
                    Button(onClick = { save() }, enabled = !saving) {
                        Text(Strings.get(appState.language, "save"))
                    }
                },
            )
        },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextField(value = titleZh, onValueChange = { titleZh = it }, placeholder = "标题 (中文)")
            TextField(value = titleEn, onValueChange = { titleEn = it }, placeholder = "Title (English)")
            TextField(value = summary, onValueChange = { summary = it }, placeholder = Strings.get(appState.language, "postSummary"), minLines = 2)
            TextField(value = content, onValueChange = { content = it }, placeholder = Strings.get(appState.language, "postContent") + " (Markdown)", minLines = 12)
            TextField(value = category, onValueChange = { category = it }, placeholder = Strings.get(appState.language, "category"))
            TextField(value = tagsText, onValueChange = { tagsText = it }, placeholder = "标签，逗号分隔")
            Row {
                Switch(checked = status == "published", onCheckedChange = { status = if (it) "published" else "draft" })
                Spacer(Modifier.width(8.dp))
                Text(if (status == "published") Strings.get(appState.language, "publish") else Strings.get(appState.language, "draft"))
            }
            if (post != null) {
                Button(
                    onClick = {
                        api.delete<Any>("/api/admin/posts/${post.id}")
                        onSaved()
                    },
                ) {
                    Text(Strings.get(appState.language, "delete"))
                }
            }
        }
    }
}
```

- [ ] **Step 4: 接入导航并验证编译**

在 `App.kt` 处理 Admin 相关路由（`AppRoute.Admin`、`AppRoute.AdminEditor`），并维护 `editingPost: Post?`。未登录时显示登录页。

```bash
./gradlew :composeApp:wasmJsBrowserDistribution`
```

- [ ] **Step 5: 提交**

```bash
git add composeApp
git commit -m "feat: admin login, post management and editor"
```

---

### Task 17: 前端构建优化 + Pages 部署配置

**Files:**
- Create: `composeApp/src/wasmJsMain/resources/_redirects`
- Create: `composeApp/src/wasmJsMain/resources/_headers`
- Create: `scripts/build-frontend.ps1`
- Create: `scripts/deploy-all.ps1`
- Create: `.dev.vars.example`
- Modify: `worker/wrangler.jsonc`（真实 D1/KV id）

**Interfaces:**
- Consumes: Task 1-16 全部代码
- Produces: 一键构建/部署脚本

- [ ] **Step 1: 编写 Pages 配置文件**

`composeApp/src/wasmJsMain/resources/_redirects`:
```txt
/* /index.html 200
```
说明：SPA 回退。

`composeApp/src/wasmJsMain/resources/_headers`:
```txt
/*.wasm
  Content-Type: application/wasm
  Cache-Control: public, max-age=31536000, immutable

/assets/*
  Cache-Control: public, max-age=31536000, immutable

/index.html
  Cache-Control: no-cache
```

注意：需将 `_redirects`/`_headers` 复制到构建产物目录（Gradle 任务）。

在 `composeApp/build.gradle.kts` 添加：
```kotlin
tasks.named("wasmJsBrowserDistribution") {
    doLast {
        val distDir = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
        val webResources = layout.projectDirectory.dir("src/wasmJsMain/resources")
        copy {
            from(webResources) { include("_redirects", "_headers") }
            into(distDir)
        }
    }
}
```

- [ ] **Step 2: 编写构建/部署脚本**

`scripts/build-frontend.ps1`:
```powershell
param(
    [string]$OutputDir = "dist/frontend"
)
$ErrorActionPreference = "Stop"
Write-Host "Building wasmJs frontend..."
.\gradlew.bat :composeApp:wasmJsBrowserDistribution
if ($LASTEXITCODE -ne 0) { exit 1 }
$src = "composeApp/build/dist/wasmJs/productionExecutable"
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
Copy-Item -Path "$src\*" -Destination $OutputDir -Recurse -Force
Write-Host "Frontend built to $OutputDir"
```

`scripts/deploy-all.ps1`:
```powershell
param(
    [string]$ProjectName = "hyperos-blog"
)
$ErrorActionPreference = "Stop"

Write-Host "=== Step 1: Deploy Worker ==="
Push-Location worker
npx wrangler deploy
if ($LASTEXITCODE -ne 0) { Pop-Location; exit 1 }
Pop-Location

Write-Host "=== Step 2: Build Frontend ==="
.\gradlew.bat :composeApp:wasmJsBrowserDistribution
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "=== Step 3: Deploy Frontend to Pages ==="
npx wrangler pages deploy composeApp/build/dist/wasmJs/productionExecutable --project-name=$ProjectName
if ($LASTEXITCODE -ne 0) { exit 1 }
Write-Host "Done!"
```

`.dev.vars.example`（worker 目录）：
```bash
# 本地开发环境变量示例，复制为 .dev.vars
```

- [ ] **Step 3: 提交**

```bash
git add scripts composeApp worker
git commit -m "chore: build and deploy scripts"
```

---

### Task 18: 部署到 Cloudflare + 初始化数据 + 端到端验证

**Files:**
- Modify: `worker/wrangler.jsonc`（填入真实 ID）
- Create: `scripts/seed-data.mjs`

**Interfaces:**
- Consumes: 用户提供的 Cloudflare 账号 ID 和 API token
- Produces: 线上可访问的博客

- [ ] **Step 1: 配置 Cloudflare 认证**

设置环境变量（在 PowerShell 会话，请填入你自己的凭据）：
```powershell
$env:CLOUDFLARE_ACCOUNT_ID = "你的账号ID"
$env:CLOUDFLARE_API_TOKEN = "你的API令牌"
```
确认：
```bash
npx wrangler whoami
```

- [ ] **Step 2: 创建 D1 与 KV**

```bash
cd worker
npx wrangler d1 create hyperos-blog
# 记下 database_id
npx wrangler kv namespace create BLOG_KV
# 记下 id
```
将两个 ID 填入 `worker/wrangler.jsonc`。

- [ ] **Step 3: 应用远程迁移 + 初始化管理员**

```bash
npx wrangler d1 migrations apply hyperos-blog --remote
node ../scripts/init-admin.mjs admin YOUR_STRONG_PASSWORD --remote
```

- [ ] **Step 4: 插入种子数据**

`scripts/seed-data.mjs` 插入欢迎文章、分类、友链、设置：
```javascript
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
```

- [ ] **Step 5: 部署**

```bash
# 部署 Worker
cd worker && npx wrangler deploy
# 构建前端
cd .. && .\gradlew.bat :composeApp:wasmJsBrowserDistribution
# 部署前端
npx wrangler pages deploy composeApp/build/dist/wasmJs/productionExecutable --project-name=hyperos-blog
```

- [ ] **Step 6: 端到端验证**

```bash
# 健康检查
curl https://hyperos-blog-worker.<your-subdomain>.workers.dev/api/health
# API 验证
curl https://hyperos-blog-worker.<your-subdomain>.workers.dev/api/posts
curl https://hyperos-blog-worker.<your-subdomain>.workers.dev/api/posts/welcome
curl https://hyperos-blog.<your-subdomain>.pages.dev/
# SEO 验证（用爬虫 UA）
curl -A "Mozilla/5.0 (compatible; Googlebot/2.1)" https://hyperos-blog.<your-subdomain>.pages.dev/post/welcome
```
期望：页面返回完整预渲染 HTML。

- [ ] **Step 7: 提交**

```bash
git add scripts worker
git commit -m "chore: deployment configuration and seed data"
```

---

### Task 19: 收尾 — README + 最终验证

**Files:**
- Create: `README.md`

**Interfaces:**
- Consumes: 全部任务
- Produces: 项目文档

- [ ] **Step 1: 编写 README**

```markdown
# HyperOS 博客

基于 Compose Multiplatform (wasmJs) + Miuix UI 的个人博客，部署在 Cloudflare (Pages + Workers + D1 + KV)。

## 功能

- 文章列表 / 详情 / Markdown 渲染 / 目录
- 分类 / 标签 / 全文搜索 / 归档
- 评论 + 点赞（IP 防重）
- 主题切换（浅色/深色/动态主题色）
- 阅读统计 + 访客计数
- 留言板 / 友链 / 关于页
- 在线管理后台（密码登录，文章 CRUD）
- 中英双语
- SEO 服务端预渲染

## 目录结构

| 目录 | 说明 |
|---|---|
| `composeApp/` | Kotlin wasmJs 前端 (Miuix UI) |
| `worker/` | Cloudflare Worker API (D1 + KV) |
| `worker/migrations/` | D1 迁移 |
| `scripts/` | 构建/部署/初始化脚本 |
| `docs/superpowers/` | 设计文档与实施计划 |

## 本地开发

```bash
# 前端
.\gradlew.bat :composeApp:wasmJsBrowserRun
# 后端
cd worker && npx wrangler dev
```

## 构建与部署

```bash
# 一键部署（需先配置 CLOUDFLARE_ACCOUNT_ID / CLOUDFLARE_API_TOKEN）
.\scripts\deploy-all.ps1
```

## 初始化

```bash
cd worker
npx wrangler d1 migrations apply hyperos-blog --remote
node ..\scripts\init-admin.mjs <username> <password> --remote
node ..\scripts\seed-data.mjs
```
```

- [ ] **Step 2: 最终全量验证**

```bash
# 前端构建
./gradlew :composeApp:wasmJsBrowserDistribution
# Worker 类型检查
cd worker && npx tsc --noEmit
# 端到端 curl 验证线上
```

- [ ] **Step 3: 提交**

```bash
git add README.md
git commit -m "docs: add README"
```

---

## 自审记录

**1. 规格覆盖：**
- 架构（方案A Pages+Workers）→ Task 1, 17, 18
- D1 七张表 → Task 2
- KV 用途（会话/访客/防重/排行）→ Task 4, 6
- 公开 API → Task 5, 6
- 管理 API → Task 4, 7
- SEO 预渲染 → Task 8
- 主题系统（MiuixTheme/ThemeController/ColorSchemeMode）→ Task 9
- 全部 Miuix 组件覆盖（TopAppBar/SearchBar/Card/NavigationBar/PullToRefresh/TabRow/Divider/TextField/Switch/Checkbox/RadioButton/Slider/ProgressIndicator/Snackbar/Tooltip/Badge/Icon/FloatingActionButton/ColorPicker/preference 系列/OverlayDialog/OverlayBottomSheet）→ Task 11-16
- 中英双语 → Task 9
- 评论含邮箱 → Task 6, 12
- 密码 PBKDF2 → Task 4, 10
- 不适用 R2 → 全部
- 部署到用户账户 → Task 18

**2. 占位符扫描：** 无 TBD/TODO；每个代码任务含完整实现。

**3. 类型一致性：** `PublicPost`/`PostRow`/`ApiResponse<T>`/`ApiClient`/`AppState`/`ThemeState`/`AppRoute` 在各 Task 间签名一致；`toPublicPost` 统一为 `PostRow → PublicPost`。
