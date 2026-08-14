# 部署完成记录（2026-08-13）

## 2026-08-14 二次更新：根治 hover 卡死 + 输入框失灵
- 根因：侧栏的 `pointerInput { while(true) { awaitPointerEvent() } }` 无限循环在 wasm 上会阻塞 Compose
  指针事件系统，导致①侧栏 hover 状态卡住②`/settings` 输入框无法点击聚焦输入
- 修复：彻底移除该 `pointerInput`，改用 DOM 全局 `mousemove` 监听（expect/actual `PointerTracker`）
  + `onGloballyPositioned`/`boundsInWindow` 判断鼠标是否在侧栏矩形内，完全绕开 Compose 命中测试
- 最新部署：https://bb9c0dc6.hyperos-blog.pages.dev（production=hyperos-blog.pages.dev）
- Git：feature/hyperos-blog 与 main 均推送至 6b0a073

## 2026-08-14 更新：修复 + 重部署
- 修复侧栏 hover 展开失效：改用 `pointerInput` + 指针位置检测（原 `hoverable/collectIsHoveredAsState` 在 wasm 不可靠）
- `/settings` 页面新增密码保护：未登录只显示密码框，验对后进入设置页
- 修复导航后 URL 变但页面不切换：`navigate()` 先 `UrlRouter.push` 再用 `scope.launch` 更新 `routeResult`
- 部署陷阱记录：Pages 构建 `_worker.js` 必须在干净的 dist（无已有 `_worker.js`）内执行
  `wrangler pages functions build`，否则会走 advanced-mode 错误路径导致 proxy 失效
- 最新部署：https://a8b12342.hyperos-blog.pages.dev（production=hyperos-blog.pages.dev）
- Git：feature/hyperos-blog 与 main 均推送至 b979c94

## 已完成并验证

### 前端（Pages）
- 预览域名：https://69dfd7cf.hyperos-blog.pages.dev
- 正式域名：hyperos-blog.pages.dev

### URL 路由（全部独立页面，均验证 200）
| 路径 | 页面 |
|------|------|
| `/` | 首页（博主信息卡：头像/名字/简介/统计） |
| `/archive` | 归档（文章列表 + 分类筛选 + 分页） |
| `/messages` | 留言板 |
| `/friends` | 友链 |
| `/about` | 关于 |
| `/settings` | 设置 |
| `/search` | 搜索 |
| `/tags` | 标签 |
| `/post/:slug` | 文章详情 |
| `/admin` | 管理后台（未登录→密码登录页） |
| `/admin/editor` | 写/编辑文章 |
| `/admin/settings` | 站点设置（名称/图标/头像/简介） |
| `/admin/comments` | 评论管理 |

### 功能
- 侧边导航 hover 圆角胶囊（选中+hover 均圆角）
- 首页博主信息卡：头像（外链 URL）、名字、简介、统计
- 文章列表移至归档页
- 管理登录只输密码（后端固定 admin 用户名）
- 管理面板：站点设置（网页名称/favicon/头像/简介，保存后动态更新 document.title + favicon + 首页）、评论管理（列表+删除）、写博客
- 双 API 备份：优先 `https://api.astroptis.dpdns.org`，失败自动回退同源 `/api`
- 远程图片加载（Kamel-free，skia 解码，用于头像）

### Worker
- `https://api.astroptis.dpdns.org`（custom domain，已验证 health/settings/comments 正常）
- `https://hyperos-blog-worker.tianlu4-5.workers.dev`（保留）
- 新增 `GET /api/admin/comments`（管理鉴权，含文章标题）

### Git
- feature/hyperos-blog 与 main 均推送至 86fab0e
- 仓库：https://github.com/Astroptis/hyperos-blog

## 待办（需要 DNS 令牌）

### 根域名绑定
`astroptis.dpdns.org` 已绑定到 Pages 项目，状态 `pending`，原因是：

> CNAME record not set

需要添加 DNS 记录：
```
类型: CNAME
名称: astroptis.dpdns.org
内容: hyperos-blog.pages.dev
代理: 已代理 (Proxied)
```

方法（任选一）：
1. Cloudflare 控制台 → `astroptis.dpdns.org` zone → DNS → 添加记录
2. 提供 DNS 编辑令牌（`CLOUDFLARE_API_TOKEN`），执行：
   ```
   curl -X POST https://api.cloudflare.com/client/v4/zones/31d176eed28ad2efced46696934b1aa6/dns_records \
     -H "Authorization: Bearer <DNS_TOKEN>" -H "Content-Type: application/json" \
     -d '{"type":"CNAME","name":"astroptis.dpdns.org","content":"hyperos-blog.pages.dev","proxied":true,"ttl":1}'
   ```

添加后等待几分钟，`astroptis.dpdns.org` 即可访问网站。

### 凭据提醒
- admin 密码：`AaFvdD@i!bV%xN@G`（建议登录后到站点设置或后续轮换）
- 曾暴露的旧 Cloudflare token 建议轮换

## 重新部署命令
```
# Worker
cd worker && CLOUDFLARE_ACCOUNT_ID=da604eb91531a90d46f5b53d504b4c60 CLOUDFLARE_API_TOKEN=<token> npx wrangler deploy

# 前端（构建 + 部署）
.\gradlew.bat :composeApp:wasmJsBrowserDistribution
# 然后从 dist 目录构建 functions 并部署 pages（详见 scripts/deploy-all.ps1）
```