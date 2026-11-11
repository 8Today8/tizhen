package com.tizhen.core.account.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.entity.AppUser;
import com.tizhen.core.account.entity.LlmCredential;
import com.tizhen.core.account.mapper.AppUserMapper;
import com.tizhen.core.account.mapper.LlmCredentialMapper;
import com.tizhen.core.account.mapper.UserStatisticsMapper;
import com.tizhen.core.account.model.AdminExternalAccountView;
import com.tizhen.core.account.model.AdminLlmCredentialView;
import com.tizhen.core.account.model.AdminUserDetail;
import com.tizhen.core.account.model.AdminUserRow;
import com.tizhen.core.account.model.AdminUserSummary;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.model.LoginMetadata;
import com.tizhen.core.admin.service.OperationLogService;
import com.tizhen.core.external.entity.ExternalAccount;
import com.tizhen.core.external.mapper.ExternalAccountMapper;
import com.tizhen.core.infra.file.AvatarStorageService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 后台用户管理：列表、详情、新增、批量启停用、批量删除与重置密码。
 * 内容运营只读，所有写操作仅超级管理员可执行。
 */
@Service
public class AdminUserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Set<String> ALLOWED_STATUSES = Set.of("active", "disabled");
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 100;

    private static final Map<String, String> PROVIDER_NAMES = Map.of(
            "deepseek", "DeepSeek",
            "qwen", "通义千问",
            "zhipu", "智谱 AI",
            "openai", "OpenAI");

    private final AppUserMapper appUserMapper;
    private final ExternalAccountMapper externalAccountMapper;
    private final LlmCredentialMapper llmCredentialMapper;
    private final UserStatisticsMapper userStatisticsMapper;
    private final PlayerSessionInvalidationService playerSessionInvalidationService;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;

    public AdminUserService(
            AppUserMapper appUserMapper,
            ExternalAccountMapper externalAccountMapper,
            LlmCredentialMapper llmCredentialMapper,
            UserStatisticsMapper userStatisticsMapper,
            PlayerSessionInvalidationService playerSessionInvalidationService,
            OperationLogService operationLogService,
            PasswordEncoder passwordEncoder,
            AvatarStorageService avatarStorageService) {
        this.appUserMapper = appUserMapper;
        this.externalAccountMapper = externalAccountMapper;
        this.llmCredentialMapper = llmCredentialMapper;
        this.userStatisticsMapper = userStatisticsMapper;
        this.playerSessionInvalidationService = playerSessionInvalidationService;
        this.operationLogService = operationLogService;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorageService = avatarStorageService;
    }

    @Transactional(readOnly = true)
    public PageResult<AdminUserSummary> list(String keyword, String status, int page, int pageSize) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedStatus = normalizeStatus(status);
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<AdminUserRow> result = new Page<>(safePage, safePageSize);
        appUserMapper.selectAdminUserPage(result, normalizedKeyword, normalizedStatus);
        List<AdminUserSummary> items = result.getRecords().stream()
                .map(this::toSummary)
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional(readOnly = true)
    public AdminUserDetail detail(Long userId) {
        AdminUserRow row = requireUserRow(userId);
        AppUser user = appUserMapper.selectById(userId);

        List<AdminExternalAccountView> cfAccounts = externalAccountMapper.selectList(
                        Wrappers.<ExternalAccount>lambdaQuery()
                                .eq(ExternalAccount::getUserId, userId)
                                .orderByDesc(ExternalAccount::getCreatedAt)
                                .orderByDesc(ExternalAccount::getId))
                .stream()
                .map(this::toExternalAccountView)
                .toList();

        LlmCredential credential = llmCredentialMapper.selectOne(
                Wrappers.<LlmCredential>lambdaQuery()
                        .eq(LlmCredential::getUserId, userId)
                        .last("LIMIT 1"));

        return new AdminUserDetail(
                toSummary(row),
                cfAccounts,
                toCredentialView(credential),
                userStatisticsMapper.countDataGenTasks(userId),
                userStatisticsMapper.countSubmissions(userId),
                userStatisticsMapper.countFirstAc(userId),
                userStatisticsMapper.countWrongBook(userId),
                userStatisticsMapper.countBattles(userId));
    }

    @Transactional
    public AdminUserSummary createUser(AdminPrincipal actor,
                                       String username,
                                       String nickname,
                                       String password,
                                       String email,
                                       String phone,
                                       String school,
                                       String avatarUrl,
                                       LoginMetadata metadata) {
        requireSuperAdmin(actor);
        String normalizedUsername = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED,
                    "用户名需为 3–20 位字母、数字或下划线");
        }
        String normalizedNickname = nickname == null ? "" : nickname.trim();
        if (normalizedNickname.isEmpty() || normalizedNickname.length() > 64) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "昵称需为 1–64 个字符");
        }
        validatePassword(password);
        String normalizedEmail = normalizeOptional(email, 254, "邮箱");
        if (normalizedEmail != null && !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "邮箱格式不正确");
        }
        String normalizedPhone = normalizeOptional(phone, 32, "手机号");
        String normalizedSchool = normalizeOptional(school, 128, "学校");
        String normalizedAvatarUrl = avatarStorageService.normalizeAvatarUrl(avatarUrl);

        long usernameCount = appUserMapper.selectCount(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getUsername, normalizedUsername));
        if (usernameCount > 0) {
            throw new BusinessException(ErrorCode.AUTH_USERNAME_TAKEN);
        }
        if (normalizedEmail != null) {
            long emailCount = appUserMapper.selectCount(Wrappers.<AppUser>lambdaQuery()
                    .eq(AppUser::getEmail, normalizedEmail));
            if (emailCount > 0) {
                throw new BusinessException(ErrorCode.AUTH_EMAIL_TAKEN);
            }
        }

        AppUser user = new AppUser();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(normalizedNickname);
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        user.setSchool(normalizedSchool);
        user.setAvatarUrl(normalizedAvatarUrl);
        user.setStatus("active");
        appUserMapper.insert(user);

        operationLogService.record(
                actor.id(),
                "create",
                "user",
                user.getId(),
                "新增用户 " + normalizedUsername,
                Map.of("username", normalizedUsername, "status", "active",
                        "hasAvatar", normalizedAvatarUrl != null),
                metadata);
        return toSummary(appUserMapper.selectAdminUserRow(user.getId()));
    }

    /**
     * 超级管理员更换用户头像；avatarUrl 为空表示清除。
     */
    @Transactional
    public AdminUserSummary updateAvatar(AdminPrincipal actor,
                                         Long userId,
                                         String avatarUrl,
                                         LoginMetadata metadata) {
        requireSuperAdmin(actor);
        AppUser user = requireUserEntity(userId);
        String normalizedAvatarUrl = avatarStorageService.normalizeAvatarUrl(avatarUrl);
        String previousAvatarUrl = user.getAvatarUrl();

        if (!java.util.Objects.equals(previousAvatarUrl, normalizedAvatarUrl)) {
            appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                    .eq(AppUser::getId, userId)
                    .set(AppUser::getAvatarUrl, normalizedAvatarUrl));
            operationLogService.record(
                    actor.id(),
                    "update_avatar",
                    "user",
                    userId,
                    "更新用户头像 " + user.getUsername(),
                    Map.of("username", user.getUsername(), "hasAvatar", normalizedAvatarUrl != null),
                    metadata);
            if (previousAvatarUrl != null) {
                avatarStorageService.deleteByUrl(previousAvatarUrl);
            }
        }
        return toSummary(appUserMapper.selectAdminUserRow(userId));
    }

    @Transactional
    public AdminUserSummary updateStatus(AdminPrincipal actor,
                                         Long userId,
                                         String status,
                                         LoginMetadata metadata) {
        requireSuperAdmin(actor);
        String normalizedStatus = normalizeRequiredStatus(status);
        AppUser user = requireUserEntity(userId);

        boolean disabling = "disabled".equals(normalizedStatus);
        if (!normalizedStatus.equals(user.getStatus())) {
            AppUser update = new AppUser();
            update.setId(userId);
            update.setStatus(normalizedStatus);
            appUserMapper.updateById(update);
            if (disabling) {
                playerSessionInvalidationService.invalidate(userId, user.getUsername());
            }
            operationLogService.record(
                    actor.id(),
                    disabling ? "disable" : "enable",
                    "user",
                    userId,
                    (disabling ? "禁用用户 " : "启用用户 ") + user.getUsername(),
                    Map.of("username", user.getUsername(), "status", normalizedStatus),
                    metadata);
        }

        return toSummary(requireUserRow(userId));
    }

    @Transactional
    public void updateStatuses(AdminPrincipal actor,
                               List<Long> userIds,
                               String status,
                               LoginMetadata metadata) {
        requireSuperAdmin(actor);
        String normalizedStatus = normalizeRequiredStatus(status);
        List<Long> normalizedIds = normalizeUserIds(userIds);
        List<AppUser> users = requireUsers(normalizedIds);
        boolean disabling = "disabled".equals(normalizedStatus);
        List<AppUser> changedUsers = users.stream()
                .filter(user -> !normalizedStatus.equals(user.getStatus()))
                .toList();
        if (changedUsers.isEmpty()) {
            return;
        }

        int updated = appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                .in(AppUser::getId, changedUsers.stream().map(AppUser::getId).toList())
                .isNull(AppUser::getDeletedAt)
                .set(AppUser::getStatus, normalizedStatus));
        if (updated != changedUsers.size()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "部分用户状态已变化，请刷新后重试");
        }
        for (AppUser user : changedUsers) {
            if (disabling) {
                playerSessionInvalidationService.invalidate(user.getId(), user.getUsername());
            }
            operationLogService.record(
                    actor.id(),
                    disabling ? "disable" : "enable",
                    "user",
                    user.getId(),
                    (disabling ? "禁用用户 " : "启用用户 ") + user.getUsername(),
                    Map.of("username", user.getUsername(), "status", normalizedStatus, "batch", true),
                    metadata);
        }
    }

    @Transactional
    public void batchDelete(AdminPrincipal actor, List<Long> userIds, LoginMetadata metadata) {
        requireSuperAdmin(actor);
        List<Long> normalizedIds = normalizeUserIds(userIds);
        List<AppUser> users = requireUsers(normalizedIds);
        LocalDateTime deletedAt = LocalDateTime.now(ZoneOffset.UTC);

        int updated = appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                .in(AppUser::getId, normalizedIds)
                .isNull(AppUser::getDeletedAt)
                .set(AppUser::getStatus, "disabled")
                .set(AppUser::getDeletedAt, deletedAt));
        if (updated != normalizedIds.size()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "部分用户已被删除，请刷新后重试");
        }
        for (AppUser user : users) {
            playerSessionInvalidationService.invalidate(user.getId(), user.getUsername());
            operationLogService.record(
                    actor.id(),
                    "delete",
                    "user",
                    user.getId(),
                    "删除用户 " + user.getUsername(),
                    Map.of("username", user.getUsername(), "status", "disabled", "batch", true),
                    metadata);
        }
    }

    @Transactional
    public void resetPassword(AdminPrincipal actor,
                              Long userId,
                              String newPassword,
                              LoginMetadata metadata) {
        requireSuperAdmin(actor);
        validatePassword(newPassword);
        AppUser user = requireUserEntity(userId);

        AppUser update = new AppUser();
        update.setId(userId);
        update.setPasswordHash(passwordEncoder.encode(newPassword));
        appUserMapper.updateById(update);

        playerSessionInvalidationService.invalidate(userId, user.getUsername());
        operationLogService.record(
                actor.id(),
                "reset_password",
                "user",
                userId,
                "重置用户密码 " + user.getUsername(),
                Map.of("username", user.getUsername()),
                metadata);
    }

    private void requireSuperAdmin(AdminPrincipal actor) {
        if (!AdminAuthService.isSuperAdmin(actor)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "仅超级管理员可管理选手用户");
        }
    }

    private AppUser requireUserEntity(Long userId) {
        AppUser user = userId == null ? null : appUserMapper.selectById(userId);
        if (user == null || user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "用户不存在或已删除");
        }
        return user;
    }

    private List<AppUser> requireUsers(List<Long> userIds) {
        List<AppUser> users = appUserMapper.selectBatchIds(userIds);
        boolean containsDeleted = users.stream().anyMatch(user -> user.getDeletedAt() != null);
        if (users.size() != userIds.size() || containsDeleted) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "部分用户不存在或已删除");
        }
        return users;
    }

    private List<Long> normalizeUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "请选择用户");
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId == null || userId <= 0) {
                throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "用户 ID 非法");
            }
            uniqueIds.add(userId);
        }
        if (uniqueIds.size() > MAX_BATCH_SIZE) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "单次最多操作 100 位用户");
        }
        return List.copyOf(uniqueIds);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 64) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "密码需为 6–64 个字符");
        }
    }

    private String normalizeOptional(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED,
                    fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }

    private AdminUserRow requireUserRow(Long userId) {
        AdminUserRow row = appUserMapper.selectAdminUserRow(userId);
        if (row == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "用户不存在");
        }
        return row;
    }

    private AdminUserSummary toSummary(AdminUserRow row) {
        return new AdminUserSummary(
                String.valueOf(row.getId()),
                row.getUsername(),
                row.getNickname(),
                row.getEmail(),
                row.getPhone(),
                row.getAvatarUrl(),
                row.getSchool(),
                row.getCfHandle(),
                row.getStatus(),
                TimeUtils.toInstant(row.getLastLoginAt()),
                TimeUtils.toInstant(row.getCreatedAt()));
    }

    private AdminExternalAccountView toExternalAccountView(ExternalAccount account) {
        return new AdminExternalAccountView(
                String.valueOf(account.getId()),
                account.getPlatform(),
                account.getHandle(),
                account.getProfileUrl(),
                account.getVerificationStatus(),
                account.getDataSource(),
                account.getWeeklyCount(),
                account.getWeeklyWeekStart(),
                account.getAllTimeCount(),
                TimeUtils.toInstant(account.getLastSyncAt()),
                account.getStatus(),
                TimeUtils.toInstant(account.getUnboundAt()),
                TimeUtils.toInstant(account.getCreatedAt()));
    }

    private AdminLlmCredentialView toCredentialView(LlmCredential credential) {
        if (credential == null) {
            return new AdminLlmCredentialView(false, null, null, null, null, null, null);
        }
        boolean configured = "valid".equals(credential.getStatus())
                && credential.getApiKeyCiphertext() != null
                && !credential.getApiKeyCiphertext().isBlank();
        String provider = credential.getProvider();
        return new AdminLlmCredentialView(
                configured,
                provider,
                provider == null ? null : PROVIDER_NAMES.getOrDefault(provider, provider),
                credential.getModel(),
                credential.getStatus(),
                TimeUtils.toInstant(credential.getLastValidatedAt()),
                credential.getLastValidationError());
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
