package com.tizhen.core.account.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户详情中的只读统计。均为按用户索引的常量级计数查询。
 */
@Mapper
public interface UserStatisticsMapper {

    @Select("SELECT COUNT(*) FROM data_gen_task WHERE user_id = #{userId}")
    long countDataGenTasks(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM submission WHERE user_id = #{userId}")
    long countSubmissions(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM first_ac WHERE user_id = #{userId}")
    long countFirstAc(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM wrong_book WHERE user_id = #{userId} AND removed_at IS NULL")
    long countWrongBook(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM battle_room WHERE creator_id = #{userId} OR opponent_id = #{userId}")
    long countBattles(@Param("userId") Long userId);
}
