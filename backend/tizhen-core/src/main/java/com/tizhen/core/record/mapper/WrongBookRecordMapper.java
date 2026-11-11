package com.tizhen.core.record.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.core.record.model.WrongBookRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 错题本只读查询。仅返回未移除记录，不读取提交代码或参考答案。
 */
@Mapper
public interface WrongBookRecordMapper {

    @Select("""
            <script>
            SELECT wb.id, wb.problem_id, p.type AS problem_type, p.title,
                   p.difficulty, wb.latest_error, wb.latest_score,
                   wb.error_count, wb.last_error_at
            FROM wrong_book wb
            JOIN problem p ON p.id = wb.problem_id
            WHERE wb.user_id = #{userId}
              AND wb.removed_at IS NULL
            <if test="problemType != null and problemType != ''">
              AND p.type = #{problemType}
            </if>
            <if test="keyword != null and keyword != ''">
              AND p.title LIKE CONCAT('%', #{keyword}, '%')
            </if>
            ORDER BY wb.last_error_at DESC, wb.id DESC
            </script>
            """)
    IPage<WrongBookRow> selectUserWrongBookPage(Page<WrongBookRow> page,
                                                @Param("userId") Long userId,
                                                @Param("problemType") String problemType,
                                                @Param("keyword") String keyword);
}
