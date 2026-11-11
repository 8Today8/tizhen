package com.tizhen.core.judge.model;

import com.tizhen.core.problem.model.ScoringRubricItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 提交详情。答案类字段仅在自己查看且判题/判分完成后返回。
 */
public record SubmissionDetailView(
        String id,
        SubmissionProblemBrief problem,
        String battleId,
        String language,
        String code,
        List<String> answerJson,
        String answerText,
        String status,
        String verdict,
        BigDecimal score,
        String aiFeedback,
        String failureReason,
        String judgeDetail,
        String compileOutput,
        Integer judgeTimeMs,
        Integer memoryKb,
        Instant judgeStartedAt,
        Instant judgedAt,
        Instant createdAt,
        boolean firstAc,
        List<String> correctOptions,
        String explanation,
        String referenceAnswer,
        List<ScoringRubricItem> scoringRubric
) {
}
