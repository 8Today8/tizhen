package com.tizhen.core.external.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.external.entity.ExternalAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExternalAccountMapper extends BaseMapper<ExternalAccount> {
}
