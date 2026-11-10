# 题阵后端

Java 21 + Spring Boot 3.5 + MyBatis-Plus + MySQL 8.4 + Redis 7 + Spring Security/Spring Session + Flyway + MapStruct。

## 模块

| 模块 | 职责 |
|---|---|
| `tizhen-common` | 统一响应、错误码、全局异常、请求 ID、通用工具 |
| `tizhen-core` | M01–M09 业务模块与基础设施适配（Flyway 迁移也在此） |
| `tizhen-app` | 对外 Web 进程：REST、WebSocket、会话与安全 |
| `tizhen-worker` | 异步任务进程：判题、造数据、CF 同步、清理 |
| `tizhen-test-support` | Testcontainers 集成测试基类与架构约束依赖 |

包结构遵循 `docs/design/02.整体架构设计.md`：

- 业务模块：`account`、`external`、`learning`、`problem`、`judge`、`leaderboard`、`record`、`battle`、`datagen`、`admin`
- 模块内部建议分层：`application` / `domain` / `infrastructure` / `query`
- 跨模块依赖只允许调用对方应用服务或查询端口，禁止直接访问 Mapper/Entity

## 本地启动

1. 启动基础设施（项目根目录）：

```bash
bash scripts/dev-up.sh
```

2. 导出开发环境变量（`deploy/.env` 已被 git 忽略）：

```bash
set -a
source deploy/.env
set +a
```

3. 构建并安装模块：

```bash
cd backend
mvn clean install -DskipTests
```

4. 启动应用：

```bash
mvn -pl tizhen-app spring-boot:run
```

异步任务进程单独启动：

```bash
mvn -pl tizhen-worker spring-boot:run
```

访问：

- 健康检查：`http://localhost:8080/actuator/health`
- 接口文档：`http://localhost:8080/swagger-ui.html`

启动时 Flyway 会执行 `tizhen-core/src/main/resources/db/migration` 下的迁移。

## 数据库迁移约定

- 唯一建表来源是 Flyway；`V1__init_schema.sql` 已包含 24 张表与基础角色。
- **禁止修改已执行的迁移文件**，结构变更新增 `V2__xxx.sql`。
- `scripts/sql/schema.sql` 是 V1 的设计快照，只用于文档对照。

## 安全基线

- 会话存储使用 Redis（Spring Session），选手 Cookie 名称 `TIZHEN_SID`。
- CSRF 使用 `XSRF-TOKEN` Cookie + `X-XSRF-TOKEN` 请求头。
- `/api/**` 默认要求认证；账号模块接入后拆分选手/后台两条会话链路。
- 所有错误统一返回 `{ success, code, message, data, fieldErrors, requestId, timestamp }`。

## 常用配置

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_HOST` | `127.0.0.1` | MySQL 主机 |
| `MYSQL_PORT` | `3306` | MySQL 端口 |
| `MYSQL_DATABASE` | `tizhen` | 数据库名 |
| `MYSQL_USER` | `tizhen` | 应用账号 |
| `MYSQL_PASSWORD` | 无 | 必填 |
| `REDIS_HOST` | `127.0.0.1` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | 空 | Redis 密码 |
| `DATA_STORAGE_DIR` | `./data/storage` | 造数据文件根目录 |
