package com.tizhen.core.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.problem.entity.TestData;
import org.apache.ibatis.annotations.Mapper;

/**
 * OJ 测试数据访问。只允许管理员接口读取正文。
 */
@Mapper
public interface TestDataMapper extends BaseMapper<TestData> {
}
