package com.tizhen.core.external.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.common.util.WeekUtils;
import com.tizhen.core.external.entity.ManualDeclaration;
import com.tizhen.core.external.mapper.ManualDeclarationMapper;
import com.tizhen.core.external.model.ManualDeclarationView;
import com.tizhen.core.external.model.PlayerManualDeclarationPage;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 选手手动申报。仅允许维护北京时间当前自然周的数据；
 * 提交后状态重置为未审核，由超级管理员审核通过后才计入榜单。
 */
@Service
public class ManualDeclarationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int PLATFORM_MAX_LENGTH = 32;
    private static final int ACCOUNT_MAX_LENGTH = 128;
    private static final int PROFILE_URL_MAX_LENGTH = 512;
    private static final int COUNT_MAX = 100_000;

    private final ManualDeclarationMapper manualDeclarationMapper;

    public ManualDeclarationService(ManualDeclarationMapper manualDeclarationMapper) {
        this.manualDeclarationMapper = manualDeclarationMapper;
    }

    @Transactional(readOnly = true)
    public PlayerManualDeclarationPage list(Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        LocalDate weekStartDate = WeekUtils.currentWeekStart();

        Page<ManualDeclaration> result = new Page<>(safePage, safePageSize);
        manualDeclarationMapper.selectPage(result, Wrappers.<ManualDeclaration>lambdaQuery()
                .eq(ManualDeclaration::getUserId, userId)
                .eq(ManualDeclaration::getWeekStartDate, weekStartDate)
                .eq(ManualDeclaration::getActiveFlag, 1)
                .orderByDesc(ManualDeclaration::getUpdatedAt)
                .orderByDesc(ManualDeclaration::getId));

        List<ManualDeclarationView> items = result.getRecords().stream()
                .map(this::toView)
                .toList();
        long totalPages = safePageSize <= 0
                ? 0
                : (result.getTotal() + safePageSize - 1) / safePageSize;
        return new PlayerManualDeclarationPage(
                items, safePage, safePageSize, result.getTotal(), totalPages, weekStartDate);
    }

    @Transactional
    public ManualDeclarationView upsert(Long userId,
                                        String platform,
                                        String account,
                                        String profileUrl,
                                        Integer count) {
        String normalizedPlatform = normalizePlatform(platform);
        String normalizedAccount = normalizeAccount(account);
        String normalizedProfileUrl = normalizeProfileUrl(profileUrl);
        int normalizedCount = normalizeCount(count);
        LocalDate weekStartDate = WeekUtils.currentWeekStart();
        LocalDateTime now = TimeUtils.toUtcLocalDateTime(Instant.now());

        ManualDeclaration existing = findActive(userId, normalizedPlatform, weekStartDate);
        if (existing != null) {
            updateActive(existing.getId(), normalizedAccount, normalizedProfileUrl, normalizedCount, now);
            return toView(manualDeclarationMapper.selectById(existing.getId()));
        }

        ManualDeclaration declaration = new ManualDeclaration();
        declaration.setUserId(userId);
        declaration.setPlatform(normalizedPlatform);
        declaration.setAccount(normalizedAccount);
        declaration.setProfileUrl(normalizedProfileUrl);
        declaration.setCount(normalizedCount);
        declaration.setWeekStartDate(weekStartDate);
        declaration.setVerificationStatus("unverified");
        declaration.setActiveFlag(1);
        declaration.setCreatedAt(now);
        declaration.setUpdatedAt(now);

        try {
            manualDeclarationMapper.insert(declaration);
        } catch (DuplicateKeyException exception) {
            // 并发提交由唯一索引兜底，改为覆盖已有有效记录。
            ManualDeclaration concurrent = findActive(userId, normalizedPlatform, weekStartDate);
            if (concurrent == null) {
                throw exception;
            }
            updateActive(concurrent.getId(), normalizedAccount, normalizedProfileUrl, normalizedCount, now);
            return toView(manualDeclarationMapper.selectById(concurrent.getId()));
        }
        return toView(declaration);
    }

    @Transactional
    public void delete(Long userId, Long declarationId) {
        ManualDeclaration declaration = manualDeclarationMapper.selectById(declarationId);
        if (declaration == null
                || !userId.equals(declaration.getUserId())
                || declaration.getActiveFlag() == null
                || declaration.getActiveFlag() != 1) {
            throw new BusinessException(ErrorCode.MANUAL_DECLARATION_NOT_FOUND);
        }
        if (!WeekUtils.currentWeekStart().equals(declaration.getWeekStartDate())) {
            throw new BusinessException(ErrorCode.MANUAL_DECLARATION_WEEK_LOCKED);
        }

        LocalDateTime now = TimeUtils.toUtcLocalDateTime(Instant.now());
        int updated = manualDeclarationMapper.update(null, Wrappers.<ManualDeclaration>lambdaUpdate()
                .eq(ManualDeclaration::getId, declarationId)
                .eq(ManualDeclaration::getUserId, userId)
                .eq(ManualDeclaration::getActiveFlag, 1)
                .set(ManualDeclaration::getActiveFlag, null)
                .set(ManualDeclaration::getDeletedAt, now)
                .set(ManualDeclaration::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.MANUAL_DECLARATION_NOT_FOUND);
        }
    }

    private ManualDeclaration findActive(Long userId, String platform, LocalDate weekStartDate) {
        return manualDeclarationMapper.selectOne(Wrappers.<ManualDeclaration>lambdaQuery()
                .eq(ManualDeclaration::getUserId, userId)
                .eq(ManualDeclaration::getPlatform, platform)
                .eq(ManualDeclaration::getWeekStartDate, weekStartDate)
                .eq(ManualDeclaration::getActiveFlag, 1)
                .last("LIMIT 1"));
    }

    private void updateActive(Long declarationId,
                              String account,
                              String profileUrl,
                              int count,
                              LocalDateTime now) {
        int updated = manualDeclarationMapper.update(null, Wrappers.<ManualDeclaration>lambdaUpdate()
                .eq(ManualDeclaration::getId, declarationId)
                .eq(ManualDeclaration::getActiveFlag, 1)
                .set(ManualDeclaration::getAccount, account)
                .set(ManualDeclaration::getProfileUrl, profileUrl)
                .set(ManualDeclaration::getCount, count)
                // 任何覆盖都视为重新申报，必须由超级管理员重新审核。
                .set(ManualDeclaration::getVerificationStatus, "unverified")
                .set(ManualDeclaration::getReviewNote, null)
                .set(ManualDeclaration::getReviewedBy, null)
                .set(ManualDeclaration::getReviewedAt, null)
                .set(ManualDeclaration::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "申报记录已变化，请刷新后重试");
        }
    }

    private String normalizePlatform(String platform) {
        String normalized = platform == null ? "" : platform.trim();
        if (normalized.isEmpty() || normalized.length() > PLATFORM_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "平台名称长度为 1–32 个字符");
        }
        return normalized;
    }

    private String normalizeAccount(String account) {
        String normalized = account == null ? "" : account.trim();
        if (normalized.isEmpty() || normalized.length() > ACCOUNT_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "平台账号长度为 1–128 个字符");
        }
        return normalized;
    }

    private String normalizeProfileUrl(String profileUrl) {
        if (profileUrl == null || profileUrl.isBlank()) {
            return null;
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

    private int normalizeCount(Integer count) {
        if (count == null || count < 0 || count > COUNT_MAX) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "过题数必须在 0–100000 之间");
        }
        return count;
    }

    private ManualDeclarationView toView(ManualDeclaration declaration) {
        return new ManualDeclarationView(
                String.valueOf(declaration.getId()),
                declaration.getPlatform(),
                declaration.getAccount(),
                declaration.getProfileUrl(),
                declaration.getCount() == null ? 0 : declaration.getCount(),
                declaration.getWeekStartDate(),
                declaration.getVerificationStatus(),
                TimeUtils.toInstant(declaration.getCreatedAt()),
                TimeUtils.toInstant(declaration.getUpdatedAt()));
    }
}
