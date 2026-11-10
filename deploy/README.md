# 题阵本地开发基础设施

当前目录提供 **MySQL 8.4 LTS + Redis 7.4.x** 的一键开发环境，供后端本地启动、数据库初始化和会话/缓存联调使用。

## 文件说明

| 文件 | 用途 |
|---|---|
| `docker-compose.dev.yml` | MySQL、Redis 开发环境编排 |
| `.env.example` | 环境变量模板；复制为 `.env` 后使用 |
| `.env` | 本机开发密码，已被 `.gitignore` 忽略 |
| `redis/redis.conf` | Redis 持久化与运行配置 |

MySQL 的字符集与 UTC 参数直接写在 compose 的 `command` 中。原因是 Windows Docker Desktop 的 bind mount 常把配置文件识别为 world-writable，MySQL 8.4 会忽略这类配置文件。

## 启动

在项目根目录执行：

```bash
bash scripts/dev-up.sh
```

脚本会检查 Docker 引擎、加载 `deploy/.env`，启动 MySQL 和 Redis，并输出容器状态。

停止但保留数据：

```bash
bash scripts/dev-down.sh
```

查看日志：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.dev.yml logs -f
```

## 连接信息

| 服务 | 地址 | 数据库/账号 |
|---|---|---|
| MySQL | `127.0.0.1:3306` | 数据库 `tizhen`；应用账号 `tizhen`，密码见 `deploy/.env` |
| Redis | `127.0.0.1:6379` | 密码见 `deploy/.env` |

MySQL root 账号仅用于本地初始化和管理；后端应使用 `tizhen` 应用账号。

## 数据库初始化与迁移

数据库表结构由后端启动时的 **Flyway** 管理，迁移文件位于：

```text
backend/tizhen-core/src/main/resources/db/migration/
```

- `V1__init_schema.sql` 是首期基线，包含 24 张表与基础角色数据。
- `scripts/sql/schema.sql` 是同一基线的设计快照，只用于文档对照；**后续结构变更只新增 Flyway 迁移，不再修改该快照**。
- Docker 编排不再挂载 initdb 脚本，MySQL 启动后只创建空库。

已有本地数据卷（此前由 initdb 初始化）无需重建：Flyway 配置了
`baseline-on-migrate=true`、`baseline-version=1`，会把已有结构登记为 V1 并继续应用后续迁移。

如需从零重建本地数据库（会删除本地数据）：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.dev.yml down -v
bash scripts/dev-up.sh
```

## 目录规划与当前状态

架构文档 5.4 节列出的部署资产，当前仅完成本地开发依赖：

| 路径 | 状态 |
|---|---|
| `docker-compose.dev.yml` | 已实现，MySQL + Redis |
| `.env.example` / `.env` | 已实现 |
| `redis/redis.conf` | 已实现 |
| `docker-compose.yml` | 待生产编排确认 |
| `nginx/nginx.conf` | 待域名、TLS 与服务名确认 |
| `mysql/conf.d/` | 待生产配置确认 |
| `app/Dockerfile`、`worker/Dockerfile`、`judge/Dockerfile` | 待基础镜像与构建方式确认 |

未实现的目录已放置 README 说明约束，确认参数后再补充实际配置。

## 安全与边界

- MySQL、Redis 仅绑定 `127.0.0.1`，不暴露到局域网。
- `deploy/.env` 含本地开发密码，禁止提交到版本库。
- 当前编排只包含开发依赖；`tizhen-app`、`tizhen-worker`、`nginx`、`go-judge` 待镜像和沙箱参数确定后加入完整部署编排。
- Docker Hub 拉取镜像需要可用网络；如本机无法直连，需要在 Docker Desktop 中配置代理或镜像源。
