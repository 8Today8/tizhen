# go-judge 镜像

待补充 `Dockerfile` 与运行参数。配置来源为 `judge-runner/`，安全要求：

- 不暴露公网端口，仅允许 worker 访问；
- 不挂载业务数据库或文件卷；
- 限制 CPU、内存、进程数与输出大小；
- 固定 go-judge 版本并记录升级说明。
