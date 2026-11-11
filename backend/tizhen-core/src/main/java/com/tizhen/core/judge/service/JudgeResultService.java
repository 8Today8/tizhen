package com.tizhen.core.judge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tizhen.core.battle.service.BattleLifecycleService;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.mapper.FirstAcMapper;
import com.tizhen.core.judge.mapper.SubmissionMapper;
import com.tizhen.core.judge.mapper.WrongBookMapper;
import com.tizhen.core.judge.model.JudgeOutcome;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 判题结果落库、首次 AC 与错题本写入。更新带状态条件，
 * 确保重复投递或崩溃恢复后的旧结果不会覆盖终态。
 */
@Service
public class JudgeResultService {

    private static final String TYPE_OJ = "oj";
    private static final String TYPE_SHORT_ANSWER = "short_answer";
    private static final String STATUS_FINISHED = "finished";
    private static final String VERDICT_AC = "AC";
    private static final BigDecimal PASS_SCORE = BigDecimal.valueOf(60);

    private final SubmissionMapper submissionMapper;
    private final FirstAcMapper firstAcMapper;
    private final WrongBookMapper wrongBookMapper;
    private final BattleLifecycleService battleLifecycleService;

    public JudgeResultService(SubmissionMapper submissionMapper,
                              FirstAcMapper firstAcMapper,
                              WrongBookMapper wrongBookMapper,
                              BattleLifecycleService battleLifecycleService) {
        this.submissionMapper = submissionMapper;
        this.firstAcMapper = firstAcMapper;
        this.wrongBookMapper = wrongBookMapper;
        this.battleLifecycleService = battleLifecycleService;
    }

    /**
     * @return 是否由本次调用完成终态迁移
     */
    @Transactional
    public boolean complete(Long submissionId, JudgeOutcome outcome) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int updated = submissionMapper.update(null, Wrappers.<Submission>lambdaUpdate()
                .eq(Submission::getId, submissionId)
                .in(Submission::getStatus, "pending", "judging", "grading")
                .set(Submission::getStatus, outcome.status())
                .set(Submission::getVerdict, outcome.verdict())
                .set(Submission::getScore, outcome.score())
                .set(Submission::getAiFeedback, outcome.aiFeedback())
                .set(Submission::getFailureReason, outcome.failureReason())
                .set(Submission::getJudgeDetail, outcome.judgeDetail())
                .set(Submission::getCompileOutput, outcome.compileOutput())
                .set(Submission::getJudgeTimeMs, outcome.judgeTimeMs())
                .set(Submission::getMemoryKb, outcome.memoryKb())
                .set(Submission::getJudgedAt, now)
                .set(Submission::getUpdatedAt, now));
        if (updated == 0 || !outcome.isFinished()) {
            return updated > 0;
        }

        if (TYPE_OJ.equals(submission.getProblemType()) && VERDICT_AC.equals(outcome.verdict())) {
            firstAcMapper.insertIgnore(submission.getUserId(), submission.getProblemId(),
                    submissionId, now);
        }

        if (submission.getBattleId() != null) {
            battleLifecycleService.onSubmissionFinished(submission, outcome);
        }

        if (TYPE_OJ.equals(submission.getProblemType())
                && !VERDICT_AC.equals(outcome.verdict())) {
            wrongBookMapper.upsert(submission.getUserId(), submission.getProblemId(),
                    outcome.verdict(), null, now);
        } else if (TYPE_SHORT_ANSWER.equals(submission.getProblemType())
                && outcome.score() != null
                && outcome.score().compareTo(PASS_SCORE) < 0) {
            wrongBookMapper.upsert(submission.getUserId(), submission.getProblemId(),
                    "AI " + formatScore(outcome.score()), outcome.score(), now);
        }
        return true;
    }

    private String formatScore(BigDecimal score) {
        BigDecimal normalized = score.stripTrailingZeros();
        return normalized.scale() <= 0
                ? normalized.toBigInteger().toString()
                : normalized.toPlainString();
    }
}
