# 开发与运维脚本

已实现：

| 脚本 | 用途 |
|---|---|
| `dev-up.sh` | 检查 Docker 引擎，启动 MySQL 与 Redis |
| `dev-down.sh` | 停止开发依赖，保留数据卷 |
| `sql/schema.sql` | V1 设计快照，仅用于文档对照；实际迁移由 Flyway 执行 |

规划中（部署阶段补充）：

| 脚本 | 计划行为 |
|---|---|
| `db-migrate.sh` | 调用 Flyway 执行迁移，使用 `deploy/.env` 中的连接信息 |
| `backup.sh` | 每日备份 MySQL 与文件卷，保留 7 天 |
| `reset-demo-data.sh` | 重置开发/演示数据，禁止用于生产 |

后三者依赖生产编排、备份目录与演示数据集定义，当前未实现，避免写入未经确认的行为。
