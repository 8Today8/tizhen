package com.tizhen.core.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.learning.entity.LearningProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 学习进度访问。用于路线/阶段删除前的引用保护，以及选手阶段完成状态。
 */
@Mapper
public interface LearningProgressMapper extends BaseMapper<LearningProgress> {

    @Select("SELECT COUNT(*) FROM learning_progress WHERE stage_id = #{stageId}")
    long countByStageId(@Param("stageId") Long stageId);

    @Select("""
            SELECT COUNT(*)
            FROM learning_progress lp
            JOIN learning_stage s ON s.id = lp.stage_id
            WHERE s.route_id = #{routeId}
            """)
    long countByRouteId(@Param("routeId") Long routeId);
}
