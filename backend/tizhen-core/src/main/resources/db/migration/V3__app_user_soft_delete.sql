-- 用户管理新增删除能力。采用逻辑删除保留历史成绩与关联记录，
-- 删除时同时置为 disabled，确保登录与榜单展示立即停止。
ALTER TABLE `app_user`
  ADD COLUMN `deleted_at` DATETIME(3) NULL COMMENT '逻辑删除时间（UTC）' AFTER `last_login_at`,
  ADD KEY `idx_app_user_deleted_at` (`deleted_at`);
