package com.tizhen.core.judge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.mapper.SubmissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * worker 任务领取与崩溃恢复。领取和状态迁移都在事务内完成，
 * 使用 SKIP LOCKED 避免多执行器重复处理同一条提交。
 */
@Service
public class SubmissionTaskService {

    private static final String TYPE_OJ = "oj";
    private static final String TYPE_SHORT_ANSWER = "short_answer";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_JUDGING = "judging";
    private static final String STATUS_GRADING = "grading";
    private static final String STATUS_FAILED = "failed";
    private static final String FAILURE_REASON = "判题服务异常，请稍后重新提交";

    private final SubmissionMapper submissionMapper;

    public SubmissionTaskService(SubmissionMapper submissionMapper) {
        this.submissionMapper = submissionMapper;
    }

    @Transactional
    public List<Submission> claimOjTasks(int limit) {
        List<Submission> rows = submissionMapper.selectPendingOjForUpdate(limit);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (Submission row : rows) {
            row.setStatus(STATUS_JUDGING);
            row.setJudgeStartedAt(now);
            row.setJudgeAttempts(nextAttempt(row.getJudgeAttempts()));
            submissionMapper.updateById(row);
        }
        return rows;
    }

    @Transactional
    public List<Submission> claimShortAnswerTasks(int limit) {
        List<Submission> rows = submissionMapper.selectPendingShortAnswerForUpdate(limit);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (Submission row : rows) {
            row.setJudgeStartedAt(now);
            row.setJudgeAttempts(nextAttempt(row.getJudgeAttempts()));
            submissionMapper.updateById(row);
        }
        return rows;
    }

    /**
     * 按 id 领取任务。allowInProgress 用于重试消息与 broker 重投：
     * 正常消息只领取 pending，重试消息允许接管仍在执行态的任务。
     */
    @Transactional
    public Submission claimById(Long submissionId, boolean allowInProgress, int maxAttempts) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int updated = allowInProgress
                ? submissionMapper.claimInProgressById(submissionId, now, maxAttempts)
                : submissionMapper.claimPendingById(submissionId, now);
        return updated == 1 ? submissionMapper.selectById(submissionId) : null;
    }

    /**
     * 释放长时间停留在进行中的任务。未超过尝试上限时重新排队，
     * 超过上限则标记失败，避免用户一直看到“判题中”。
     */
    @Transactional
    public void recoverStale(LocalDateTime threshold, int maxAttempts) {
        List<Submission> rows = submissionMapper.selectStaleInProgress(threshold, 100);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (Submission row : rows) {
            int attempts = row.getJudgeAttempts() == null ? 0 : row.getJudgeAttempts();
            var update = Wrappers.<Submission>lambdaUpdate()
                    .eq(Submission::getId, row.getId())
                    .eq(Submission::getStatus, row.getStatus())
                    .eq(Submission::getJudgeStartedAt, row.getJudgeStartedAt());
            if (attempts >= maxAttempts) {
                update.set(Submission::getStatus, STATUS_FAILED)
                        .set(Submission::getVerdict, null)
                        .set(Submission::getFailureReason, FAILURE_REASON)
                        .set(Submission::getJudgedAt, now)
                        .set(Submission::getUpdatedAt, now);
            } else if (TYPE_SHORT_ANSWER.equals(row.getProblemType())) {
                update.set(Submission::getJudgeStartedAt, null)
                        .set(Submission::getUpdatedAt, now);
            } else if (TYPE_OJ.equals(row.getProblemType())) {
                update.set(Submission::getStatus, STATUS_PENDING)
                        .set(Submission::getJudgeStartedAt, null)
                        .set(Submission::getUpdatedAt, now);
            } else {
                continue;
            }
            submissionMapper.update(null, update);
        }
    }

    private int nextAttempt(Integer attempts) {
        return attempts == null ? 1 : attempts + 1;
    }
}
