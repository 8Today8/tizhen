# MySQL 配置

待补充生产环境配置。开发编排已在 `docker-compose.dev.yml` 的 `command` 中直接设置字符集与时区，原因是 Windows bind mount 的配置文件会被 MySQL 8.4 判为 world-writable 而忽略。

生产配置需与数据库规则保持一致：`utf8mb4`、`utf8mb4_0900_ai_ci`、UTC 时区、慢查询与连接数上限。
