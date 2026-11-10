# Pinia Store

已实现：

- `auth.ts`：当前选手与管理员会话快照，不做持久化。
- `theme.ts`：明亮/暗淡主题，持久化到 localStorage。

规划：

- `battle.ts`：房间、对战进度与 WebSocket 状态（M07 对战模块）。
- `leaderboard.ts`：榜单筛选条件与缓存（M05 榜单模块）。

Store 只保存前端状态，认证与权限仍由后端校验。
