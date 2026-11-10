package com.tizhen.common.api;

import java.util.List;

/**
 * 统一分页响应。页码从 1 开始，pageSize 默认 20、最大 100。
 */
public record PageResult<T>(
        List<T> items,
        int page,
        int pageSize,
        long total,
        long totalPages
) {

    public PageResult {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> PageResult<T> of(List<T> items, int page, int pageSize, long total) {
        long totalPages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(items, page, pageSize, total, totalPages);
    }
}
