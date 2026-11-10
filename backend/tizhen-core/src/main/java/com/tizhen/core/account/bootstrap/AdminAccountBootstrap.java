package com.tizhen.core.account.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tizhen.core.account.entity.AdminRole;
import com.tizhen.core.account.entity.AdminUser;
import com.tizhen.core.account.mapper.AdminRoleMapper;
import com.tizhen.core.account.mapper.AdminUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 首次部署引导：仅当 admin_user 为空时，按环境变量创建初始超级管理员。
 * 未配置用户名或密码则不创建，适合生产环境由运维显式启用。
 */
@Component
public class AdminAccountBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountBootstrap.class);
    private static final String SUPER_ADMIN_CODE = "super_admin";
    private static final int USERNAME_MAX_LENGTH = 32;
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int PASSWORD_MAX_LENGTH = 64;

    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String displayName;

    public AdminAccountBootstrap(
            AdminUserMapper adminUserMapper,
            AdminRoleMapper adminRoleMapper,
            PasswordEncoder passwordEncoder,
            @Value("${tizhen.bootstrap.admin.username:}") String username,
            @Value("${tizhen.bootstrap.admin.password:}") String password,
            @Value("${tizhen.bootstrap.admin.display-name:超级管理员}") String displayName) {
        this.adminUserMapper = adminUserMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        this.displayName = displayName == null || displayName.isBlank() ? "超级管理员" : displayName.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isEmpty() && password.isEmpty()) {
            return;
        }
        if (username.isEmpty() || password.isEmpty()) {
            log.warn("引导管理员配置不完整，已跳过；请同时设置用户名与密码");
            return;
        }
        if (username.length() > USERNAME_MAX_LENGTH
                || password.length() < PASSWORD_MIN_LENGTH
                || password.length() > PASSWORD_MAX_LENGTH) {
            log.warn("引导管理员配置不合法，已跳过；用户名不超过 {} 位，密码长度 {}–{} 位",
                    USERNAME_MAX_LENGTH, PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH);
            return;
        }
        if (adminUserMapper.selectCount(null) > 0) {
            return;
        }

        AdminRole role = adminRoleMapper.selectOne(Wrappers.<AdminRole>lambdaQuery()
                .eq(AdminRole::getCode, SUPER_ADMIN_CODE)
                .last("LIMIT 1"));
        if (role == null) {
            log.error("引导管理员失败：缺少角色 {}", SUPER_ADMIN_CODE);
            return;
        }

        AdminUser admin = new AdminUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setDisplayName(displayName);
        admin.setRoleId(role.getId());
        admin.setStatus("active");
        adminUserMapper.insert(admin);
        log.info("已创建初始超级管理员 username={}", username);
    }
}
