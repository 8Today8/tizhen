package com.tizhen.core.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.judge.entity.WrongBook;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 错题本聚合写入。再次出错时自动恢复未移除状态并累计错误次数。
 */
@Mapper
public interface WrongBookMapper extends BaseMapper<WrongBook> {

    @Insert("""
            INSERT INTO wrong_book
              (user_id, problem_id, latest_error, latest_score, error_count,
               last_error_at, removed_at, created_at, updated_at)
            VALUES
              (#{userId}, #{problemId}, #{latestError}, #{latestScore}, 1,
               #{occurredAt}, NULL, #{occurredAt}, #{occurredAt})
            ON DUPLICATE KEY UPDATE
              latest_error = VALUES(latest_error),
              latest_score = VALUES(latest_score),
              error_count = error_count + 1,
              last_error_at = VALUES(last_error_at),
              removed_at = NULL,
              updated_at = VALUES(updated_at)
            """)
    int upsert(@Param("userId") Long userId,
               @Param("problemId") Long problemId,
               @Param("latestError") String latestError,
               @Param("latestScore") BigDecimal latestScore,
               @Param("occurredAt") LocalDateTime occurredAt);
}
