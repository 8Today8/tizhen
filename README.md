# 题阵

算法训练与对战平台：学习路线、题库与判题、榜单、1v1 对战、AI 造数据与运营后台。

当前仓库处于**基础工程搭建完成、业务功能待开发**阶段。设计文档见 `docs/design/`，交互原型见 `prototype/`。

## 技术架构

| 层 | 技术 |
|---|---|
| 后端 | Java 21、Spring Boot 3.5、MyBatis-Plus、MySQL 8.4、Redis 7、Spring Security + Spring Session、Flyway、MapStruct |
| 前端 | Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios、Element Plus、CodeMirror 6 |
| 判题 | 独立 go-judge 沙箱服务（待接入） |
| 部署 | Docker Compose（本地开发依赖） |

后端为模块化单体，拆分为 `tizhen-common`、`tizhen-core`、`tizhen-app`、`tizhen-worker`、`tizhen-test-support`。前端为单一 SPA，用户端 `/` 与运营后台 `/admin` 按路由树拆分。

## 目录

```text
backend/       # Maven 多模块后端
frontend/      # Vue 3 单页应用
judge-runner/  # go-judge 配置与语言环境（骨架，待接入）
deploy/        # 本地开发基础设施与部署说明
docs/          # 需求、设计与开发规范
prototype/     # 页面原型参考
scripts/       # 开发环境脚本
```

## 本地启动

### 1. 启动 MySQL 与 Redis

```bash
bash scripts/dev-up.sh
```

首次使用需先复制 `deploy/.env.example` 为 `deploy/.env` 并填写密码。表结构由后端启动时的 Flyway 自动创建，无需手工执行 SQL。

### 2. 启动后端

```bash
set -a
source deploy/.env
set +a

cd backend
mvn clean install -DskipTests
mvn -pl tizhen-app spring-boot:run
```

- 健康检查：http://127.0.0.1:8080/actuator/health
- 接口文档：http://127.0.0.1:8080/swagger-ui.html

> 在 Windows 上请使用 Git Bash 执行 `source`。若使用 PowerShell，请逐项设置 `MYSQL_PASSWORD`、`REDIS_PASSWORD` 等环境变量。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://127.0.0.1:5173 。Vite 已将 `/api` 与 `/ws` 代理到 `127.0.0.1:8080`。

## 验证

```bash
# 后端编译与测试
cd backend && mvn clean verify

# 前端类型检查、单元测试、构建
cd frontend
npm run type-check
npm run test:unit
npm run build
```

## 当前完成范围

- 后端多模块骨架、统一响应信封、稳定错误码、全局异常处理、请求 ID。
- MyBatis-Plus 分页配置、Flyway V1（24 张表与基础角色）、本地文件存储抽象。
- Spring Security 安全基线：`/api/**` 默认要求认证，统一 401/403 JSON，Redis 会话与 CSRF Cookie 配置。
- 前端路由、三种布局、主题令牌（明亮 / 暗淡）、Axios 请求封装与 CSRF 头注入。
- 运营后台壳层、菜单分组、面包屑与全部管理页面的布局骨架（含四类题库子页面，数据留空）。
- 账号、题库、判题、榜单、对战、造数据、运营后台等业务接口尚未实现。

## 约定

- 数据库结构变更只新增 Flyway 迁移，不修改 `V1__init_schema.sql`；`scripts/sql/schema.sql` 为设计快照。
- API 响应、错误码、分页与会话约定见 `docs/design/04.API接口设计.md` 与 `docs/rules/api-rules.md`。
- 数据模型与约束见 `docs/design/03.数据库设计.md` 与 `docs/rules/database-rules.md`。
