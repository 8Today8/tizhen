package com.tizhen.core.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.judge.entity.FirstAc;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 首次 AC 事实。唯一键保证并发提交下只记录一次。
 */
@Mapper
public interface FirstAcMapper extends BaseMapper<FirstAc> {

    @Insert("""
            INSERT IGNORE INTO first_ac
              (user_id, problem_id, submission_id, first_ac_at, created_at)
            VALUES
              (#{userId}, #{problemId}, #{submissionId}, #{firstAcAt}, #{firstAcAt})
            """)
    int insertIgnore(@Param("userId") Long userId,
                     @Param("problemId") Long problemId,
                     @Param("submissionId") Long submissionId,
                     @Param("firstAcAt") LocalDateTime firstAcAt);
}
