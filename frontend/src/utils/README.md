# 工具函数

已实现：

- `cookie.ts`：读取非 HttpOnly Cookie（CSRF Token）。
- `format.ts`：UTC 时间转 Asia/Shanghai 展示。
- `storage.ts`：localStorage 安全读写，兼容禁用存储的场景。

规划：

- `escape.ts`：题面 HTML 转义与清洗（markdown-it + DOMPurify 接入时补充）。
