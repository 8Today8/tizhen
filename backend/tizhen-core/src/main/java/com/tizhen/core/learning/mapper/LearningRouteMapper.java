package com.tizhen.core.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.core.learning.entity.LearningRoute;
import com.tizhen.core.learning.model.AdminRouteRow;
import com.tizhen.core.learning.model.RouteProgressCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学习路线访问。列表一次带出阶段数，避免逐行统计。
 */
@Mapper
public interface LearningRouteMapper extends BaseMapper<LearningRoute> {

    @Select("""
            <script>
            SELECT r.id, r.name, r.description, r.cover_url, r.sort_order, r.status,
                   (SELECT COUNT(*) FROM learning_stage s
                     WHERE s.route_id = r.id AND s.deleted_at IS NULL) AS stage_count,
                   r.created_at, r.updated_at
            FROM learning_route r
            WHERE r.deleted_at IS NULL
            <if test="status != null and status != ''">
              AND r.status = #{status}
            </if>
            <if test="keyword != null and keyword != ''">
              AND (r.name LIKE CONCAT('%', #{keyword}, '%')
                   OR r.description LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY r.sort_order ASC, r.id ASC
            </script>
            """)
    IPage<AdminRouteRow> selectAdminRoutePage(Page<AdminRouteRow> page,
                                              @Param("keyword") String keyword,
                                              @Param("status") String status);

    @Select("""
            SELECT r.id, r.name, r.description, r.cover_url, r.sort_order, r.status,
                   (SELECT COUNT(*) FROM learning_stage s
                     WHERE s.route_id = r.id AND s.deleted_at IS NULL) AS stage_count,
                   r.created_at, r.updated_at
            FROM learning_route r
            WHERE r.id = #{id} AND r.deleted_at IS NULL
            """)
    AdminRouteRow selectAdminRouteRow(@Param("id") Long id);

    /**
     * 批量统计当前选手在各路线下已完成、且当前仍发布的阶段数。
     */
    @Select("""
            <script>
            SELECT s.route_id,
                   COUNT(*) AS stage_count,
                   COUNT(CASE WHEN lp.completed = 1 THEN 1 END) AS completed_stage_count
            FROM learning_stage s
            LEFT JOIN learning_progress lp
              ON lp.stage_id = s.id AND lp.user_id = #{userId}
            WHERE s.deleted_at IS NULL
              AND s.status = 'published'
              AND s.route_id IN
              <foreach collection="routeIds" item="routeId" open="(" separator="," close=")">
                #{routeId}
              </foreach>
            GROUP BY s.route_id
            </script>
            """)
    List<RouteProgressCount> selectRouteProgressCounts(@Param("userId") Long userId,
                                                       @Param("routeIds") List<Long> routeIds);
}
