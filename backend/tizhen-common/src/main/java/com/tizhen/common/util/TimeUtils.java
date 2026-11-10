package com.tizhen.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 数据库时间统一按 UTC 的 LocalDateTime 存储，对外输出 Instant（带 Z）。
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    public static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    public static LocalDateTime toUtcLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
