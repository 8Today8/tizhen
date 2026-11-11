package com.tizhen.core.leaderboard.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.core.leaderboard.model.LeaderboardRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 榜单实时派生查询。周榜与总榜共用同一聚合结构，仅分数来源不同。
 */
@Mapper
public interface LeaderboardMapper {

    /**
     * 分页查询榜单；userId 为空时返回完整排名。
     */
    IPage<LeaderboardRow> selectLeaderboard(Page<LeaderboardRow> page,
                                            @Param("week") boolean week,
                                            @Param("weekStartDate") LocalDate weekStartDate,
                                            @Param("weekStartAt") LocalDateTime weekStartAt,
                                            @Param("weekEndAt") LocalDateTime weekEndAt,
                                            @Param("userId") Long userId);

    /**
     * 查询单个用户在完整排名中的位置；无成绩或已禁用时返回 null。
     */
    LeaderboardRow selectLeaderboardUser(@Param("week") boolean week,
                                         @Param("weekStartDate") LocalDate weekStartDate,
                                         @Param("weekStartAt") LocalDateTime weekStartAt,
                                         @Param("weekEndAt") LocalDateTime weekEndAt,
                                         @Param("userId") Long userId);
}
