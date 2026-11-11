package com.tizhen.core.problem.model;

/**
 * 题目引用统计。由一次带子查询的查询加载，用于详情展示与删除判断。
 */
public class AdminProblemUsage {

    private Long stageCount;

    private Long submissionCount;

    private Long firstAcCount;

    private Long wrongBookCount;

    private Long battleCount;

    private Long externalCompletionCount;

    public Long getStageCount() {
        return stageCount;
    }

    public void setStageCount(Long stageCount) {
        this.stageCount = stageCount;
    }

    public Long getSubmissionCount() {
        return submissionCount;
    }

    public void setSubmissionCount(Long submissionCount) {
        this.submissionCount = submissionCount;
    }

    public Long getFirstAcCount() {
        return firstAcCount;
    }

    public void setFirstAcCount(Long firstAcCount) {
        this.firstAcCount = firstAcCount;
    }

    public Long getWrongBookCount() {
        return wrongBookCount;
    }

    public void setWrongBookCount(Long wrongBookCount) {
        this.wrongBookCount = wrongBookCount;
    }

    public Long getBattleCount() {
        return battleCount;
    }

    public void setBattleCount(Long battleCount) {
        this.battleCount = battleCount;
    }

    public Long getExternalCompletionCount() {
        return externalCompletionCount;
    }

    public void setExternalCompletionCount(Long externalCompletionCount) {
        this.externalCompletionCount = externalCompletionCount;
    }

    public boolean hasReferences() {
        return value(stageCount) > 0
                || value(submissionCount) > 0
                || value(firstAcCount) > 0
                || value(wrongBookCount) > 0
                || value(battleCount) > 0
                || value(externalCompletionCount) > 0;
    }

    private long value(Long count) {
        return count == null ? 0L : count;
    }
}
