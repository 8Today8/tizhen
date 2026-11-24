package com.tizhen.core.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.model.ProblemAttemptStats;
import com.tizhen.core.judge.model.SubmissionListRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提交记录读取与 worker 任务领取。领取语句使用 SKIP LOCKED，
 * 保证多 worker 并发时同一条提交只被一个执行器处理。
 */
@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

    @Select("""
            <script>
            SELECT s.id, s.problem_id, s.problem_type, p.title AS problem_title,
                   s.battle_id, s.language, s.status, s.verdict, s.score,
                   s.created_at, s.judged_at
            FROM submission s
            LEFT JOIN problem p ON p.id = s.problem_id
            WHERE s.user_id = #{userId}
            <if test="problemId != null">
              AND s.problem_id = #{problemId}
            </if>
            <if test="problemType != null and problemType != ''">
              AND s.problem_type = #{problemType}
            </if>
            <if test="status != null and status != ''">
              AND s.status = #{status}
            </if>
            ORDER BY s.created_at DESC, s.id DESC
            </script>
            """)
    IPage<SubmissionListRow> selectUserSubmissionPage(Page<SubmissionListRow> page,
                                                      @Param("userId") Long userId,
                                                      @Param("problemId") Long problemId,
                                                      @Param("problemType") String problemType,
                                                      @Param("status") String status);

    /**
     * 汇总用户在某题上的作答进度。最近结果取最后一条提交，未完成时为 null。
     */
    @Select("""
            SELECT COUNT(*) AS attempt_count,
                   (
                     SELECT s2.verdict FROM submission s2
                     WHERE s2.user_id = #{userId} AND s2.problem_id = #{problemId}
                     ORDER BY s2.created_at DESC, s2.id DESC
                     LIMIT 1
                   ) AS last_verdict,
                   MAX(created_at) AS last_submitted_at
            FROM submission
            WHERE user_id = #{userId} AND problem_id = #{problemId}
            """)
    ProblemAttemptStats selectAttemptStats(@Param("userId") Long userId,
                                           @Param("problemId") Long problemId);

    @Select("""
            SELECT * FROM submission
            WHERE problem_type = 'oj' AND status = 'pending'
            ORDER BY created_at ASC, id ASC
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<Submission> selectPendingOjForUpdate(@Param("limit") int limit);

    @Select("""
            SELECT * FROM submission
            WHERE problem_type = 'short_answer' AND status = 'grading'
              AND judge_started_at IS NULL
            ORDER BY created_at ASC, id ASC
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<Submission> selectPendingShortAnswerForUpdate(@Param("limit") int limit);

    @Select("""
            SELECT * FROM submission
            WHERE status IN ('judging', 'grading')
              AND judge_started_at IS NOT NULL
              AND judge_started_at < #{threshold}
            ORDER BY judge_started_at ASC
            LIMIT #{limit}
            """)
    List<Submission> selectStaleInProgress(@Param("threshold") LocalDateTime threshold,
                                           @Param("limit") int limit);

    /**
     * 按 id 条件领取待处理任务，供 MQ 消费端使用。更新 0 行表示已被处理或重复投递。
     */
    @Update("""
            UPDATE submission
            SET status = CASE WHEN problem_type = 'oj' THEN 'judging' ELSE status END,
                judge_started_at = #{now},
                judge_attempts = judge_attempts + 1,
                updated_at = #{now}
            WHERE id = #{id}
              AND ((problem_type = 'oj' AND status = 'pending')
                OR (problem_type = 'short_answer' AND status = 'grading'
                    AND judge_started_at IS NULL))
            """)
    int claimPendingById(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 领取一条仍在执行中的任务，用于重试消息和 broker 重投。
     * 只有未达到尝试上限时才允许占用，防止重复消息无限执行。
     */
    @Update("""
            UPDATE submission
            SET judge_started_at = #{now},
                judge_attempts = judge_attempts + 1,
                updated_at = #{now}
            WHERE id = #{id}
              AND judge_attempts < #{maxAttempts}
              AND ((problem_type = 'oj' AND status = 'judging')
                OR (problem_type = 'short_answer' AND status = 'grading'))
            """)
    int claimInProgressById(@Param("id") Long id,
                            @Param("now") LocalDateTime now,
                            @Param("maxAttempts") int maxAttempts);

    /**
     * 补偿扫描：捞取落库超过阈值仍未进入执行态的任务。
     */
    @Select("""
            SELECT id, problem_type FROM submission
            WHERE ((problem_type = 'oj' AND status = 'pending')
                OR (problem_type = 'short_answer' AND status = 'grading'
                    AND judge_started_at IS NULL))
              AND created_at < #{threshold}
            ORDER BY created_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<Submission> selectUndispatched(@Param("threshold") LocalDateTime threshold,
                                        @Param("limit") int limit);
}
