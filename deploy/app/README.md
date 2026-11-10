# tizhen-app 镜像

待补充 `Dockerfile`。需要覆盖：

- 多阶段构建：Maven 构建 `tizhen-app`，运行时使用 JRE 21；
- 以非 root 用户运行；
- 挂载文件存储卷（`tizhen.storage.base-dir`）；
- 健康检查使用 `/actuator/health`。

基础镜像、镜像仓库与构建参数确定后补充。
