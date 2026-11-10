package com.tizhen.core.leaderboard.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.core.leaderboard.model.LeaderboardRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 榜单实时派生查询。周榜与总榜共用同一聚合结构，仅分数来源不同。
 */
@Mapper
public interface LeaderboardMapper {

    @Select("""
            <script>
            WITH cf_ranked AS (
              SELECT ea.user_id, ea.handle, ea.verification_status, ea.data_source, ea.last_sync_at,
                     ea.weekly_count, ea.weekly_week_start, ea.all_time_count,
                     ROW_NUMBER() OVER (
                       PARTITION BY ea.user_id, ea.platform
                       ORDER BY CASE WHEN ea.active_flag = 1 THEN 0 ELSE 1 END,
                                ea.unbound_at DESC,
                                ea.id DESC
                     ) AS rn
              FROM external_account ea
              WHERE ea.platform = 'codeforces'
            ),
            cf AS (
              SELECT * FROM cf_ranked WHERE rn = 1
            ),
            internal AS (
              SELECT fa.user_id, COUNT(*) AS solved
              FROM first_ac fa
              <if test="week">
                WHERE fa.first_ac_at &gt;= #{weekStartAt} AND fa.first_ac_at &lt; #{weekEndAt}
              </if>
              GROUP BY fa.user_id
            ),
            manual AS (
              SELECT md.user_id, SUM(md.count) AS solved,
                     COUNT(*) AS declaration_count,
                     MAX(md.updated_at) AS manual_updated_at
              FROM manual_declaration md
              WHERE md.active_flag = 1 AND md.verification_status = 'verified'
              <if test="week">
                AND md.week_start_date = #{weekStartDate}
              </if>
              GROUP BY md.user_id
            ),
            base AS (
              SELECT u.id, u.username, u.nickname, u.avatar_url, u.school, u.email,
                     COALESCE(i.solved, 0) AS internal_solved,
                     COALESCE(
                       <choose>
                         <when test="week">
                           CASE WHEN cf.weekly_week_start = #{weekStartDate} THEN cf.weekly_count ELSE 0 END
                         </when>
                         <otherwise>cf.all_time_count</otherwise>
                       </choose>,
                       0
                     ) AS cf_solved,
                     COALESCE(m.solved, 0) AS manual_solved,
                     COALESCE(m.declaration_count, 0) AS manual_declaration_count,
                     cf.handle AS cf_handle,
                     cf.verification_status AS cf_verification_status,
                     cf.data_source AS cf_data_source,
                     cf.last_sync_at AS cf_last_sync_at,
                     m.manual_updated_at AS manual_updated_at
              FROM app_user u
              LEFT JOIN internal i ON i.user_id = u.id
              LEFT JOIN cf ON cf.user_id = u.id
              LEFT JOIN manual m ON m.user_id = u.id
              WHERE u.status = 'active'
            ),
            scored AS (
              SELECT base.*, (internal_solved + cf_solved + manual_solved) AS total_score
              FROM base
            )
            SELECT id, username, nickname, avatar_url, school, email, cf_handle,
                   internal_solved, cf_solved, manual_solved, manual_declaration_count, total_score,
                   RANK() OVER (ORDER BY total_score DESC, internal_solved DESC) AS `rank`,
                   cf_verification_status, cf_data_source, cf_last_sync_at, manual_updated_at
            FROM scored
            WHERE total_score &gt; 0
            ORDER BY total_score DESC, internal_solved DESC, id ASC
            </script>
            """)
    IPage<LeaderboardRow> selectAdminLeaderboard(Page<LeaderboardRow> page,
                                                 @Param("week") boolean week,
                                                 @Param("weekStartDate") LocalDate weekStartDate,
                                                 @Param("weekStartAt") LocalDateTime weekStartAt,
                                                 @Param("weekEndAt") LocalDateTime weekEndAt);
}
