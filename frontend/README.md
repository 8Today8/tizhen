# 题阵前端

Vue 3 + TypeScript + Vite 单页应用，用户端与运营后台共用同一 SPA，按路由树拆分：

- `/`：用户端（`CustomerLayout`）
- `/login`、`/admin/login`：登录页（`AuthLayout`）
- `/admin`：运营后台（`AdminLayout`）

当前为**工程骨架**：运营后台已完成壳层与各管理页面的布局骨架（数据留空），用户端页面仍为占位实现；账号、题库、判题、榜单、对战、造数据等业务接口尚未接入。

## 常用命令

```bash
npm install
npm run dev          # http://127.0.0.1:5173
npm run type-check
npm run build
npm run test:unit
npm run test:e2e     # 首次运行前执行 npx playwright install chromium
```

开发服务器将 `/api` 与 `/ws` 代理到 `http://127.0.0.1:8080`（见 `vite.config.ts`）。环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `/api/v1` | API 基地址 |
| `VITE_WS_BASE_URL` | `ws://127.0.0.1:8080/ws` | WebSocket 地址 |

## 目录结构

```text
public/           # favicon 等静态资源
src/
  api/            # 请求封装与各模块端点（当前为占位文件）
  components/     # base / problem / battle / datagen / leaderboard / admin
  composables/    # useTheme、usePendingFeature；其余组合式函数随模块补充
  layouts/        # CustomerLayout、AdminLayout、AuthLayout
  router/         # 路由表与导航守卫
  stores/         # Pinia：auth、theme
  styles/         # 设计令牌、基础样式、后台布局样式、明亮/暗淡主题
  types/          # API 信封与分页类型
  utils/          # Cookie、时间格式化、localStorage
  views/          # auth / home / learning / problems / leaderboard / battle /
                  # datagen / records / profile / xcpc / admin / error
tests/
  unit/           # Vitest + Vue Test Utils
  e2e/            # Playwright
```

## 请求与认证约定

- 统一响应信封：`{ success, code, message, data, fieldErrors, requestId, timestamp }`，由 `src/api/http.ts` 的 `unwrap` 校验并解包。
- 失败统一抛出 `ApiError`，携带稳定错误码与 `fieldErrors`。
- 所有写请求自动附加 `X-XSRF-TOKEN`：用户端读取 `XSRF-TOKEN`，`/admin` 请求读取 `ADMIN-XSRF-TOKEN`。
- `withCredentials` 已开启，会话 Cookie 由后端 `HttpOnly` 管理。
- 路由守卫只负责导航体验，真正的身份认证和接口权限校验由后端执行。

## 主题

`src/stores/theme.ts` 维护 `light` / `dusk` 两种模式并写入 `localStorage`（键 `tizhen.theme`），页面通过 `src/composables/useTheme.ts` 读取与切换。主题同步 `html[data-theme]` 与 `html.dark`；Element Plus 组件变量在 `src/styles/tokens.css` 中统一覆盖，主题装饰令牌位于 `src/styles/themes/`。

## 后续接入顺序

1. M01 账号：登录/注册/会话恢复、CSRF 引导、路由守卫接入。
2. M03 学习内容、M04 题库与判题。
3. M05 榜单、M06 学习记录与错题本、M07 对战、M08 AI 造数据。
4. M09 运营后台各管理页。
