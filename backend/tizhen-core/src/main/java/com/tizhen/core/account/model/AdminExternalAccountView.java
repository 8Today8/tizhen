package com.tizhen.core.account.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 用户详情中的 CF 绑定历史。
 */
public record AdminExternalAccountView(
        String id,
        String platform,
        String handle,
        String profileUrl,
        String verificationStatus,
        String dataSource,
        Integer weeklyCount,
        LocalDate weeklyWeekStart,
        Integer allTimeCount,
        Instant lastSyncAt,
        String status,
        Instant unboundAt,
        Instant createdAt
) {
}
