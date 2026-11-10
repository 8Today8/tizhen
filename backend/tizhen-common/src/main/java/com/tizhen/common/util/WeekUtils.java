package com.tizhen.common.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * 业务自然周计算：数据库按 UTC 存储，周口径统一使用 Asia/Shanghai。
 */
public final class WeekUtils {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private WeekUtils() {
    }

    public static LocalDate weekStartOf(Instant instant) {
        return instant.atZone(BUSINESS_ZONE)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate currentWeekStart() {
        return weekStartOf(Instant.now());
    }
}
