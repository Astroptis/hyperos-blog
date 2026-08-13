# 设计文档：域名绑定与双 API 备份

日期：2026-08-13

## 背景

- 域名：`astroptis.dpdns.org`（网站根域名）、`api.astroptis.dpdns.org`（API 子域名）
- 新建了 DNS 编辑令牌（账号内已有 `astroptis.dpdns.org` 域名）
- 目标：Pages 绑定根域名，Worker 绑定子域名，前端做两个 API 地址备份

## 需求确认

1. **API 双备份**：优先调 `api.astroptis.dpdns.org`，请求失败（网络错误/5xx/超时）自动回退到同源 `/api` 重试
2. **域名绑定**：`astroptis.dpdns.org` 绑 Pages 网站，`api.astroptis.dpdns.org` 绑 Worker
3. **Worker 部署**：绑定子域名 + 保留 workers.dev

## 模块设计

### 模块 1：前端双 API 备份

**文件**：`data/ApiClient.kt`、`data/ApiClientFactory.kt`（common + wasmJs）

- `ApiClient` 构造函数改为接收**主 baseUrl 列表**：
  ```kotlin
  class ApiClient(
      private val baseUrls: List<String>,  // [api.astroptis.dpdns.org, 同源 /api]
      private val engine: HttpClientEngine,
  )
  ```
- `request()` 逻辑改为：遍历 baseUrls，逐个尝试；成功返回；失败（异常/5xx）记录后尝试下一个 baseUrl
- 注意：主 API 为跨域请求，需要确认 Worker 已配置 CORS 允许 Pages 域名
- `ApiClientFactory (wasmJs)`：
  ```kotlin
  val origin = window.location.origin
  val baseUrls = listOf(
      "https://api.astroptis.dpdns.org",   // 主：子域名 Worker
      origin,                               // 备用：同源 Pages functions 代理
  )
  return ApiClient(baseUrls, Js.create())
  ```

### 模块 2：Worker 绑定子域名

**文件**：`worker/wrangler.jsonc`

- 添加 `routes`：
  ```jsonc
  "routes": [
    { "pattern": "api.astroptis.dpdns.org", "custom_domain": true }
  ]
  ```
- 保留 `workers_dev: true`
- 需要配置 DNS：`api` 子域名 CNAME → Worker（用新建的 DNS 令牌）
- Worker 需配置 CORS 允许 `astroptis.dpdns.org` 及 pages.dev 域名

### 模块 3：Pages 绑定根域名

- 通过 `npx wrangler pages deployment` 或 Cloudflare 控制台绑定 `astroptis.dpdns.org`
- DNS：`astroptis.dpdns.org` CNAME → Pages 项目域名
- Pages functions 继续提供同源 `/api` 代理（保留作回退）

### 模块 4：CORS 配置

**文件**：`worker/src/`（index.ts 或 cors 处理）

- 检查现有 CORS：允许 `astroptis.dpdns.org`、`https://*.pages.dev`
- 确保 `api.astroptis.dpdns.org` 的响应带正确 `Access-Control-Allow-Origin`

## 数据流

```
前端（astroptis.dpdns.org）
  ├── 优先 → https://api.astroptis.dpdns.org （Worker 直接）
  └── 回退 → 同源 /api （Pages functions 代理到 Worker）
```

## 错误处理

- 请求失败自动切换 baseUrl 重试
- 两个都失败才返回错误

## 测试

- 构建 + 部署 Worker（绑定子域名）
- 部署 Pages（绑定根域名）
- 浏览器验证：正常访问、断掉子域名验证回退
- 检查 CORS 头

## 备注

- 使用新建的 DNS 编辑令牌配置域名解析（wrangler 会读取 CLOUDFLARE_API_TOKEN）
- 需要确认 dpdns.org 域名的 DNS 记录类型（CNAME 或 A 记录）