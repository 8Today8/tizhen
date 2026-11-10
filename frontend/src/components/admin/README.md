# 后台组件

运营后台页面布局骨架的复用组件：

| 组件 | 职责 |
|---|---|
| `AdminPage.vue` | 页头（标题、描述、操作区）与页面内容容器 |
| `AdminToolbar.vue` | 列表筛选栏与统计信息 |
| `AdminTableCard.vue` | 可带标题、工具栏、表格与分页的卡片容器 |
| `AdminEmptyState.vue` | 统一空状态 |
| `AdminPagination.vue` | 统一分页骨架，默认 20 条/页 |

布局样式统一在 `src/styles/admin.css`，页面只声明业务列与筛选控件，不重复编写布局样式。
