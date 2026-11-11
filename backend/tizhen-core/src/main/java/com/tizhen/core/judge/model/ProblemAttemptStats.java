package com.tizhen.core.judge.model;

import java.time.LocalDateTime;

/**
 * 用户在某题上的提交聚合统计。
 */
public class ProblemAttemptStats {

    private Long attemptCount;
    private String lastVerdict;
    private LocalDateTime lastSubmittedAt;

    public Long getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Long attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getLastVerdict() {
        return lastVerdict;
    }

    public void setLastVerdict(String lastVerdict) {
        this.lastVerdict = lastVerdict;
    }

    public LocalDateTime getLastSubmittedAt() {
        return lastSubmittedAt;
    }

    public void setLastSubmittedAt(LocalDateTime lastSubmittedAt) {
        this.lastSubmittedAt = lastSubmittedAt;
    }
}
