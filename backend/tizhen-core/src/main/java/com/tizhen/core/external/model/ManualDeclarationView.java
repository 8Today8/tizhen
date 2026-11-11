package com.tizhen.core.external.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 选手本人的手动申报记录。审核信息仅后台可见，这里只暴露状态。
 */
public record ManualDeclarationView(
        String id,
        String platform,
        String account,
        String profileUrl,
        int count,
        LocalDate weekStartDate,
        String verificationStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
