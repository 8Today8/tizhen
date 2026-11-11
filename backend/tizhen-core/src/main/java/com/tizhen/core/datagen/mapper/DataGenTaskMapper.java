package com.tizhen.core.datagen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.datagen.entity.DataGenTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
