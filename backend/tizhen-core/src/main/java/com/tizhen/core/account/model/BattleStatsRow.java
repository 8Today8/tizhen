package com.tizhen.core.account.model;

/**
 * 对战统计聚合行，由 MyBatis 映射。
 */
public class BattleStatsRow {

    private Long total;
    private Long wins;
    private Long losses;
    private Long draws;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getWins() {
        return wins;
    }

    public void setWins(Long wins) {
        this.wins = wins;
    }

    public Long getLosses() {
        return losses;
    }

    public void setLosses(Long losses) {
        this.losses = losses;
    }

    public Long getDraws() {
        return draws;
    }

    public void setDraws(Long draws) {
        this.draws = draws;
    }
}
