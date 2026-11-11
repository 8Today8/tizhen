package com.tizhen.core.learning.model;

/**
 * 阶段内已发布题目数量。
 */
public class StageProblemCount {

    private Long stageId;

    private Long problemCount;

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public Long getProblemCount() {
        return problemCount;
    }

    public void setProblemCount(Long problemCount) {
        this.problemCount = problemCount;
    }
}
