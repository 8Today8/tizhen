package com.tizhen.core.learning.model;

/**
 * 路线阶段的完成统计。只统计当前仍发布的阶段。
 */
public class RouteProgressCount {

    private Long routeId;

    private Long stageCount;

    private Long completedStageCount;

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public Long getStageCount() {
        return stageCount;
    }

    public void setStageCount(Long stageCount) {
        this.stageCount = stageCount;
    }

    public Long getCompletedStageCount() {
        return completedStageCount;
    }

    public void setCompletedStageCount(Long completedStageCount) {
        this.completedStageCount = completedStageCount;
    }
}
