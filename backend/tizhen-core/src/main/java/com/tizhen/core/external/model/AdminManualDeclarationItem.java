package com.tizhen.core.external.model;

import com.tizhen.core.account.model.UserBrief;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 后台手动申报记录。包含审核信息与软删除记录的有效标记。
 */
public record AdminManualDeclarationItem(
        String id,
        UserBrief user,
        String username,
        String platform,
        String account,
        String profileUrl,
        int count,
        LocalDate weekStartDate,
        String verificationStatus,
        String reviewNote,
        String reviewedBy,
        String reviewedByName,
        Instant reviewedAt,
        boolean activeFlag,
        Instant createdAt,
        Instant updatedAt
) {
}
