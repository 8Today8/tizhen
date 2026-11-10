package com.tizhen.core.account.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.entity.AdminRole;
import com.tizhen.core.account.entity.AdminUser;
import com.tizhen.core.account.mapper.AdminRoleMapper;
import com.tizhen.core.account.mapper.AdminUserMapper;
import com.tizhen.core.account.model.AdminAccountView;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.model.LoginMetadata;
import com.tizhen.core.account.model.RoleView;
import com.tizhen.core.admin.service.OperationLogService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 固定角色与管理员账号管理。所有写操作仅超级管理员可执行。
 */
@Service
public class AdminAccountService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Set<String> ALLOWED_STATUSES = Set.of("active", "disabled");
    private static final Set<String> ROLE_CODES = Set.of("super_admin", "content_operator");
    private static final String CONTENT_OPERATOR = "content_operator";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AdminSessionService adminSessionService;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public AdminAccountService(
            AdminUserMapper adminUserMapper,
            AdminRoleMapper adminRoleMapper,
            AdminSessionService adminSessionService,
            OperationLogService operationLogService,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper) {
        this.adminUserMapper = adminUserMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.adminSessionService = adminSessionService;
        this.operationLogService = operationLogService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<RoleView> listRoles(AdminPrincipal actor) {
        requireSuperAdmin(actor);
        return adminRoleMapper.selectList(Wrappers.<AdminRole>lambdaQuery()
                        .orderByAsc(AdminRole::getId))
                .stream()
                .map(this::toRoleView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AdminAccountView> listAccounts(AdminPrincipal actor,
                                                     String keyword,
                                                     String status,
                                                     String roleCode,
                                                     int page,
                                                     int pageSize) {
        requireSuperAdmin(actor);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedStatus = normalizeStatus(status);
        Long roleId = null;
        if (roleCode != null && !roleCode.isBlank()) {
            String normalizedRoleCode = roleCode.trim().toLowerCase(Locale.ROOT);
            if (!ROLE_CODES.contains(normalizedRoleCode)) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "roleCode 不支持");
            }
            roleId = requireRole(normalizedRoleCode).getId();
        }

        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        Page<AdminUser> result = new Page<>(safePage, safePageSize);
        Long finalRoleId = roleId;
        adminUserMapper.selectPage(result, Wrappers.<AdminUser>lambdaQuery()
                .eq(finalRoleId != null, AdminUser::getRoleId, finalRoleId)
                .eq(normalizedStatus != null, AdminUser::getStatus, normalizedStatus)
                .and(normalizedKeyword != null, wrapper -> wrapper
                        .like(AdminUser::getUsername, normalizedKeyword)
                        .or()
                        .like(AdminUser::getDisplayName, normalizedKeyword))
                .orderByDesc(AdminUser::getCreatedAt)
                .orderByDesc(AdminUser::getId));

        Map<Long, AdminRole> roles = adminRoleMapper.selectList(null).stream()
                .collect(java.util.stream.Collectors.toMap(AdminRole::getId, role -> role));
        List<AdminAccountView> items = result.getRecords().stream()
                .map(admin -> toAccountView(admin, roles.get(admin.getRoleId())))
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional(readOnly = true)
    public AdminAccountView getAccount(AdminPrincipal actor, Long adminId) {
        requireSuperAdmin(actor);
        return toAccountView(requireAdmin(adminId));
    }

    @Transactional
    public AdminAccountView createAccount(AdminPrincipal actor,
                                          String username,
                                          String displayName,
                                          String password,
                                          String roleCode,
                                          LoginMetadata metadata) {
        requireSuperAdmin(actor);
        String normalizedUsername = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "用户名需为 3–20 位字母、数字或下划线");
        }
        String normalizedDisplayName = displayName == null ? "" : displayName.trim();
        if (normalizedDisplayName.isEmpty() || normalizedDisplayName.length() > 64) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "显示名称需为 1–64 个字符");
        }
        if (password == null || password.length() < 6 || password.length() > 64) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "密码需为 6–64 个字符");
        }
        String normalizedRoleCode = roleCode == null ? "" : roleCode.trim().toLowerCase(Locale.ROOT);
        if (!CONTENT_OPERATOR.equals(normalizedRoleCode)) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_NOT_ALLOWED, "仅允许创建内容运营账号");
        }
        long existing = adminUserMapper.selectCount(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, normalizedUsername));
        if (existing > 0) {
            throw new BusinessException(ErrorCode.AUTH_ADMIN_USERNAME_TAKEN);
        }

        AdminRole role = requireRole(normalizedRoleCode);
        AdminUser admin = new AdminUser();
        admin.setUsername(normalizedUsername);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setDisplayName(normalizedDisplayName);
        admin.setRoleId(role.getId());
        admin.setStatus("active");
        adminUserMapper.insert(admin);

        operationLogService.record(
                actor.id(),
                "create",
                "admin_user",
                admin.getId(),
                "新增管理员账号 " + normalizedUsername,
                Map.of("username", normalizedUsername, "roleCode", normalizedRoleCode),
                metadata);
        return toAccountView(adminUserMapper.selectById(admin.getId()), role);
    }

    @Transactional
    public AdminAccountView updateStatus(AdminPrincipal actor,
                                         Long adminId,
                                         String status,
                                         LoginMetadata metadata) {
        requireSuperAdmin(actor);
        String normalizedStatus = normalizeRequiredStatus(status);
        AdminUser target = requireAdmin(adminId);
        boolean disabling = "disabled".equals(normalizedStatus);

        if (disabling && actor.id().equals(adminId)) {
            throw new BusinessException(ErrorCode.AUTH_SELF_DISABLE);
        }
        AdminRole role = requireRoleById(target.getRoleId());
        if (disabling
                && "super_admin".equals(role.getCode())
                && "active".equals(target.getStatus())
                && adminUserMapper.countActiveSuperAdmins() <= 1) {
            throw new BusinessException(ErrorCode.AUTH_LAST_SUPER_ADMIN);
        }

        if (!normalizedStatus.equals(target.getStatus())) {
            AdminUser update = new AdminUser();
            update.setId(adminId);
            update.setStatus(normalizedStatus);
            adminUserMapper.updateById(update);
            if (disabling) {
                adminSessionService.deleteByAdminId(adminId);
            }
            operationLogService.record(
                    actor.id(),
                    disabling ? "disable" : "enable",
                    "admin_user",
                    adminId,
                    (disabling ? "禁用管理员 " : "启用管理员 ") + target.getUsername(),
                    Map.of("username", target.getUsername(), "status", normalizedStatus),
                    metadata);
        }

        return toAccountView(requireAdmin(adminId));
    }

    @Transactional
    public void resetPassword(AdminPrincipal actor,
                              Long adminId,
                              String newPassword,
                              LoginMetadata metadata) {
        requireSuperAdmin(actor);
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 64) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "新密码需为 6–64 个字符");
        }
        AdminUser target = requireAdmin(adminId);
        AdminUser update = new AdminUser();
        update.setId(adminId);
        update.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUserMapper.updateById(update);

        adminSessionService.deleteByAdminId(adminId);
        operationLogService.record(
                actor.id(),
                "reset_password",
                "admin_user",
                adminId,
                "重置管理员密码 " + target.getUsername(),
                Map.of("username", target.getUsername()),
                metadata);
    }

    private void requireSuperAdmin(AdminPrincipal actor) {
        if (!AdminAuthService.isSuperAdmin(actor)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "仅超级管理员可管理角色与管理员账号");
        }
    }

    private AdminUser requireAdmin(Long adminId) {
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_ACCOUNT_NOT_FOUND);
        }
        return admin;
    }

    private AdminRole requireRole(String roleCode) {
        AdminRole role = adminRoleMapper.selectOne(Wrappers.<AdminRole>lambdaQuery()
                .eq(AdminRole::getCode, roleCode)
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_NOT_ALLOWED);
        }
        return role;
    }

    private AdminRole requireRoleById(Long roleId) {
        AdminRole role = adminRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
        }
        return role;
    }

    private AdminAccountView toAccountView(AdminUser admin) {
        return toAccountView(admin, requireRoleById(admin.getRoleId()));
    }

    private AdminAccountView toAccountView(AdminUser admin, AdminRole role) {
        if (role == null) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
        }
        return new AdminAccountView(
                String.valueOf(admin.getId()),
                admin.getUsername(),
                admin.getDisplayName(),
                admin.getAvatarUrl(),
                role.getCode(),
                role.getName(),
                admin.getStatus(),
                TimeUtils.toInstant(admin.getLastLoginAt()),
                TimeUtils.toInstant(admin.getCreatedAt()));
    }

    private RoleView toRoleView(AdminRole role) {
        return new RoleView(
                String.valueOf(role.getId()),
                role.getCode(),
                role.getName(),
                parsePermissions(role));
    }

    private List<String> parsePermissions(AdminRole role) {
        if (role.getPermissions() == null || role.getPermissions().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(role.getPermissions(), new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "status 仅支持 active 或 disabled");
        }
        return normalized;
    }

    private String normalizeRequiredStatus(String status) {
        String normalized = normalizeStatus(status);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "status 不能为空");
        }
        return normalized;
    }
}
