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

## 在线地址

- 前端: https://hyperos-blog.pages.dev
- API Worker: https://hyperos-blog-worker.tianlu4-5.workers.dev

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

注意：Windows 上 `wrangler dev` 本地模式可能因 workerd 崩溃不可用，请直接用远程部署验证。

## 构建与部署

```bash
# 一键部署（需先配置 CLOUDFLARE_ACCOUNT_ID / CLOUDFLARE_API_TOKEN）
.\scripts\deploy-all.ps1
```

## 初始化

```bash
cd worker
npx wrangler d1 migrations apply hyperos-blog --remote
node ..\scripts\init-admin.mjs <username> <password> <database-id> --remote
node ..\scripts\seed-data.mjs
```