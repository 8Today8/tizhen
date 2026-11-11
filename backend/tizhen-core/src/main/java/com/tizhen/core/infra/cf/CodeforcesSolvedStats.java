package com.tizhen.core.infra.cf;

import java.time.LocalDate;

/**
 * Codeforces 过题统计：历史唯一 AC 题数与北京时间当前自然周内首次 AC 题数。
 *
 * @param allTimeSolved 历史唯一 AC 题数
 * @param weeklySolved  当前自然周内首次 AC 题数
 * @param weekStartDate 统计周的周一日期
 */
public record CodeforcesSolvedStats(int allTimeSolved, int weeklySolved, LocalDate weekStartDate) {
}
