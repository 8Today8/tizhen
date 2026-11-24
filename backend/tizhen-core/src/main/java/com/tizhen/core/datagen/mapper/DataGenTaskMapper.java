package com.tizhen.core.datagen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.datagen.entity.DataGenTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 造数据任务读取与 worker 任务领取。领取语句使用 SKIP LOCKED，
 * 保证多 worker 并发时同一任务只被一个执行器处理。
 */
@Mapper
public interface DataGenTaskMapper extends BaseMapper<DataGenTask> {

    @Select("""
            SELECT * FROM data_gen_task
            WHERE status = 'pending'
            ORDER BY created_at ASC, id ASC
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<DataGenTask> selectPendingForUpdate(@Param("limit") int limit);

    @Select("""
            SELECT * FROM data_gen_task
            WHERE status IN ('llm_generating', 'llm_data_generating', 'generating')
              AND heartbeat_at IS NOT NULL
              AND heartbeat_at < #{threshold}
            ORDER BY created_at ASC, id ASC
            LIMIT 100
            """)
    List<DataGenTask> selectStaleInProgress(@Param("threshold") LocalDateTime threshold);

    @Select("""
            SELECT * FROM data_gen_task
            WHERE files_cleaned_at IS NULL
              AND files_expire_at < #{now}
            ORDER BY files_expire_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<DataGenTask> selectExpiredFiles(@Param("now") LocalDateTime now,
                                         @Param("limit") int limit);

    /**
     * 按 id 领取待处理任务，供 MQ 消费端使用。更新 0 行表示已被处理或重复投递。
     */
    @Update("""
            UPDATE data_gen_task
            SET status = CASE generation_mode
                    WHEN 'llm_generator' THEN 'llm_generating'
                    WHEN 'llm_direct_data' THEN 'llm_data_generating'
                    ELSE 'generating' END,
                generator_status = CASE
                    WHEN generation_mode = 'llm_generator' THEN 'generating'
                    ELSE generator_status END,
                llm_data_status = CASE
                    WHEN generation_mode = 'llm_direct_data' THEN 'generating'
                    ELSE llm_data_status END,
                started_at = #{now},
                heartbeat_at = #{now},
                attempts = attempts + 1,
                updated_at = #{now}
            WHERE id = #{id} AND status = 'pending'
            """)
    int claimPendingById(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 领取一条仍在执行中的任务，用于重试消息和 broker 重投。
     */
    @Update("""
            UPDATE data_gen_task
            SET status = CASE generation_mode
                    WHEN 'llm_generator' THEN 'llm_generating'
                    WHEN 'llm_direct_data' THEN 'llm_data_generating'
                    ELSE 'generating' END,
                generator_status = CASE
                    WHEN generation_mode = 'llm_generator' THEN 'generating'
                    ELSE generator_status END,
                llm_data_status = CASE
                    WHEN generation_mode = 'llm_direct_data' THEN 'generating'
                    ELSE llm_data_status END,
                started_at = #{now},
                heartbeat_at = #{now},
                attempts = attempts + 1,
                updated_at = #{now}
            WHERE id = #{id}
              AND status IN ('generating', 'llm_generating', 'llm_data_generating')
              AND attempts < #{maxAttempts}
            """)
    int claimInProgressById(@Param("id") Long id,
                            @Param("now") LocalDateTime now,
                            @Param("maxAttempts") int maxAttempts);

    /**
     * 补偿扫描：捞取落库超过阈值仍未进入执行态的任务。
     */
    @Select("""
            SELECT id FROM data_gen_task
            WHERE status = 'pending' AND created_at < #{threshold}
            ORDER BY created_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<DataGenTask> selectPendingBefore(@Param("threshold") LocalDateTime threshold,
                                          @Param("limit") int limit);
}
