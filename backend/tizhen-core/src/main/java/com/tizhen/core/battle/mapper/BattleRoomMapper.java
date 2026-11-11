package com.tizhen.core.battle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.core.battle.entity.BattleRoom;
import com.tizhen.core.problem.entity.Problem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对战房间状态迁移。所有状态变更都带条件，保证并发提交、超时与断线只产生一个最终结果。
 */
@Mapper
public interface BattleRoomMapper extends BaseMapper<BattleRoom> {

    @Select("SELECT * FROM battle_room WHERE room_code = #{roomCode}")
    BattleRoom selectByRoomCode(@Param("roomCode") String roomCode);

    /**
     * 从已发布且具备测试数据的 OJ 题中按筛选条件随机抽题。
     */
    @Select("""
            <script>
            SELECT p.*
            FROM problem p
            WHERE p.status = 'published'
              AND p.deleted_at IS NULL
              AND p.type = 'oj'
              AND EXISTS (SELECT 1 FROM test_data td WHERE td.problem_id = p.id)
            <if test="difficulty != null and difficulty != ''">
              AND p.difficulty = #{difficulty}
            </if>
            <if test="tag != null and tag != ''">
              AND EXISTS (
                SELECT 1 FROM problem_tag pt
                WHERE pt.problem_id = p.id
                  AND pt.tag_type = 'topic'
                  AND pt.tag_value = #{tag}
              )
            </if>
            ORDER BY RAND()
            LIMIT 1
            </script>
            """)
    Problem selectRandomOjProblem(@Param("difficulty") String difficulty,
                                  @Param("tag") String tag);

    @Update("""
            UPDATE battle_room
            SET opponent_id = #{userId}, opponent_ready = 0, status = 'waiting',
                updated_at = #{now}
            WHERE id = #{roomId}
              AND status = 'waiting'
              AND opponent_id IS NULL
              AND creator_id <> #{userId}
            """)
    int joinIfAvailable(@Param("roomId") Long roomId,
                        @Param("userId") Long userId,
                        @Param("now") LocalDateTime now);

    @Update("""
            UPDATE battle_room
            SET creator_ready = #{ready},
                status = CASE
                  WHEN #{ready} = 1 AND opponent_id IS NOT NULL AND opponent_ready = 1 THEN 'ready'
                  WHEN #{ready} = 0 THEN 'waiting'
                  ELSE status
                END,
                updated_at = #{now}
            WHERE id = #{roomId}
              AND creator_id = #{userId}
              AND status IN ('waiting', 'ready')
            """)
    int setCreatorReady(@Param("roomId") Long roomId,
                        @Param("userId") Long userId,
                        @Param("ready") boolean ready,
                        @Param("now") LocalDateTime now);

    @Update("""
            UPDATE battle_room
            SET opponent_ready = #{ready},
                status = CASE
                  WHEN #{ready} = 1 AND creator_ready = 1 THEN 'ready'
                  WHEN #{ready} = 0 THEN 'waiting'
                  ELSE status
                END,
                updated_at = #{now}
            WHERE id = #{roomId}
              AND opponent_id = #{userId}
              AND status IN ('waiting', 'ready')
            """)
    int setOpponentReady(@Param("roomId") Long roomId,
                         @Param("userId") Long userId,
                         @Param("ready") boolean ready,
                         @Param("now") LocalDateTime now);

    @Update("""
            UPDATE battle_room
            SET status = 'ongoing', problem_id = #{problemId}, started_at = #{now},
                updated_at = #{now}
            WHERE id = #{roomId}
              AND creator_id = #{userId}
              AND status = 'ready'
              AND opponent_id IS NOT NULL
              AND creator_ready = 1
              AND opponent_ready = 1
            """)
    int startReadyRoom(@Param("roomId") Long roomId,
                       @Param("userId") Long userId,
                       @Param("problemId") Long problemId,
                       @Param("now") LocalDateTime now);

    /**
     * 结束进行中的房间。仅 status='ongoing' 时生效，先 AC、认输与断线共用。
     */
    @Update("""
            UPDATE battle_room
            SET status = 'finished', result = #{result}, end_reason = #{reason},
                ended_at = #{endedAt}, updated_at = #{endedAt}
            WHERE id = #{roomId} AND status = 'ongoing'
            """)
    int finishOngoing(@Param("roomId") Long roomId,
                      @Param("result") String result,
                      @Param("reason") String reason,
                      @Param("endedAt") LocalDateTime endedAt);

    @Update("""
            UPDATE battle_room
            SET status = 'cancelled', result = NULL, end_reason = 'cancelled',
                ended_at = #{now}, updated_at = #{now}
            WHERE id = #{roomId}
              AND creator_id = #{userId}
              AND status IN ('waiting', 'ready')
            """)
    int cancelWaiting(@Param("roomId") Long roomId,
                      @Param("userId") Long userId,
                      @Param("now") LocalDateTime now);

    @Select("""
            <script>
            SELECT *
            FROM battle_room
            WHERE (creator_id = #{userId} OR opponent_id = #{userId})
              AND status = 'finished'
            <if test="result != null and result != ''">
              AND (
                (#{result} = 'draw' AND result = 'draw')
                OR (#{result} = 'win' AND (
                  (creator_id = #{userId} AND result = 'creator_win')
                  OR (opponent_id = #{userId} AND result = 'opponent_win')
                ))
                OR (#{result} = 'loss' AND (
                  (creator_id = #{userId} AND result = 'opponent_win')
                  OR (opponent_id = #{userId} AND result = 'creator_win')
                ))
              )
            </if>
            ORDER BY created_at DESC, id DESC
            </script>
            """)
    IPage<BattleRoom> selectMyFinishedRooms(Page<BattleRoom> page,
                                            @Param("userId") Long userId,
                                            @Param("result") String result);

    @Select("""
            SELECT * FROM battle_room
            WHERE status = 'ongoing'
              AND started_at IS NOT NULL
              AND started_at <= #{threshold}
            ORDER BY started_at ASC
            LIMIT #{limit}
            """)
    List<BattleRoom> selectExpiredOngoing(@Param("threshold") LocalDateTime threshold,
                                          @Param("limit") int limit);

    @Select("SELECT * FROM battle_room WHERE status = 'ongoing'")
    List<BattleRoom> selectOngoingRooms();
}
