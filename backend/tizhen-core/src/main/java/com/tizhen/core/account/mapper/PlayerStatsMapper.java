package com.tizhen.core.account.mapper;

import com.tizhen.core.account.model.BattleStatsRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 个人统计的只读聚合查询。学习统计只计入当前仍发布的内容。
 */
@Mapper
public interface PlayerStatsMapper {

    @Select("""
            SELECT COUNT(*)
            FROM submission
            WHERE user_id = #{userId} AND problem_type = #{problemType}
            """)
    long countSubmissionsByType(@Param("userId") Long userId,
                                @Param("problemType") String problemType);

    @Select("""
            SELECT COUNT(*) AS total,
                   COALESCE(SUM(CASE
                     WHEN (creator_id = #{userId} AND result = 'creator_win')
                       OR (opponent_id = #{userId} AND result = 'opponent_win')
                     THEN 1 ELSE 0 END), 0) AS wins,
                   COALESCE(SUM(CASE
                     WHEN (creator_id = #{userId} AND result = 'opponent_win')
                       OR (opponent_id = #{userId} AND result = 'creator_win')
                     THEN 1 ELSE 0 END), 0) AS losses,
                   COALESCE(SUM(CASE WHEN result = 'draw' THEN 1 ELSE 0 END), 0) AS draws
            FROM battle_room
            WHERE status = 'finished'
              AND (creator_id = #{userId} OR opponent_id = #{userId})
            """)
    BattleStatsRow selectBattleStats(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM learning_stage s
            JOIN learning_route r ON r.id = s.route_id
            WHERE s.status = 'published' AND s.deleted_at IS NULL
              AND r.status = 'published' AND r.deleted_at IS NULL
            """)
    long countPublishedStages();

    @Select("""
            SELECT COUNT(*)
            FROM learning_progress lp
            JOIN learning_stage s ON s.id = lp.stage_id
            JOIN learning_route r ON r.id = s.route_id
            WHERE lp.user_id = #{userId} AND lp.completed = 1
              AND s.status = 'published' AND s.deleted_at IS NULL
              AND r.status = 'published' AND r.deleted_at IS NULL
            """)
    long countCompletedPublishedStages(@Param("userId") Long userId);
}
