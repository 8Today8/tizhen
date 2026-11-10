-- 题阵 · 数据库建表脚本（最新结构设计快照）
-- 正式建表由 Flyway 执行：backend/tizhen-core/src/main/resources/db/migration/V1__init_schema.sql、V2__manual_declaration_review.sql
-- 本文件用于设计文档对照；结构变更以 Flyway 迁移为准，并同步更新本快照。
-- 目标数据库：MySQL 8.4 LTS
-- 设计依据：docs/design/01.功能模块梳理.md、docs/design/02.整体架构设计.md、docs/rules/database-rules.md
-- 约定：
--   1. 全部表使用 InnoDB、utf8mb4；对象名 snake_case；表名使用单数。
--   2. 主键统一 BIGINT UNSIGNED AUTO_INCREMENT；禁止物理外键。
--   3. 时间统一按 UTC 存储，使用 DATETIME(3)；展示层转换为 Asia/Shanghai。
--   4. 业务周使用 week_start_date / weekly_week_start 保存“周一”的日期，避免时区换算歧义。
--   5. 软删除通过 deleted_at、removed_at、status 或 active_flag 表达；唯一约束只对有效记录生效。
--   6. 本脚本只包含表结构与基础角色初始化，不包含演示业务数据。

CREATE DATABASE IF NOT EXISTS `tizhen`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `tizhen`;

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================================
-- 一、账号与权限（M01）
-- ============================================================

CREATE TABLE IF NOT EXISTS `admin_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(32) NOT NULL COMMENT '角色编码：super_admin / content_operator',
  `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `permissions` JSON NOT NULL COMMENT '权限编码数组；首期固定角色，后端仍按角色编码鉴权',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_role_code` (`code`),
  CONSTRAINT `chk_admin_role_code` CHECK (`code` IN ('super_admin', 'content_operator'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='后台角色；首期仅超级管理员与内容运营';

CREATE TABLE IF NOT EXISTS `admin_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(32) NOT NULL COMMENT '登录用户名，全局唯一',
  `password_hash` VARCHAR(100) NOT NULL COMMENT '密码哈希（BCrypt），禁止明文',
  `display_name` VARCHAR(64) NOT NULL COMMENT '后台显示名称',
  `avatar_url` VARCHAR(512) NULL COMMENT '头像地址',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色 ID；逻辑关联 admin_role.id',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态：active / disabled',
  `last_login_at` DATETIME(3) NULL COMMENT '最近登录时间（UTC）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_username` (`username`),
  KEY `idx_admin_user_role` (`role_id`),
  KEY `idx_admin_user_status` (`status`),
  CONSTRAINT `chk_admin_user_status` CHECK (`status` IN ('active', 'disabled'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='后台管理员账号；不开放注册，由超级管理员创建';

CREATE TABLE IF NOT EXISTS `app_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(20) NOT NULL COMMENT '登录用户名，3–20 位字母/数字/下划线，大小写不敏感',
  `password_hash` VARCHAR(100) NOT NULL COMMENT '密码哈希（BCrypt），禁止明文',
  `nickname` VARCHAR(64) NOT NULL COMMENT '站内展示昵称',
  `email` VARCHAR(254) NULL COMMENT '绑定邮箱；首期只做基础绑定与展示',
  `phone` VARCHAR(32) NULL COMMENT '可选手机号；首期只做展示，不做验证/找回',
  `avatar_url` VARCHAR(512) NULL COMMENT '头像地址',
  `school` VARCHAR(128) NULL COMMENT '学校；可选',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态：active / disabled',
  `last_login_at` DATETIME(3) NULL COMMENT '最近登录时间（UTC）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_user_username` (`username`),
  UNIQUE KEY `uk_app_user_email` (`email`),
  KEY `idx_app_user_status` (`status`),
  KEY `idx_app_user_created_at` (`created_at`),
  CONSTRAINT `chk_app_user_username_length` CHECK (CHAR_LENGTH(`username`) BETWEEN 3 AND 20),
  CONSTRAINT `chk_app_user_status` CHECK (`status` IN ('active', 'disabled'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='选手账号；禁用后不删除数据，仅停止登录与榜单展示';

CREATE TABLE IF NOT EXISTS `llm_credential` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '选手 ID；逻辑关联 app_user.id',
  `provider` VARCHAR(16) NOT NULL COMMENT '预置服务商：deepseek / qwen / zhipu / openai',
  `model` VARCHAR(64) NULL COMMENT '可选模型名；为空时使用服务端默认模型',
  `api_key_ciphertext` MEDIUMTEXT NULL COMMENT 'AES-256-GCM 密文（含 nonce/tag）；禁止明文',
  `api_key_last4` CHAR(4) NULL COMMENT 'Key 末 4 位，仅用于脱敏展示',
  `encryption_key_version` SMALLINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '加密密钥版本，便于轮换',
  `status` VARCHAR(16) NOT NULL DEFAULT 'valid' COMMENT '状态：valid / invalid / deleted',
  `last_validated_at` DATETIME(3) NULL COMMENT '最近一次可用性校验时间（UTC）',
  `last_validation_error` VARCHAR(512) NULL COMMENT '最近一次校验失败原因；不得包含 Key 明文',
  `deleted_at` DATETIME(3) NULL COMMENT '软删除时间（UTC）；删除时清空密文与末四位',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_llm_credential_user` (`user_id`),
  KEY `idx_llm_credential_status` (`status`),
  CONSTRAINT `chk_llm_credential_provider` CHECK (`provider` IN ('deepseek', 'qwen', 'zhipu', 'openai')),
  CONSTRAINT `chk_llm_credential_status` CHECK (`status` IN ('valid', 'invalid', 'deleted')),
  CONSTRAINT `chk_llm_credential_active_cipher` CHECK (`status` = 'deleted' OR `api_key_ciphertext` IS NOT NULL),
  CONSTRAINT `chk_llm_credential_deleted_cipher` CHECK (`status` <> 'deleted' OR `api_key_ciphertext` IS NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='选手 LLM 造数据凭证；每人一行，删除时保留行以维持历史任务引用';

-- ============================================================
-- 二、外部成绩接入（M02）
-- ============================================================

CREATE TABLE IF NOT EXISTS `external_account` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '选手 ID；逻辑关联 app_user.id',
  `platform` VARCHAR(32) NOT NULL COMMENT '外部平台；首期固定 codeforces',
  `handle` VARCHAR(64) NOT NULL COMMENT '平台账号 handle',
  `profile_url` VARCHAR(512) NULL COMMENT '平台个人主页',
  `verification_status` VARCHAR(16) NOT NULL DEFAULT 'simulated' COMMENT '校验状态：verified / simulated / unverified',
  `data_source` VARCHAR(16) NOT NULL DEFAULT 'simulated' COMMENT '数据来源：simulated / api',
  `weekly_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前统计周过题数',
  `weekly_week_start` DATE NULL COMMENT '当前统计周周一日期（北京时间自然周）',
  `all_time_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '历史累计过题数',
  `last_sync_at` DATETIME(3) NULL COMMENT '最近同步时间（UTC）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态：active / unbound',
  `unbound_at` DATETIME(3) NULL COMMENT '解绑时间（UTC）；历史数据保留但不再更新',
  `active_flag` TINYINT UNSIGNED NULL DEFAULT 1 COMMENT '有效标记：1=有效，NULL=已解绑；用于唯一约束',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_external_account_platform_handle_active` (`platform`, `handle`, `active_flag`),
  UNIQUE KEY `uk_external_account_user_platform_active` (`user_id`, `platform`, `active_flag`),
  KEY `idx_external_account_week` (`weekly_week_start`, `user_id`),
  KEY `idx_external_account_user_created` (`user_id`, `created_at`),
  CONSTRAINT `chk_external_account_verification` CHECK (`verification_status` IN ('verified', 'simulated', 'unverified')),
  CONSTRAINT `chk_external_account_source` CHECK (`data_source` IN ('simulated', 'api')),
  CONSTRAINT `chk_external_account_status` CHECK (`status` IN ('active', 'unbound')),
  CONSTRAINT `chk_external_account_active_flag` CHECK (
    (`status` = 'active' AND `active_flag` = 1 AND `unbound_at` IS NULL)
    OR (`status` = 'unbound' AND `active_flag` IS NULL AND `unbound_at` IS NOT NULL)
  ),
  CONSTRAINT `chk_external_account_week_monday` CHECK (
    `weekly_week_start` IS NULL OR DAYOFWEEK(`weekly_week_start`) = 2
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='CF 等外部账号；保存当前周快照与历史累计，解绑保留历史';

CREATE TABLE IF NOT EXISTS `manual_declaration` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '选手 ID；逻辑关联 app_user.id',
  `platform` VARCHAR(32) NOT NULL COMMENT '申报平台，例如 牛客 / 洛谷 / AtCoder',
  `account` VARCHAR(128) NOT NULL COMMENT '平台账号或主页标识',
  `profile_url` VARCHAR(512) NULL COMMENT '平台主页地址；可选',
  `count` INT UNSIGNED NOT NULL COMMENT '本周申报过题数',
  `week_start_date` DATE NOT NULL COMMENT '申报自然周周一日期（北京时间）',
  `verification_status` VARCHAR(16) NOT NULL DEFAULT 'unverified' COMMENT '校验状态：unverified / verified / rejected',
  `review_note` VARCHAR(200) NULL COMMENT '审核备注（仅后台可见）',
  `reviewed_by` BIGINT UNSIGNED NULL COMMENT '审核管理员 ID；逻辑关联 admin_user.id',
  `reviewed_at` DATETIME(3) NULL COMMENT '审核时间（UTC）',
  `active_flag` TINYINT UNSIGNED NULL DEFAULT 1 COMMENT '有效标记：1=有效，NULL=已删除；用于唯一约束',
  `deleted_at` DATETIME(3) NULL COMMENT '软删除时间（UTC）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manual_declaration_user_platform_week` (`user_id`, `platform`, `week_start_date`, `active_flag`),
  KEY `idx_manual_declaration_week` (`week_start_date`, `user_id`),
  KEY `idx_manual_declaration_user_created` (`user_id`, `created_at`),
  KEY `idx_manual_declaration_review` (`verification_status`, `active_flag`, `week_start_date`, `user_id`),
  CONSTRAINT `chk_manual_declaration_week_monday` CHECK (DAYOFWEEK(`week_start_date`) = 2),
  CONSTRAINT `chk_manual_declaration_verification` CHECK (`verification_status` IN ('unverified', 'verified', 'rejected')),
  CONSTRAINT `chk_manual_declaration_active` CHECK (
    (`active_flag` = 1 AND `deleted_at` IS NULL)
    OR (`active_flag` IS NULL AND `deleted_at` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='其他平台周过题手动申报；同用户+平台+周只保留一条有效记录';

-- ============================================================
-- 三、学习内容与进度（M03）
-- ============================================================

CREATE TABLE IF NOT EXISTS `learning_route` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '路线名称（专题）',
  `description` TEXT NULL COMMENT '路线简介',
  `cover_url` VARCHAR(512) NULL COMMENT '封面地址',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `status` VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '状态：draft / published / offline',
  `deleted_at` DATETIME(3) NULL COMMENT '软删除时间（UTC）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  KEY `idx_learning_route_status_sort` (`status`, `sort_order`),
  CONSTRAINT `chk_learning_route_status` CHECK (`status` IN ('draft', 'published', 'offline'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学习路线（专题）';

CREATE TABLE IF NOT EXISTS `learning_stage` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `route_id` BIGINT UNSIGNED NOT NULL COMMENT '路线 ID；逻辑关联 learning_route.id',
  `name` VARCHAR(100) NOT NULL COMMENT '阶段名称',
  `content` MEDIUMTEXT NULL COMMENT '图文内容（富文本）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '路线内排序值',
  `status` VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '状态：draft / published / offline',
  `deleted_at` DATETIME(3) NULL COMMENT '软删除时间（UTC）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  KEY `idx_learning_stage_route_sort` (`route_id`, `sort_order`),
  KEY `idx_learning_stage_status` (`status`),
  CONSTRAINT `chk_learning_stage_status` CHECK (`status` IN ('draft', 'published', 'offline'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学习阶段；一个路线包含多个阶段';

CREATE TABLE IF NOT EXISTS `stage_course` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stage_id` BIGINT UNSIGNED NOT NULL COMMENT '阶段 ID；逻辑关联 learning_stage.id',
  `title` VARCHAR(200) NOT NULL COMMENT '课程标题',
  `url` VARCHAR(512) NOT NULL COMMENT 'B 站视频完整链接',
  `duration_seconds` INT UNSIGNED NULL COMMENT '视频时长（秒）',
  `source` VARCHAR(128) NULL COMMENT '来源/UP 主；可选',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '阶段内排序值',
  `status` VARCHAR(16) NOT NULL DEFAULT 'published' COMMENT '状态：draft / published / offline',
  `deleted_at` DATETIME(3) NULL COMMENT '软删除时间（UTC）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  KEY `idx_stage_course_stage_sort` (`stage_id`, `sort_order`),
  KEY `idx_stage_course_status` (`status`),
  CONSTRAINT `chk_stage_course_status` CHECK (`status` IN ('draft', 'published', 'offline'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='阶段关联的 B 站视频课程；不做观看进度';

CREATE TABLE IF NOT EXISTS `stage_problem` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stage_id` BIGINT UNSIGNED NOT NULL COMMENT '阶段 ID；逻辑关联 learning_stage.id',
  `problem_id` BIGINT UNSIGNED NOT NULL COMMENT '题目 ID；逻辑关联 problem.id',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '阶段内题目排序',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_problem` (`stage_id`, `problem_id`),
  KEY `idx_stage_problem_problem` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='阶段与题目的多对多关联；同一题可被多个阶段引用';

CREATE TABLE IF NOT EXISTS `learning_progress` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '选手 ID；逻辑关联 app_user.id',
  `stage_id` BIGINT UNSIGNED NOT NULL COMMENT '阶段 ID；逻辑关联 learning_stage.id',
  `completed` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否完成：0/1',
  `completed_at` DATETIME(3) NULL COMMENT '完成时间（UTC）；取消完成时置空',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_learning_progress_user_stage` (`user_id`, `stage_id`),
  KEY `idx_learning_progress_stage` (`stage_id`, `completed`),
  CONSTRAINT `chk_learning_progress_completed` CHECK (
    (`completed` = 0 AND `completed_at` IS NULL)
    OR (`completed` = 1 AND `completed_at` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='选手阶段完成状态；路线进度只按阶段完成数计算';

CREATE TABLE IF NOT EXISTS `external_completion` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '选手 ID；逻辑关联 app_user.id',
  `problem_id` BIGINT UNSIGNED NOT NULL COMMENT '外链题目 ID；逻辑关联 problem.id',
  `completed` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否完成：0/1',
  `completion_source` VARCHAR(16) NOT NULL DEFAULT 'manual' COMMENT '完成来源：auto / manual',
  `completed_at` DATETIME(3) NULL COMMENT '完成时间（UTC）；取消完成时置空',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_external_completion_user_problem` (`user_id`, `problem_id`),
  KEY `idx_external_completion_problem` (`problem_id`),
  CONSTRAINT `chk_external_completion_source` CHECK (`completion_source` IN ('auto', 'manual')),
  CONSTRAINT `chk_external_completion_completed` CHECK (
    (`completed` = 0 AND `completed_at` IS NULL)
    OR (`completed` = 1 AND `completed_at` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='外链题完成状态；只用于学习进度展示，不计入榜单';

-- ============================================================
-- 四、题库与判题（M04）
-- ============================================================

CREATE TABLE IF NOT EXISTS `problem` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` VARCHAR(16) NOT NULL COMMENT '题型：oj / objective / short_answer / external',
  `objective_type` VARCHAR(16) NULL COMMENT '客观题类型：judge / single / multiple；仅 objective 使用',
  `title` VARCHAR(200) NOT NULL COMMENT '题目标题',
  `statement` MEDIUMTEXT NOT NULL COMMENT '题面（富文本/Markdown）',
  `input_format` MEDIUMTEXT NULL COMMENT '输入格式；OJ 使用',
  `output_format` MEDIUMTEXT NULL COMMENT '输出格式；OJ 使用',
  `explanation` MEDIUMTEXT NULL COMMENT '客观题解析；可选',
  `reference_answer` MEDIUMTEXT NULL COMMENT '简答题参考答案',
  `scoring_rubric` JSON NULL COMMENT '简答题评分要点数组',
  `max_score` SMALLINT UNSIGNED NOT NULL DEFAULT 100 COMMENT '简答题满分；本期固定 100',
  `difficulty` VARCHAR(16) NOT NULL COMMENT '难度：entry / basic / advanced',
  `source` VARCHAR(64) NOT NULL COMMENT '来源，例如 站内原创 / Codeforces',
  `external_url` VARCHAR(512) NULL COMMENT '外链题完整地址；仅 external 使用',
  `platform` VARCHAR(32) NULL COMMENT '外链题平台；仅 external 使用',
  `time_limit_ms` INT UNSIGNED NULL COMMENT 'OJ 时限（毫秒）',
  `memory_limit_mb` INT UNSIGNED NULL COMMENT 'OJ 内存限制（MB）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '状态：draft / published / offline',
  `deleted_at` DATETIME(3) NULL COMMENT '软删除时间（UTC）；被引用题目不得软删除',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  KEY `idx_problem_type_status` (`type`, `status`),
  KEY `idx_problem_difficulty` (`difficulty`),
  KEY `idx_problem_source` (`source`),
  KEY `idx_problem_created_at` (`created_at`),
  CONSTRAINT `chk_problem_type` CHECK (`type` IN ('oj', 'objective', 'short_answer', 'external')),
  CONSTRAINT `chk_problem_objective_type` CHECK (`objective_type` IS NULL OR `objective_type` IN ('judge', 'single', 'multiple')),
  CONSTRAINT `chk_problem_difficulty` CHECK (`difficulty` IN ('entry', 'basic', 'advanced')),
  CONSTRAINT `chk_problem_status` CHECK (`status` IN ('draft', 'published', 'offline')),
  CONSTRAINT `chk_problem_objective_type_required` CHECK (`type` <> 'objective' OR `objective_type` IS NOT NULL),
  CONSTRAINT `chk_problem_objective_type_only` CHECK (`type` = 'objective' OR `objective_type` IS NULL),
  CONSTRAINT `chk_problem_external_url` CHECK (`type` <> 'external' OR (`external_url` IS NOT NULL AND `platform` IS NOT NULL)),
  CONSTRAINT `chk_problem_publish_oj` CHECK (
    `status` <> 'published' OR `type` <> 'oj'
    OR (`time_limit_ms` IS NOT NULL AND `memory_limit_mb` IS NOT NULL)
  ),
  CONSTRAINT `chk_problem_publish_short` CHECK (
    `status` <> 'published' OR `type` <> 'short_answer'
    OR (`reference_answer` IS NOT NULL AND `scoring_rubric` IS NOT NULL)
  ),
  CONSTRAINT `chk_problem_short_max_score` CHECK (
    `type` <> 'short_answer' OR `max_score` = 100
  ),
  CONSTRAINT `chk_problem_publish_external` CHECK (
    `status` <> 'published' OR `type` <> 'external' OR `external_url` IS NOT NULL
  ),
  CONSTRAINT `chk_problem_limits` CHECK (
    (`time_limit_ms` IS NULL OR `time_limit_ms` > 0)
    AND (`memory_limit_mb` IS NULL OR `memory_limit_mb` > 0)
    AND `max_score` > 0
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='统一题库：OJ / 客观题 / 简答题 / 外链题';

CREATE TABLE IF NOT EXISTS `problem_tag` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `problem_id` BIGINT UNSIGNED NOT NULL COMMENT '题目 ID；逻辑关联 problem.id',
  `tag_type` VARCHAR(16) NOT NULL DEFAULT 'topic' COMMENT '标签类型；首期固定 topic',
  `tag_value` VARCHAR(64) NOT NULL COMMENT '标签值，例如 动态规划 / 图论',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_problem_tag` (`problem_id`, `tag_type`, `tag_value`),
  KEY `idx_problem_tag_value` (`tag_type`, `tag_value`),
  CONSTRAINT `chk_problem_tag_type` CHECK (`tag_type` = 'topic')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='题目专题标签；难度/来源/题型使用 problem 主表字段';

CREATE TABLE IF NOT EXISTS `problem_sample` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `problem_id` BIGINT UNSIGNED NOT NULL COMMENT '题目 ID；逻辑关联 problem.id',
  `input_data` MEDIUMTEXT NULL COMMENT '样例输入',
  `output_data` MEDIUMTEXT NULL COMMENT '样例输出',
  `explanation` TEXT NULL COMMENT '样例说明；可选',
  `sort_order` SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '样例顺序',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_problem_sample_sort` (`problem_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='题面样例；与判题测试数据分开';

CREATE TABLE IF NOT EXISTS `test_data` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `problem_id` BIGINT UNSIGNED NOT NULL COMMENT '题目 ID；逻辑关联 problem.id',
  `input_data` LONGTEXT NOT NULL COMMENT '测试输入',
  `output_data` LONGTEXT NOT NULL COMMENT '标准输出',
  `sort_order` SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '测试点顺序',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_test_data_problem_sort` (`problem_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='OJ 判题测试数据；首期直接存数据库';

CREATE TABLE IF NOT EXISTS `objective_option` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `problem_id` BIGINT UNSIGNED NOT NULL COMMENT '题目 ID；逻辑关联 problem.id',
  `option_key` CHAR(1) NOT NULL COMMENT '选项键，例如 A/B/C/D',
  `content` TEXT NOT NULL COMMENT '选项内容',
  `is_correct` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否正确答案：0/1',
  `sort_order` SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '选项顺序',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_objective_option_problem_key` (`problem_id`, `option_key`),
  KEY `idx_objective_option_sort` (`problem_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='客观题选项与正确答案标记';

CREATE TABLE IF NOT EXISTS `submission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '提交人 ID；逻辑关联 app_user.id',
  `problem_id` BIGINT UNSIGNED NOT NULL COMMENT '题目 ID；逻辑关联 problem.id',
  `problem_type` VARCHAR(16) NOT NULL COMMENT '提交时题型快照：oj / objective / short_answer',
  `battle_id` BIGINT UNSIGNED NULL COMMENT '对战房间 ID；普通刷题为空',
  `language` VARCHAR(16) NULL COMMENT 'OJ 语言：cpp17 / python310',
  `code` LONGTEXT NULL COMMENT 'OJ 提交代码',
  `answer_json` JSON NULL COMMENT '客观题所选选项键数组，例如 ["A","C"]',
  `answer_text` MEDIUMTEXT NULL COMMENT '简答题文本答案',
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态：pending / judging / grading / finished / failed',
  `verdict` VARCHAR(16) NULL COMMENT '结果：AC / WA / TLE / MLE / RE / CE / SYSTEM_ERROR',
  `score` DECIMAL(5,2) NULL COMMENT '客观题得分或简答题 AI 分数，0–100',
  `ai_feedback` MEDIUMTEXT NULL COMMENT '简答题 AI 评语；按不可信内容转义展示',
  `failure_reason` TEXT NULL COMMENT '判题/判分失败原因',
  `judge_detail` TEXT NULL COMMENT '判题详情或首个失败测试点说明',
  `compile_output` MEDIUMTEXT NULL COMMENT '编译输出/错误信息',
  `judge_time_ms` INT UNSIGNED NULL COMMENT '最大运行时间（毫秒）',
  `memory_kb` INT UNSIGNED NULL COMMENT '最大内存占用（KB）',
  `judge_attempts` SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'worker 领取次数；用于崩溃恢复，非业务重试',
  `judge_started_at` DATETIME(3) NULL COMMENT '判题/判分开始时间（UTC）',
  `judged_at` DATETIME(3) NULL COMMENT '判题/判分完成时间（UTC）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '提交时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  KEY `idx_submission_user_problem_created` (`user_id`, `problem_id`, `created_at`),
  KEY `idx_submission_problem_created` (`problem_id`, `created_at`),
  KEY `idx_submission_status_created` (`status`, `created_at`),
  KEY `idx_submission_battle` (`battle_id`),
  KEY `idx_submission_user_created` (`user_id`, `created_at`),
  CONSTRAINT `chk_submission_problem_type` CHECK (`problem_type` IN ('oj', 'objective', 'short_answer')),
  CONSTRAINT `chk_submission_status` CHECK (`status` IN ('pending', 'judging', 'grading', 'finished', 'failed')),
  CONSTRAINT `chk_submission_language` CHECK (`language` IS NULL OR `language` IN ('cpp17', 'python310')),
  CONSTRAINT `chk_submission_verdict` CHECK (
    `verdict` IS NULL OR `verdict` IN ('AC', 'WA', 'TLE', 'MLE', 'RE', 'CE', 'SYSTEM_ERROR')
  ),
  CONSTRAINT `chk_submission_score` CHECK (`score` IS NULL OR (`score` >= 0 AND `score` <= 100)),
  CONSTRAINT `chk_submission_oj_fields` CHECK (
    `problem_type` <> 'oj' OR (`language` IS NOT NULL AND `code` IS NOT NULL)
  ),
  CONSTRAINT `chk_submission_objective_fields` CHECK (
    `problem_type` <> 'objective' OR `answer_json` IS NOT NULL
  ),
  CONSTRAINT `chk_submission_short_fields` CHECK (
    `problem_type` <> 'short_answer' OR `answer_text` IS NOT NULL
  ),
  CONSTRAINT `chk_submission_finished_verdict` CHECK (`status` <> 'finished' OR `verdict` IS NOT NULL),
  CONSTRAINT `chk_submission_failed_reason` CHECK (`status` <> 'failed' OR `failure_reason` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='统一提交/作答记录；普通与对战提交共用，对战通过 battle_id 关联';

CREATE TABLE IF NOT EXISTS `first_ac` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '选手 ID；逻辑关联 app_user.id',
  `problem_id` BIGINT UNSIGNED NOT NULL COMMENT 'OJ 题目 ID；逻辑关联 problem.id',
  `submission_id` BIGINT UNSIGNED NOT NULL COMMENT '产生首次 AC 的提交 ID；逻辑关联 submission.id',
  `first_ac_at` DATETIME(3) NOT NULL COMMENT '首次 AC 发生时间（UTC）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '记录创建时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_first_ac_user_problem` (`user_id`, `problem_id`),
  UNIQUE KEY `uk_first_ac_submission` (`submission_id`),
  KEY `idx_first_ac_time_user` (`first_ac_at`, `user_id`),
  KEY `idx_first_ac_user_time` (`user_id`, `first_ac_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='站内首次 AC 事实；唯一约束保证同一用户同一题只计一次';

CREATE TABLE IF NOT EXISTS `wrong_book` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '选手 ID；逻辑关联 app_user.id',
  `problem_id` BIGINT UNSIGNED NOT NULL COMMENT '题目 ID；逻辑关联 problem.id',
  `latest_error` VARCHAR(64) NOT NULL COMMENT '最近错误标记，例如 WA / TLE / AI 52',
  `latest_score` DECIMAL(5,2) NULL COMMENT '简答题最近一次 AI 分数；其他题型为空',
  `error_count` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '累计错误次数',
  `last_error_at` DATETIME(3) NOT NULL COMMENT '最近错误时间（UTC）',
  `removed_at` DATETIME(3) NULL COMMENT '选手手动移出时间（UTC）；再次出错时置空',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wrong_book_user_problem` (`user_id`, `problem_id`),
  KEY `idx_wrong_book_user_active` (`user_id`, `removed_at`, `last_error_at`),
  KEY `idx_wrong_book_problem` (`problem_id`),
  CONSTRAINT `chk_wrong_book_count` CHECK (`error_count` > 0),
  CONSTRAINT `chk_wrong_book_score` CHECK (`latest_score` IS NULL OR (`latest_score` >= 0 AND `latest_score` <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='错题本聚合；按用户+题目唯一，手动移除后再次出错可恢复';

-- ============================================================
-- 五、对战（M07）
-- ============================================================

CREATE TABLE IF NOT EXISTS `battle_room` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_code` VARCHAR(16) NOT NULL COMMENT '邀请码/房间码',
  `creator_id` BIGINT UNSIGNED NOT NULL COMMENT '创建者 ID；逻辑关联 app_user.id',
  `opponent_id` BIGINT UNSIGNED NULL COMMENT '对手 ID；逻辑关联 app_user.id',
  `problem_id` BIGINT UNSIGNED NULL COMMENT '抽取的题目 ID；逻辑关联 problem.id',
  `filter_difficulty` VARCHAR(16) NULL COMMENT '创建时难度筛选：entry / basic / advanced',
  `filter_tag` VARCHAR(64) NULL COMMENT '创建时专题标签筛选',
  `creator_ready` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建者是否准备：0/1',
  `opponent_ready` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '对手是否准备：0/1',
  `status` VARCHAR(16) NOT NULL DEFAULT 'waiting' COMMENT '状态：waiting / ready / ongoing / finished / cancelled',
  `result` VARCHAR(16) NULL COMMENT '结果（相对创建者）：creator_win / opponent_win / draw',
  `end_reason` VARCHAR(16) NULL COMMENT '结束原因：first_ac / timeout / surrender / disconnect / cancelled',
  `started_at` DATETIME(3) NULL COMMENT '开始时间（UTC）',
  `ended_at` DATETIME(3) NULL COMMENT '结束时间（UTC）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_battle_room_code` (`room_code`),
  KEY `idx_battle_room_creator` (`creator_id`, `created_at`),
  KEY `idx_battle_room_opponent` (`opponent_id`, `created_at`),
  KEY `idx_battle_room_status` (`status`, `created_at`),
  KEY `idx_battle_room_problem` (`problem_id`),
  CONSTRAINT `chk_battle_room_status` CHECK (`status` IN ('waiting', 'ready', 'ongoing', 'finished', 'cancelled')),
  CONSTRAINT `chk_battle_room_result` CHECK (`result` IS NULL OR `result` IN ('creator_win', 'opponent_win', 'draw')),
  CONSTRAINT `chk_battle_room_end_reason` CHECK (
    `end_reason` IS NULL OR `end_reason` IN ('first_ac', 'timeout', 'surrender', 'disconnect', 'cancelled')
  ),
  CONSTRAINT `chk_battle_room_filter_difficulty` CHECK (
    `filter_difficulty` IS NULL OR `filter_difficulty` IN ('entry', 'basic', 'advanced')
  ),
  CONSTRAINT `chk_battle_room_distinct_players` CHECK (`opponent_id` IS NULL OR `opponent_id` <> `creator_id`),
  CONSTRAINT `chk_battle_room_finished` CHECK (
    `status` <> 'finished'
    OR (`result` IS NOT NULL AND `ended_at` IS NOT NULL AND `started_at` IS NOT NULL)
  ),
  CONSTRAINT `chk_battle_room_started` CHECK (
    `status` NOT IN ('ongoing', 'finished')
    OR (`problem_id` IS NOT NULL AND `started_at` IS NOT NULL)
  ),
  CONSTRAINT `chk_battle_room_time` CHECK (`ended_at` IS NULL OR `started_at` IS NULL OR `ended_at` >= `started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='1v1 对战房间；房间结束后直接作为对战记录，不另建记录表';

-- ============================================================
-- 六、AI 造数据（M08）
-- ============================================================

CREATE TABLE IF NOT EXISTS `data_gen_task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建者 ID；逻辑关联 app_user.id',
  `title` VARCHAR(200) NOT NULL COMMENT '题目名称/任务标题',
  `statement` MEDIUMTEXT NOT NULL COMMENT '题面',
  `input_format` MEDIUMTEXT NOT NULL COMMENT '输入格式',
  `output_format` MEDIUMTEXT NOT NULL COMMENT '输出格式',
  `std_code` LONGTEXT NOT NULL COMMENT '用户提供的 std 代码',
  `std_language` VARCHAR(16) NOT NULL DEFAULT 'cpp17' COMMENT 'std 语言：cpp17 / python310',
  `data_range` JSON NOT NULL COMMENT '数据范围变量定义数组',
  `group_count` SMALLINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '生成组数',
  `generation_mode` VARCHAR(24) NOT NULL COMMENT '模式：random / llm_generator / llm_direct_data',
  `special_requirements` TEXT NULL COMMENT 'LLM 模式特殊构造要求；选填',
  `llm_provider` VARCHAR(16) NULL COMMENT 'LLM 服务商快照：deepseek / qwen / zhipu / openai',
  `llm_model` VARCHAR(64) NULL COMMENT '实际使用的模型快照',
  `llm_credential_id` BIGINT UNSIGNED NULL COMMENT '使用的凭证 ID；逻辑关联 llm_credential.id',
  `generator_code` LONGTEXT NULL COMMENT 'LLM 构造模式生成的构造器代码；仅内部可见',
  `generator_status` VARCHAR(24) NOT NULL DEFAULT 'not_applicable' COMMENT '构造器状态：not_applicable / pending / generating / ready / failed',
  `llm_data_status` VARCHAR(24) NOT NULL DEFAULT 'not_applicable' COMMENT 'LLM 直接生成状态：not_applicable / pending / generating / validating / ready / failed',
  `status` VARCHAR(24) NOT NULL DEFAULT 'pending' COMMENT '任务状态：pending / llm_generating / llm_data_generating / generating / success / failed',
  `failure_stage` VARCHAR(24) NULL COMMENT '失败阶段：llm_generator / llm_data / generator / validation / std / system',
  `failure_reason` TEXT NULL COMMENT '失败原因',
  `result_summary` JSON NULL COMMENT '结果摘要，例如总组数、总耗时、校验统计',
  `time_limit_ms` INT UNSIGNED NOT NULL COMMENT 'std/程序时限（毫秒）',
  `memory_limit_mb` INT UNSIGNED NOT NULL COMMENT 'std/程序内存限制（MB）',
  `attempts` SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'worker 领取次数；用于崩溃恢复，非业务重试',
  `started_at` DATETIME(3) NULL COMMENT '开始执行时间（UTC）',
  `finished_at` DATETIME(3) NULL COMMENT '结束时间（UTC）',
  `heartbeat_at` DATETIME(3) NULL COMMENT 'worker 心跳时间（UTC）',
  `files_expire_at` DATETIME(3) NOT NULL COMMENT '文件过期时间（UTC），创建后 7 天',
  `files_cleaned_at` DATETIME(3) NULL COMMENT '文件清理时间（UTC）；任务记录保留',
  `active_llm_user_id` BIGINT UNSIGNED GENERATED ALWAYS AS (
    CASE
      WHEN `generation_mode` <> 'random'
       AND `status` IN ('pending', 'llm_generating', 'llm_data_generating', 'generating')
      THEN `user_id`
      ELSE NULL
    END
  ) STORED COMMENT '派生列：进行中的 LLM 任务用户 ID；用于每人最多 1 个进行中 LLM 任务',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_gen_task_active_llm` (`active_llm_user_id`),
  KEY `idx_data_gen_task_user_created` (`user_id`, `created_at`),
  KEY `idx_data_gen_task_status_created` (`status`, `created_at`),
  KEY `idx_data_gen_task_user_status_mode` (`user_id`, `status`, `generation_mode`),
  KEY `idx_data_gen_task_expire` (`files_expire_at`),
  CONSTRAINT `chk_data_gen_task_mode` CHECK (`generation_mode` IN ('random', 'llm_generator', 'llm_direct_data')),
  CONSTRAINT `chk_data_gen_task_status` CHECK (
    `status` IN ('pending', 'llm_generating', 'llm_data_generating', 'generating', 'success', 'failed')
  ),
  CONSTRAINT `chk_data_gen_task_generator_status` CHECK (
    `generator_status` IN ('not_applicable', 'pending', 'generating', 'ready', 'failed')
  ),
  CONSTRAINT `chk_data_gen_task_llm_data_status` CHECK (
    `llm_data_status` IN ('not_applicable', 'pending', 'generating', 'validating', 'ready', 'failed')
  ),
  CONSTRAINT `chk_data_gen_task_failure_stage` CHECK (
    `failure_stage` IS NULL
    OR `failure_stage` IN ('llm_generator', 'llm_data', 'generator', 'validation', 'std', 'system')
  ),
  CONSTRAINT `chk_data_gen_task_std_language` CHECK (`std_language` IN ('cpp17', 'python310')),
  CONSTRAINT `chk_data_gen_task_provider` CHECK (
    `llm_provider` IS NULL OR `llm_provider` IN ('deepseek', 'qwen', 'zhipu', 'openai')
  ),
  CONSTRAINT `chk_data_gen_task_random_llm` CHECK (
    `generation_mode` <> 'random'
    OR (`llm_provider` IS NULL AND `llm_credential_id` IS NULL AND `llm_model` IS NULL)
  ),
  CONSTRAINT `chk_data_gen_task_llm_fields` CHECK (
    `generation_mode` = 'random'
    OR (`llm_provider` IS NOT NULL AND `llm_credential_id` IS NOT NULL)
  ),
  CONSTRAINT `chk_data_gen_task_direct_no_generator` CHECK (
    `generation_mode` <> 'llm_direct_data' OR `generator_code` IS NULL
  ),
  CONSTRAINT `chk_data_gen_task_success_generator` CHECK (
    `status` <> 'success' OR `generation_mode` <> 'llm_generator' OR `generator_code` IS NOT NULL
  ),
  CONSTRAINT `chk_data_gen_task_failed_reason` CHECK (`status` <> 'failed' OR `failure_reason` IS NOT NULL),
  CONSTRAINT `chk_data_gen_task_success_finished` CHECK (`status` <> 'success' OR `finished_at` IS NOT NULL),
  CONSTRAINT `chk_data_gen_task_positive` CHECK (
    `group_count` > 0 AND `time_limit_ms` > 0 AND `memory_limit_mb` > 0
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='AI 造数据任务；保留完整参数与状态，文件单独清理';

CREATE TABLE IF NOT EXISTS `data_gen_file` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` BIGINT UNSIGNED NOT NULL COMMENT '任务 ID；逻辑关联 data_gen_task.id',
  `group_index` SMALLINT UNSIGNED NOT NULL COMMENT '数据组序号，从 1 开始',
  `input_path` VARCHAR(512) NULL COMMENT '输入文件相对路径；清理后仍保留元数据',
  `output_path` VARCHAR(512) NULL COMMENT '输出文件相对路径；清理后仍保留元数据',
  `input_size_bytes` BIGINT UNSIGNED NULL COMMENT '输入文件大小（字节）',
  `output_size_bytes` BIGINT UNSIGNED NULL COMMENT '输出文件大小（字节）',
  `std_time_ms` INT UNSIGNED NULL COMMENT 'std 运行时间（毫秒）',
  `validation_status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '校验状态：pending / passed / failed',
  `validation_message` TEXT NULL COMMENT '校验信息或失败原因',
  `file_deleted_at` DATETIME(3) NULL COMMENT '文件清理时间（UTC）；为空表示文件仍保留',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_gen_file_task_group` (`task_id`, `group_index`),
  KEY `idx_data_gen_file_validation` (`task_id`, `validation_status`),
  CONSTRAINT `chk_data_gen_file_group` CHECK (`group_index` > 0),
  CONSTRAINT `chk_data_gen_file_validation` CHECK (`validation_status` IN ('pending', 'passed', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='造数据结果文件元数据；文件清理后任务与元数据保留';

-- ============================================================
-- 七、后台支撑与审计（M09）
-- ============================================================

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `admin_id` BIGINT UNSIGNED NOT NULL COMMENT '操作管理员 ID；逻辑关联 admin_user.id',
  `action` VARCHAR(64) NOT NULL COMMENT '操作编码，例如 create / update / delete / enable / refresh_cf',
  `target_type` VARCHAR(32) NOT NULL COMMENT '目标类型，例如 route / problem / user / admin_user',
  `target_id` BIGINT UNSIGNED NULL COMMENT '目标 ID；登录等无具体目标时为空',
  `detail` TEXT NULL COMMENT '操作详情（人类可读）',
  `extra` JSON NULL COMMENT '结构化附加信息，例如变更字段、IP、来源页',
  `ip_address` VARCHAR(45) NULL COMMENT '客户端 IP；兼容 IPv6',
  `user_agent` VARCHAR(512) NULL COMMENT '客户端 User-Agent',
  `request_id` VARCHAR(64) NULL COMMENT '请求追踪 ID',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_admin_created` (`admin_id`, `created_at`),
  KEY `idx_operation_log_target` (`target_type`, `target_id`),
  KEY `idx_operation_log_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='后台操作审计日志；与业务写操作同事务写入，不可更新或删除';

-- ============================================================
-- 八、基础角色初始化
-- ============================================================

INSERT IGNORE INTO `admin_role` (`code`, `name`, `permissions`) VALUES
  ('super_admin', '超级管理员', JSON_ARRAY('*')),
  ('content_operator', '内容运营', JSON_ARRAY(
    'route:manage',
    'stage:manage',
    'course:manage',
    'problem:manage',
    'user:view',
    'leaderboard:view',
    'datagen:view',
    'operation_log:view'
  ));

-- ============================================================
-- 脚本结束
-- ============================================================
