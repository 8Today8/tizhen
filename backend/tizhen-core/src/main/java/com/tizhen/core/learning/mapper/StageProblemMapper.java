package com.tizhen.core.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.learning.entity.StageProblem;
import com.tizhen.core.learning.model.AdminStageProblemRow;
import com.tizhen.core.learning.model.ClientStageProblemRow;
import com.tizhen.core.learning.model.StageProblemCount;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 阶段题目关联访问。替换关联时先物理删除旧记录，再批量插入。
 */
@Mapper
public interface StageProblemMapper extends BaseMapper<StageProblem> {

    @Delete("DELETE FROM stage_problem WHERE stage_id = #{stageId}")
    int deleteByStageId(@Param("stageId") Long stageId);

    @Select("""
            SELECT sp.problem_id, sp.sort_order,
                   p.type, p.objective_type, p.title, p.difficulty, p.source, p.platform, p.status
            FROM stage_problem sp
            JOIN problem p ON p.id = sp.problem_id AND p.deleted_at IS NULL
            WHERE sp.stage_id = #{stageId}
            ORDER BY sp.sort_order ASC, sp.id ASC
            """)
    List<AdminStageProblemRow> selectAdminStageProblemRows(@Param("stageId") Long stageId);

    @Select("""
            <script>
            SELECT sp.stage_id, COUNT(*) AS problem_count
            FROM stage_problem sp
            JOIN problem p
              ON p.id = sp.problem_id
             AND p.deleted_at IS NULL
             AND p.status = 'published'
            WHERE sp.stage_id IN
              <foreach collection="stageIds" item="stageId" open="(" separator="," close=")">
                #{stageId}
              </foreach>
            GROUP BY sp.stage_id
            </script>
            """)
    List<StageProblemCount> countPublishedProblemsByStageIds(@Param("stageIds") List<Long> stageIds);

    @Select("""
            SELECT sp.problem_id, sp.sort_order,
                   p.type, p.objective_type, p.title, p.difficulty, p.source, p.platform,
                   p.external_url, ec.completed AS external_completed,
                   ec.completed_at AS external_completed_at
            FROM stage_problem sp
            JOIN problem p
              ON p.id = sp.problem_id
             AND p.deleted_at IS NULL
             AND p.status = 'published'
            LEFT JOIN external_completion ec
              ON ec.problem_id = p.id AND ec.user_id = #{userId}
            WHERE sp.stage_id = #{stageId}
            ORDER BY sp.sort_order ASC, sp.id ASC
            """)
    List<ClientStageProblemRow> selectPublishedStageProblems(@Param("stageId") Long stageId,
                                                             @Param("userId") Long userId);
}
