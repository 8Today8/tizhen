package com.tizhen.core.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.learning.entity.StageCourse;
import org.apache.ibatis.annotations.Mapper;

/**
 * 阶段课程访问。
 */
@Mapper
public interface StageCourseMapper extends BaseMapper<StageCourse> {
}
