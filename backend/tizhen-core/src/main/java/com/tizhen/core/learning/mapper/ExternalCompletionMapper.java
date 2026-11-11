package com.tizhen.core.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.learning.entity.ExternalCompletion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 外链题完成状态访问。
 */
@Mapper
public interface ExternalCompletionMapper extends BaseMapper<ExternalCompletion> {
}
