package com.tizhen.core.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.problem.entity.ProblemSample;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题面样例访问。编辑时按题目整体替换。
 */
@Mapper
public interface ProblemSampleMapper extends BaseMapper<ProblemSample> {
}
