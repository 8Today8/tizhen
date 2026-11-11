package com.tizhen.core.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.problem.entity.ObjectiveOption;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客观题选项访问。编辑时按题目整体替换。
 */
@Mapper
public interface ObjectiveOptionMapper extends BaseMapper<ObjectiveOption> {
}
