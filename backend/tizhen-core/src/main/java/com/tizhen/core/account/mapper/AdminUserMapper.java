package com.tizhen.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tizhen.core.account.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    @Select("""
            SELECT COUNT(*)
            FROM admin_user au
            JOIN admin_role ar ON ar.id = au.role_id
            WHERE au.status = 'active' AND ar.code = 'super_admin'
            """)
    long countActiveSuperAdmins();
}
