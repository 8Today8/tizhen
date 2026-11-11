package com.tizhen.core.judge.model;

import java.math.BigDecimal;

/**
 * worker 判题/判分结果。status 只允许 finished 或 failed。
 */
public record JudgeOutcome(
        String status,
        String verdict,
        BigDecimal score,
        String aiFeedback,
        String failureReason,
        String judgeDetail,
        String compileOutput,
        Integer judgeTimeMs,
        Integer memoryKb
) {

    public static JudgeOutcome finished(String verdict,
                                        BigDecimal score,
                                        String judgeDetail,
                                        String compileOutput,
                                        Integer judgeTimeMs,
                                        Integer memoryKb) {
        return new JudgeOutcome("finished", verdict, score, null,
                null, judgeDetail, compileOutput, judgeTimeMs, memoryKb);
    }

    public static JudgeOutcome graded(BigDecimal score,
                                      String verdict,
                                      String aiFeedback,
                                      String judgeDetail) {
        return new JudgeOutcome("finished", verdict, score, aiFeedback,
                null, judgeDetail, null, null, null);
    }

    public static JudgeOutcome failed(String failureReason) {
        return new JudgeOutcome("failed", null, null, null,
                failureReason, null, null, null, null);
    }

    public boolean isFinished() {
        return "finished".equals(status);
    }
}
