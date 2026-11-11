# 判题运行环境

存放 go-judge 沙箱配置、语言环境与执行器说明。设计依据见 `docs/design/02.整体架构设计.md` 5.4 节。

当前状态：**worker 已通过 HTTP 对接 go-judge，沙箱运行配置与语言镜像仍待补充**。判题编排由 `tizhen-worker` 负责，默认连接 `http://127.0.0.1:5050`，可通过 `GO_JUDGE_URL` 覆盖：

```text
judge-runner/
├─ config/
│  └─ go-judge.yml       # 沙箱参数、并发、语言配置（待补充）
├─ languages/
│  ├─ cpp17/             # 编译命令与运行环境说明（待补充）
│  └─ python310/         # Python 3.10+ 运行环境说明（待补充）
└─ README.md
```

安全边界：判题容器不得访问外网、不得挂载业务数据卷，只接收本次提交的源码与测试数据。
