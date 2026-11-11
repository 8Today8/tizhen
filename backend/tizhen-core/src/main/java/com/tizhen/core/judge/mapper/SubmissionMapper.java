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
}
