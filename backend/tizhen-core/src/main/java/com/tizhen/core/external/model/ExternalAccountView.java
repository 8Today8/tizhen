package com.tizhen.core.external.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 当前用户的单个外部平台账号。仅返回有效绑定。
 */
public record ExternalAccountView(
        String id,
        String platform,
        String handle,
        String profileUrl,
        String verificationStatus,
        String dataSource,
        int weeklyCount,
        LocalDate weeklyWeekStart,
        int allTimeCount,
        Instant lastSyncAt,
        String status,
        Instant unboundAt,
        Instant createdAt
) {
}
