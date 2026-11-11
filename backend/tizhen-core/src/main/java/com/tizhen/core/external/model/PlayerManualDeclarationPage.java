package com.tizhen.core.external.model;

import java.time.LocalDate;
import java.util.List;

/**
 * 当前周手动申报分页结果。weekStartDate 作为同级字段返回，便于页面标注统计周。
 */
public record PlayerManualDeclarationPage(
        List<ManualDeclarationView> items,
        int page,
        int pageSize,
        long total,
        long totalPages,
        LocalDate weekStartDate
) {

    public PlayerManualDeclarationPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
