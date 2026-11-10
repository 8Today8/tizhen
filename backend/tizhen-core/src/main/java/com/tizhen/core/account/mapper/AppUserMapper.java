package com.tizhen.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.core.account.entity.AppUser;
import com.tizhen.core.account.model.AdminUserRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 选手账号访问。列表查询直接带出当前有效 CF Handle，避免逐行查询。
 */
@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {

    @Select("""
            <script>
            SELECT u.id, u.username, u.nickname, u.email, u.phone, u.avatar_url, u.school,
                   u.status, u.last_login_at, u.created_at, ea.handle AS cf_handle
            FROM app_user u
            LEFT JOIN external_account ea
              ON ea.user_id = u.id AND ea.platform = 'codeforces' AND ea.active_flag = 1
            WHERE u.deleted_at IS NULL
            <if test="status != null and status != ''">
                AND u.status = #{status}
              </if>
              <if test="keyword != null and keyword != ''">
                AND (
                  u.username LIKE CONCAT('%', #{keyword}, '%')
                  OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                  OR u.school LIKE CONCAT('%', #{keyword}, '%')
                  OR ea.handle LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            ORDER BY u.created_at DESC, u.id DESC
            </script>
            """)
    IPage<AdminUserRow> selectAdminUserPage(Page<AdminUserRow> page,
                                            @Param("keyword") String keyword,
                                            @Param("status") String status);

    @Select("""
            SELECT u.id, u.username, u.nickname, u.email, u.phone, u.avatar_url, u.school,
                   u.status, u.last_login_at, u.created_at, ea.handle AS cf_handle
            FROM app_user u
            LEFT JOIN external_account ea
              ON ea.user_id = u.id AND ea.platform = 'codeforces' AND ea.active_flag = 1
            WHERE u.id = #{id} AND u.deleted_at IS NULL
            """)
    AdminUserRow selectAdminUserRow(@Param("id") Long id);
}
