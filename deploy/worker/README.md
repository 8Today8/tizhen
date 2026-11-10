# tizhen-worker 镜像

待补充 `Dockerfile`。与 `tizhen-app` 共用构建产物来源，运行时以非 Web 模式启动；需要访问数据库、Redis、文件存储卷与 go-judge 服务。

判题并发、任务轮询间隔通过 `WORKER_*` 环境变量配置。
