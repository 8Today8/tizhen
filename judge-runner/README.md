# 判题运行环境

存放 go-judge 沙箱配置、语言环境与执行器说明。设计依据见 `docs/design/02.整体架构设计.md` 5.4 节。

当前状态：**骨架目录，尚未接入 go-judge**。判题编排由 `tizhen-worker` 负责，运行环境参数（并发、时间/内存限制、镜像版本）确定后再补充：

```text
judge-runner/
├─ config/
│  └─ go-judge.yml       # 沙箱参数、并发、语言配置
├─ languages/
│  ├─ cpp17/             # 编译命令与运行环境说明
│  └─ python310/         # Python 3.10+ 运行环境说明
└─ README.md
```

安全边界：判题容器不得访问外网、不得挂载业务数据卷，只接收本次提交的源码与测试数据。
