package com.tizhen.core.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.core.problem.entity.Problem;
import com.tizhen.core.problem.model.AdminProblemUsage;
import com.tizhen.core.problem.model.ClientProblemRow;
import com.tizhen.core.problem.model.ProblemTagCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题库读取。后台列表只读摘要；选手端额外计算当前用户的作答状态，
 * 但任何查询都不返回测试数据、答案与评分要点。
 */
@Mapper
public interface ProblemMapper extends BaseMapper<Problem> {

    @Select("""
            <script>
            SELECT p.id, p.type, p.objective_type, p.title, p.difficulty,
                   p.source, p.platform, p.status, p.created_at, p.updated_at
            FROM problem p
            WHERE p.deleted_at IS NULL
            <if test="type != null and type != ''">
              AND p.type = #{type}
            </if>
            <if test="objectiveType != null and objectiveType != ''">
              AND p.objective_type = #{objectiveType}
            </if>
            <if test="difficulty != null and difficulty != ''">
              AND p.difficulty = #{difficulty}
            </if>
            <if test="status != null and status != ''">
              AND p.status = #{status}
            </if>
            <if test="source != null and source != ''">
              AND p.source = #{source}
            </if>
            <if test="tag != null and tag != ''">
              AND EXISTS (
                SELECT 1 FROM problem_tag pt
                WHERE pt.problem_id = p.id AND pt.tag_type = 'topic' AND pt.tag_value = #{tag}
              )
            </if>
            <if test="keyword != null and keyword != ''">
              AND p.title LIKE CONCAT('%', #{keyword}, '%')
            </if>
            ORDER BY p.updated_at DESC, p.id DESC
            </script>
            """)
    IPage<Problem> selectAdminProblemPage(Page<Problem> page,
                                          @Param("type") String type,
                                          @Param("objectiveType") String objectiveType,
                                          @Param("difficulty") String difficulty,
                                          @Param("status") String status,
                                          @Param("source") String source,
                                          @Param("tag") String tag,
                                          @Param("keyword") String keyword);

    /**
     * 一次查询题目在阶段、提交、首次 AC、错题本、对战和外链完成记录中的引用次数。
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM stage_problem WHERE problem_id = #{problemId}) AS stage_count,
              (SELECT COUNT(*) FROM submission WHERE problem_id = #{problemId}) AS submission_count,
              (SELECT COUNT(*) FROM first_ac WHERE problem_id = #{problemId}) AS first_ac_count,
              (SELECT COUNT(*) FROM wrong_book WHERE problem_id = #{problemId}) AS wrong_book_count,
              (SELECT COUNT(*) FROM battle_room WHERE problem_id = #{problemId}) AS battle_count,
              (SELECT COUNT(*) FROM external_completion WHERE problem_id = #{problemId})
                AS external_completion_count
            """)
    AdminProblemUsage selectUsage(@Param("problemId") Long problemId);

    @Select("""
            <script>
            SELECT p.id, p.type, p.objective_type, p.title, p.difficulty, p.source,
                   p.platform, p.external_url,
                   EXISTS (
                     SELECT 1 FROM first_ac fa
                     WHERE fa.user_id = #{userId} AND fa.problem_id = p.id
                   ) AS solved_by_me,
                   EXISTS (
                     SELECT 1 FROM submission s
                     WHERE s.user_id = #{userId} AND s.problem_id = p.id
                   ) AS attempted_by_me,
                   (
                     SELECT ec.completed FROM external_completion ec
                     WHERE ec.user_id = #{userId} AND ec.problem_id = p.id
                     LIMIT 1
                   ) AS external_completed
            FROM problem p
            WHERE p.deleted_at IS NULL AND p.status = 'published'
            <if test="type != null and type != ''">
              AND p.type = #{type}
            </if>
            <if test="objectiveType != null and objectiveType != ''">
              AND p.objective_type = #{objectiveType}
            </if>
            <if test="difficulty != null and difficulty != ''">
              AND p.difficulty = #{difficulty}
            </if>
            <if test="source != null and source != ''">
              AND p.source = #{source}
            </if>
            <if test="tag != null and tag != ''">
              AND EXISTS (
                SELECT 1 FROM problem_tag pt
                WHERE pt.problem_id = p.id AND pt.tag_type = 'topic' AND pt.tag_value = #{tag}
              )
            </if>
            <if test="keyword != null and keyword != ''">
              AND p.title LIKE CONCAT('%', #{keyword}, '%')
            </if>
            ORDER BY p.id DESC
            </script>
            """)
    IPage<ClientProblemRow> selectPublishedProblemPage(Page<ClientProblemRow> page,
                                                       @Param("userId") Long userId,
                                                       @Param("type") String type,
                                                       @Param("objectiveType") String objectiveType,
                                                       @Param("difficulty") String difficulty,
                                                       @Param("source") String source,
                                                       @Param("tag") String tag,
                                                       @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT p.id, p.type, p.objective_type, p.title, p.statement, p.input_format,
                   p.output_format, p.difficulty, p.source, p.platform, p.external_url,
                   p.time_limit_ms, p.memory_limit_mb, p.max_score,
                   EXISTS (
                     SELECT 1 FROM first_ac fa
                     WHERE fa.user_id = #{userId} AND fa.problem_id = p.id
                   ) AS solved_by_me,
                   EXISTS (
                     SELECT 1 FROM submission s
                     WHERE s.user_id = #{userId} AND s.problem_id = p.id
                   ) AS attempted_by_me,
                   (
                     SELECT ec.completed FROM external_completion ec
                     WHERE ec.user_id = #{userId} AND ec.problem_id = p.id
                     LIMIT 1
                   ) AS external_completed,
                   (
                     SELECT fa.first_ac_at FROM first_ac fa
                     WHERE fa.user_id = #{userId} AND fa.problem_id = p.id
                     LIMIT 1
                   ) AS first_ac_at,
                   (
                     SELECT MAX(s.created_at) FROM submission s
                     WHERE s.user_id = #{userId} AND s.problem_id = p.id
                   ) AS last_submitted_at
            FROM problem p
            WHERE p.id = #{problemId}
              AND p.deleted_at IS NULL AND p.status = 'published'
            </script>
            """)
    ClientProblemRow selectPublishedProblemRow(@Param("userId") Long userId,
                                               @Param("problemId") Long problemId);

    @Select("""
            <script>
            SELECT p.id, p.type, p.objective_type, p.title, p.difficulty, p.source,
                   p.platform, p.external_url,
                   EXISTS (
                     SELECT 1 FROM first_ac fa
                     WHERE fa.user_id = #{userId} AND fa.problem_id = p.id
                   ) AS solved_by_me,
                   EXISTS (
                     SELECT 1 FROM submission s
                     WHERE s.user_id = #{userId} AND s.problem_id = p.id
                   ) AS attempted_by_me,
                   (
                     SELECT ec.completed FROM external_completion ec
                     WHERE ec.user_id = #{userId} AND ec.problem_id = p.id
                     LIMIT 1
                   ) AS external_completed
            FROM problem p
            WHERE p.deleted_at IS NULL AND p.status = 'published'
            <if test="type != null and type != ''">
              AND p.type = #{type}
            </if>
            <if test="difficulty != null and difficulty != ''">
              AND p.difficulty = #{difficulty}
            </if>
            <if test="tag != null and tag != ''">
              AND EXISTS (
                SELECT 1 FROM problem_tag pt
                WHERE pt.problem_id = p.id AND pt.tag_type = 'topic' AND pt.tag_value = #{tag}
              )
            </if>
            ORDER BY RAND()
            LIMIT 1
            </script>
            """)
    ClientProblemRow selectRandomPublishedProblem(@Param("userId") Long userId,
                                                  @Param("type") String type,
                                                  @Param("difficulty") String difficulty,
                                                  @Param("tag") String tag);

    @Select("""
            SELECT pt.tag_value AS value, COUNT(DISTINCT pt.problem_id) AS tag_count
            FROM problem_tag pt
            JOIN problem p ON p.id = pt.problem_id
            WHERE pt.tag_type = 'topic'
              AND p.deleted_at IS NULL AND p.status = 'published'
            GROUP BY pt.tag_value
            ORDER BY tag_count DESC, value ASC
            """)
    List<ProblemTagCountRow> selectPublishedTagCounts();
}
