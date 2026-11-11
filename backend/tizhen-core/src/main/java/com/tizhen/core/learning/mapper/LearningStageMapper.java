package com.tizhen.core.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.core.learning.entity.LearningStage;
import com.tizhen.core.learning.model.AdminStageRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学习阶段访问。列表一次带出所属路线、课程数与题目数。
 */
@Mapper
public interface LearningStageMapper extends BaseMapper<LearningStage> {

    String STAGE_SUMMARY_COLUMNS = """
            s.id, s.route_id, r.name AS route_name, s.name, s.sort_order, s.status,
            (SELECT COUNT(*) FROM stage_course c
              WHERE c.stage_id = s.id AND c.deleted_at IS NULL) AS course_count,
            (SELECT COUNT(*) FROM stage_problem sp
              JOIN problem p ON p.id = sp.problem_id AND p.deleted_at IS NULL
              WHERE sp.stage_id = s.id) AS problem_count,
            s.created_at, s.updated_at
            """;

    @Select("""
            <script>
            SELECT
            """ + STAGE_SUMMARY_COLUMNS + """
            FROM learning_stage s
            JOIN learning_route r ON r.id = s.route_id AND r.deleted_at IS NULL
            WHERE s.deleted_at IS NULL
            <if test="routeId != null">
              AND s.route_id = #{routeId}
            </if>
            <if test="status != null and status != ''">
              AND s.status = #{status}
            </if>
            <if test="keyword != null and keyword != ''">
              AND (s.name LIKE CONCAT('%', #{keyword}, '%')
                   OR s.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY r.sort_order ASC, r.id ASC, s.sort_order ASC, s.id ASC
            </script>
            """)
    IPage<AdminStageRow> selectAdminStagePage(Page<AdminStageRow> page,
                                              @Param("routeId") Long routeId,
                                              @Param("keyword") String keyword,
                                              @Param("status") String status);

    @Select("""
            SELECT
            """ + STAGE_SUMMARY_COLUMNS + """
            FROM learning_stage s
            JOIN learning_route r ON r.id = s.route_id AND r.deleted_at IS NULL
            WHERE s.route_id = #{routeId} AND s.deleted_at IS NULL
            ORDER BY s.sort_order ASC, s.id ASC
            """)
    List<AdminStageRow> selectAdminStageRowsByRouteId(@Param("routeId") Long routeId);

    @Select("""
            SELECT
            """ + STAGE_SUMMARY_COLUMNS + """
            FROM learning_stage s
            JOIN learning_route r ON r.id = s.route_id AND r.deleted_at IS NULL
            WHERE s.id = #{stageId} AND s.deleted_at IS NULL
            """)
    AdminStageRow selectAdminStageRow(@Param("stageId") Long stageId);
}
