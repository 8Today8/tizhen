package com.tizhen.core.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.problem.entity.ProblemTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目专题标签访问。
 */
@Mapper
public interface ProblemTagMapper extends BaseMapper<ProblemTag> {
}
