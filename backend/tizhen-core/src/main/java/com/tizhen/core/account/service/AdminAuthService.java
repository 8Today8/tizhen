package com.tizhen.core.account.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.core.account.entity.AdminRole;
import com.tizhen.core.account.entity.AdminUser;
import com.tizhen.core.account.mapper.AdminRoleMapper;
import com.tizhen.core.account.mapper.AdminUserMapper;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.model.AuthenticatedAdmin;
import com.tizhen.core.account.model.LoginMetadata;
import com.tizhen.core.admin.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * 管理员认证：凭据校验、失败限流、登录审计与后台会话创建。
 */
@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);
    private static final String ACTIVE_STATUS = "active";
    private static final String SUPER_ADMIN_CODE = "super_admin";

    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminLoginRateLimiter rateLimiter;
    private final AdminSessionService sessionService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;
    private final String dummyPasswordHash;

    public AdminAuthService(
            AdminUserMapper adminUserMapper,
            AdminRoleMapper adminRoleMapper,
            PasswordEncoder passwordEncoder,
            AdminLoginRateLimiter rateLimiter,
            AdminSessionService sessionService,
            OperationLogService operationLogService,
            ObjectMapper objectMapper) {
        this.adminUserMapper = adminUserMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.sessionService = sessionService;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
        this.dummyPasswordHash = passwordEncoder.encode("tizhen-dummy-password");
    }

    /**
     * 用户名大小写不敏感；账号不存在与密码错误统一返回同一错误。
     */
    @Transactional
    public AuthenticatedAdmin login(String username, String password, LoginMetadata metadata) {
        String normalizedUsername = username == null ? "" : username.trim();
        String ipAddress = metadata.ipAddress();
        rateLimiter.assertNotLocked(normalizedUsername, ipAddress);

        AdminUser admin = findAdmin(normalizedUsername);
        if (admin == null) {
            passwordEncoder.matches(password, dummyPasswordHash);
            throw invalidCredentials(normalizedUsername, ipAddress);
        }
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw invalidCredentials(normalizedUsername, ipAddress);
        }
        if (!ACTIVE_STATUS.equals(admin.getStatus())) {
            throw new BusinessException(ErrorCode.ADMIN_ACCOUNT_DISABLED);
        }

        AdminRole role = adminRoleMapper.selectById(admin.getRoleId());
        if (role == null) {
            log.error("管理员角色缺失 adminId={}, roleId={}", admin.getId(), admin.getRoleId());
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
        }

        rateLimiter.reset(normalizedUsername, ipAddress);
        adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                .eq(AdminUser::getId, admin.getId())
                .set(AdminUser::getLastLoginAt, LocalDateTime.now(ZoneOffset.UTC)));

        operationLogService.recordAdminLogin(admin.getId(), metadata);
        String sessionToken = sessionService.create(admin.getId());

        return new AuthenticatedAdmin(toPrincipal(admin, role), sessionToken);
    }

    /**
     * 每次后台请求都读取账号与角色状态，停用账号的已登录会话会立即失效。
     */
    @Transactional(readOnly = true)
    public Optional<AdminPrincipal> loadPrincipal(Long adminId) {
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null || !ACTIVE_STATUS.equals(admin.getStatus())) {
            return Optional.empty();
        }
        AdminRole role = adminRoleMapper.selectById(admin.getRoleId());
        if (role == null) {
            log.error("管理员角色缺失 adminId={}, roleId={}", admin.getId(), admin.getRoleId());
            return Optional.empty();
        }
        return Optional.of(toPrincipal(admin, role));
    }

    private AdminUser findAdmin(String username) {
        if (username.isEmpty()) {
            return null;
        }
        return adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, username)
                .last("LIMIT 1"));
    }

    private BusinessException invalidCredentials(String username, String ipAddress) {
        boolean locked = rateLimiter.recordFailure(username, ipAddress);
        if (locked) {
            return new BusinessException(ErrorCode.COMMON_RATE_LIMITED, "登录失败次数过多，请稍后重试");
        }
        return new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    private AdminPrincipal toPrincipal(AdminUser admin, AdminRole role) {
        return new AdminPrincipal(
                admin.getId(),
                admin.getUsername(),
                admin.getDisplayName(),
                admin.getAvatarUrl(),
                role.getCode(),
                role.getName(),
                parsePermissions(role));
    }

    private List<String> parsePermissions(AdminRole role) {
        try {
            return objectMapper.readValue(role.getPermissions(), new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException exception) {
            log.warn("管理员角色权限解析失败 roleCode={}", role.getCode());
            return List.of();
        }
    }

    public static boolean isSuperAdmin(AdminPrincipal principal) {
        return principal != null && SUPER_ADMIN_CODE.equals(principal.roleCode());
    }
}
