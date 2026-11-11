package com.tizhen.core.external.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.external.entity.ExternalAccount;
import com.tizhen.core.external.mapper.ExternalAccountMapper;
import com.tizhen.core.external.model.ExternalAccountList;
import com.tizhen.core.external.model.ExternalAccountView;
import com.tizhen.core.infra.cf.CodeforcesClient;
import com.tizhen.core.infra.cf.CodeforcesSolvedStats;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 选手外部平台账号绑定。首期仅支持 Codeforces，绑定与刷新均读取真实公共 API。
 */
@Service
public class ExternalAccountService {

    private static final String PLATFORM_CODEFORCES = "codeforces";
    private static final String CODEFORCES_PROFILE_PREFIX = "https://codeforces.com/profile/";
    private static final int PROFILE_URL_MAX_LENGTH = 512;
    private static final Pattern HANDLE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,24}$");

    private final ExternalAccountMapper externalAccountMapper;
    private final CodeforcesClient codeforcesClient;

    public ExternalAccountService(ExternalAccountMapper externalAccountMapper,
                                  CodeforcesClient codeforcesClient) {
        this.externalAccountMapper = externalAccountMapper;
        this.codeforcesClient = codeforcesClient;
    }

    @Transactional(readOnly = true)
    public ExternalAccountList list(Long userId) {
        List<ExternalAccount> accounts = externalAccountMapper.selectList(
                Wrappers.<ExternalAccount>lambdaQuery()
                        .eq(ExternalAccount::getUserId, userId)
                        .eq(ExternalAccount::getStatus, "active")
                        .eq(ExternalAccount::getActiveFlag, 1)
                        .orderByDesc(ExternalAccount::getCreatedAt)
                        .orderByDesc(ExternalAccount::getId));
        return new ExternalAccountList(accounts.stream().map(this::toView).toList());
    }

    public ExternalAccountView bind(Long userId, String platform, String handle, String profileUrl) {
        String normalizedPlatform = normalizePlatform(platform);
        String normalizedHandle = normalizeHandle(handle);
        String normalizedProfileUrl = normalizeProfileUrl(profileUrl, normalizedHandle);

        long activeBindingCount = externalAccountMapper.selectCount(
                Wrappers.<ExternalAccount>lambdaQuery()
                        .eq(ExternalAccount::getUserId, userId)
                        .eq(ExternalAccount::getPlatform, normalizedPlatform)
                        .eq(ExternalAccount::getActiveFlag, 1));
        if (activeBindingCount > 0) {
            throw new BusinessException(ErrorCode.EXTERNAL_ACCOUNT_ALREADY_BOUND);
        }
        long handleOwnerCount = externalAccountMapper.selectCount(
                Wrappers.<ExternalAccount>lambdaQuery()
                        .eq(ExternalAccount::getPlatform, normalizedPlatform)
                        .eq(ExternalAccount::getHandle, normalizedHandle)
                        .eq(ExternalAccount::getActiveFlag, 1));
        if (handleOwnerCount > 0) {
            throw new BusinessException(ErrorCode.EXTERNAL_HANDLE_TAKEN);
        }

        CodeforcesSolvedStats stats = fetchSolvedStats(normalizedHandle);
        LocalDateTime now = TimeUtils.toUtcLocalDateTime(Instant.now());
        ExternalAccount account = new ExternalAccount();
        account.setUserId(userId);
        account.setPlatform(normalizedPlatform);
        account.setHandle(normalizedHandle);
        account.setProfileUrl(normalizedProfileUrl);
        account.setVerificationStatus("verified");
        account.setDataSource("api");
        account.setWeeklyCount(stats.weeklySolved());
        account.setWeeklyWeekStart(stats.weekStartDate());
        account.setAllTimeCount(stats.allTimeSolved());
        account.setLastSyncAt(now);
        account.setStatus("active");
        account.setActiveFlag(1);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);

        try {
            externalAccountMapper.insert(account);
        } catch (DuplicateKeyException exception) {
            // 唯一索引兜底并发绑定：重新判断冲突来源，避免把数据库异常暴露给前端。
            long currentUserBindingCount = externalAccountMapper.selectCount(
                    Wrappers.<ExternalAccount>lambdaQuery()
                            .eq(ExternalAccount::getUserId, userId)
                            .eq(ExternalAccount::getPlatform, normalizedPlatform)
                            .eq(ExternalAccount::getActiveFlag, 1));
            throw new BusinessException(currentUserBindingCount > 0
                    ? ErrorCode.EXTERNAL_ACCOUNT_ALREADY_BOUND
                    : ErrorCode.EXTERNAL_HANDLE_TAKEN);
        }
        return toView(account);
    }

    @Transactional
    public void unbind(Long userId, Long accountId) {
        LocalDateTime now = TimeUtils.toUtcLocalDateTime(Instant.now());
        int updated = externalAccountMapper.update(null, Wrappers.<ExternalAccount>lambdaUpdate()
                .eq(ExternalAccount::getId, accountId)
                .eq(ExternalAccount::getUserId, userId)
                .eq(ExternalAccount::getActiveFlag, 1)
                .set(ExternalAccount::getStatus, "unbound")
                .set(ExternalAccount::getActiveFlag, null)
                .set(ExternalAccount::getUnboundAt, now)
                .set(ExternalAccount::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.EXTERNAL_ACCOUNT_NOT_FOUND);
        }
    }

    private String normalizePlatform(String platform) {
        String normalized = platform == null ? "" : platform.trim().toLowerCase(Locale.ROOT);
        if (!PLATFORM_CODEFORCES.equals(normalized)) {
            throw new BusinessException(ErrorCode.EXTERNAL_PLATFORM_UNSUPPORTED);
        }
        return normalized;
    }

    private String normalizeHandle(String handle) {
        String normalized = handle == null ? "" : handle.trim();
        if (normalized.isEmpty() || !HANDLE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.EXTERNAL_HANDLE_INVALID,
                    "Codeforces Handle 需为 3–24 位字母、数字、下划线或短横线");
        }
        return normalized;
    }

    private CodeforcesSolvedStats fetchSolvedStats(String handle) {
        try {
            return codeforcesClient.fetchSolvedStats(handle);
        } catch (CodeforcesClient.HandleNotFoundException exception) {
            throw new BusinessException(ErrorCode.EXTERNAL_HANDLE_INVALID, "Codeforces 账号不存在");
        } catch (CodeforcesClient.UnavailableException exception) {
            throw new BusinessException(ErrorCode.EXTERNAL_CF_UNAVAILABLE);
        }
    }

    private String normalizeProfileUrl(String profileUrl, String handle) {
        if (profileUrl == null || profileUrl.isBlank()) {
            return CODEFORCES_PROFILE_PREFIX + handle;
        }
        String normalized = profileUrl.trim();
        if (normalized.length() > PROFILE_URL_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "主页地址不能超过 512 个字符");
        }
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            if (!httpScheme || uri.getHost() == null) {
                throw new IllegalArgumentException("invalid url");
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "主页地址不是合法 URL");
        }
        return normalized;
    }

    private ExternalAccountView toView(ExternalAccount account) {
        return new ExternalAccountView(
                String.valueOf(account.getId()),
                account.getPlatform(),
                account.getHandle(),
                account.getProfileUrl(),
                account.getVerificationStatus(),
                account.getDataSource(),
                account.getWeeklyCount() == null ? 0 : account.getWeeklyCount(),
                account.getWeeklyWeekStart(),
                account.getAllTimeCount() == null ? 0 : account.getAllTimeCount(),
                TimeUtils.toInstant(account.getLastSyncAt()),
                account.getStatus(),
                TimeUtils.toInstant(account.getUnboundAt()),
                TimeUtils.toInstant(account.getCreatedAt()));
    }
}
