-- 手动申报审核：未审核不计分，审核通过后计入榜单，拒绝保留记录但不计分。
ALTER TABLE `manual_declaration`
  DROP CHECK `chk_manual_declaration_verification`,
  MODIFY COLUMN `verification_status` VARCHAR(16) NOT NULL DEFAULT 'unverified'
    COMMENT '校验状态：unverified / verified / rejected',
  ADD COLUMN `review_note` VARCHAR(200) NULL COMMENT '审核备注（仅后台可见）' AFTER `verification_status`,
  ADD COLUMN `reviewed_by` BIGINT UNSIGNED NULL COMMENT '审核管理员 ID；逻辑关联 admin_user.id' AFTER `review_note`,
  ADD COLUMN `reviewed_at` DATETIME(3) NULL COMMENT '审核时间（UTC）' AFTER `reviewed_by`,
  ADD KEY `idx_manual_declaration_review`
    (`verification_status`, `active_flag`, `week_start_date`, `user_id`);

ALTER TABLE `manual_declaration`
  ADD CONSTRAINT `chk_manual_declaration_verification`
    CHECK (`verification_status` IN ('unverified', 'verified', 'rejected'));
