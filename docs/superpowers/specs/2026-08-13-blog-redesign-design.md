# 设计文档：博客改造（首页信息卡、管理面板、导航圆角）

日期：2026-08-13

## 背景

HyperOS 风格博客（Compose Multiplatform wasmJs + Miuix 0.9.3 + Cloudflare Workers/D1/KV）需要以下改造：

1. 侧边悬浮导航 hover 展开项形状为方形，需改为圆角胶囊样式
2. 首页改为博主信息卡（头像/名字/简介），文章列表移到归档页
3. 管理登录只输入密码，去掉用户名
4. 管理面板扩展：可修改网页名称、网页图标(favicon)、头像、简介；评论管理；写博客
5. 图片使用外置图床，直接粘贴外链 URL
6. 网页名称可动态修改（document.title + favicon）

## 模块设计

### 模块 1：侧边导航圆角修复

**文件**：`ui/components/MiuixScaffold.kt`

- `SideNavItemRow` hover/选中项统一使用圆角胶囊背景（`squircleBackground` 圆角与页面卡片一致，如 20-22dp）
- 展开时项背景为圆角胶囊，非方形
- 侧边栏容器本身已用 `squircleBackground`（圆角 26-28dp），保持不变

### 模块 2：首页博主信息卡

**文件**：`ui/HomeScreen.kt`

- 移除文章列表 LazyColumn 与分类 TabRow
- 改为居中博主信息卡：
  - 圆形头像（外链 URL，用远程图片加载）
  - 名字（settings `title` 或独立 `name` 字段）
  - 自我介绍（settings `bio`）
  - 站点统计：文章数/评论数/访问量（GET `/api/stats`）
  - 风格参考 miuix-wasmJs demo 的居中卡片布局（`Column.widthIn(max=600.dp)` 居中）
- 移除 PullToRefresh

### 模块 3：管理登录简化

**文件**：`ui/admin/AdminLoginScreen.kt`

- 移除用户名 TextField，只保留密码框
- 登录请求 `/api/auth/login` 仍传固定用户名 `admin` + 密码
- 登录成功跳转管理面板

### 模块 4：管理面板扩展

**文件**：`ui/admin/AdminHomeScreen.kt`（新增入口）、`ui/admin/SiteSettingsScreen.kt`（新增）

- AdminHomeScreen 顶部增加入口行：`站点设置`、`评论管理`、`写博客`（已有 Add 按钮）
- 新增 `SiteSettingsScreen`：
  - 网页名称（title）
  - 网页图标 URL（favicon）
  - 头像 URL（avatar）
  - 简介（bio）
  - 保存 → `PUT /api/settings`
  - 应用 → 更新 `appState.siteTitle`、`document.title`、favicon `<link>`
- 新增评论管理入口 → 评论列表（`GET /api/admin/comments`）+ 删除（`DELETE /api/comments/:id`）

**导航路由**：`App.kt` 增加 `AdminSiteSettings`、`AdminComments` 两个 route

### 模块 5：前端远程图片加载

**文件**：`composeApp/build.gradle.kts`、`gradle/libs.versions.toml`

- 引入图片加载库：`coil-compose`（支持 wasm）或 `kamel`。优先 Kamel（Compose MP 原生支持）
- 用于头像、favicon 预览等远程 URL 图片

### 模块 6：后端接口补充

**文件**：`worker/src/routes/admin.ts`、`worker/src/index.ts`

- 新增 `GET /api/admin/comments`：分页返回全部评论（含所属文章），管理端鉴权
- 评论删除接口已有：`DELETE /api/comments/:id`（管理端）
- settings 已支持 GET/PUT，无需改动
- favicon 通过前端动态 JS 设置，无需后端存储文件

## 数据流

- 站点配置（name/avatar/bio/favicon/title）存 settings 表（key-value），`GET/PUT /api/settings`
- 前端启动时 `GET /api/settings` → 填充 `AppState` → 首页信息卡、标题栏、favicon
- favicon：`document.getElementById('favicon')` 不存在则创建 `<link rel="icon">`，动态设 href

## 错误处理

- 登录失败：显示错误提示，不跳转
- settings 保存失败：提示错误
- 图片加载失败：显示占位（圆形灰块/首字母）

## 测试

- 编译验证：`gradlew.bat :composeApp:compileKotlinWasmJs`
- 完整构建：`gradlew.bat :composeApp:wasmJsBrowserDistribution --no-daemon`
- 部署验证：wrangler pages deploy，浏览器验证首页/导航/管理面板

## 备注

- 图床使用外链 URL，无上传集成
- 凭据：admin 密码 `AaFvdD@i!bV%xN@G`（建议轮换）