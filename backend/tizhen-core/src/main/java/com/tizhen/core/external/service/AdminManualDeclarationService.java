package com.tizhen.core.external.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.entity.AdminUser;
import com.tizhen.core.account.entity.AppUser;
import com.tizhen.core.account.mapper.AdminUserMapper;
import com.tizhen.core.account.mapper.AppUserMapper;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.model.LoginMetadata;
import com.tizhen.core.account.model.UserBrief;
import com.tizhen.core.account.service.AdminAuthService;
import com.tizhen.core.admin.service.OperationLogService;
import com.tizhen.core.external.entity.ManualDeclaration;
import com.tizhen.core.external.mapper.ManualDeclarationMapper;
import com.tizhen.core.external.model.AdminManualDeclarationItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台手动申报记录查询与审核。审核仅超级管理员可执行。
 */
@Service
public class AdminManualDeclarationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int REVIEW_NOTE_MAX_LENGTH = 200;

    private final ManualDeclarationMapper manualDeclarationMapper;
    private final AppUserMapper appUserMapper;
    private final AdminUserMapper adminUserMapper;
    private final OperationLogService operationLogService;

    public AdminManualDeclarationService(
            ManualDeclarationMapper manualDeclarationMapper,
            AppUserMapper appUserMapper,
            AdminUserMapper adminUserMapper,
            OperationLogService operationLogService) {
        this.manualDeclarationMapper = manualDeclarationMapper;
        this.appUserMapper = appUserMapper;
        this.adminUserMapper = adminUserMapper;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public PageResult<AdminManualDeclarationItem> list(Long userId,
                                                       String platform,
                                                       LocalDate weekStartDate,
                                                       int page,
                                                       int pageSize) {
        String normalizedPlatform = platform == null ? null : platform.trim();
        if (normalizedPlatform != null && normalizedPlatform.isEmpty()) {
            normalizedPlatform = null;
        }
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<ManualDeclaration> result = new Page<>(safePage, safePageSize);
        manualDeclarationMapper.selectPage(result, Wrappers.<ManualDeclaration>lambdaQuery()
                .eq(userId != null, ManualDeclaration::getUserId, userId)
                .eq(normalizedPlatform != null, ManualDeclaration::getPlatform, normalizedPlatform)
                .eq(weekStartDate != null, ManualDeclaration::getWeekStartDate, weekStartDate)
                .orderByDesc(ManualDeclaration::getUpdatedAt)
                .orderByDesc(ManualDeclaration::getId));

        List<Long> userIds = result.getRecords().stream()
                .map(ManualDeclaration::getUserId)
                .distinct()
                .toList();
        Map<Long, AppUser> users = userIds.isEmpty()
                ? Map.of()
                : appUserMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(AppUser::getId, Function.identity()));

        List<Long> reviewerIds = result.getRecords().stream()
                .map(ManualDeclaration::getReviewedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, AdminUser> reviewers = reviewerIds.isEmpty()
                ? Map.of()
                : adminUserMapper.selectBatchIds(reviewerIds).stream()
                        .collect(Collectors.toMap(AdminUser::getId, Function.identity()));

        List<AdminManualDeclarationItem> items = result.getRecords().stream()
                .map(declaration -> toItem(
                        declaration,
                        users.get(declaration.getUserId()),
                        declaration.getReviewedBy() == null
                                ? null
                                : reviewers.get(declaration.getReviewedBy())))
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    /**
     * 审核手动申报。未审核不计分；通过后计入周榜/总榜，拒绝后不计分，二者均可改判。
     */
    @Transactional
    public AdminManualDeclarationItem review(AdminPrincipal actor,
                                             Long declarationId,
                                             String status,
                                             String note,
                                             LoginMetadata metadata) {
        if (!AdminAuthService.isSuperAdmin(actor)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "仅超级管理员可审核手动申报");
        }
        String normalizedStatus = normalizeReviewStatus(status);
        String normalizedNote = normalizeReviewNote(note);

        ManualDeclaration declaration = manualDeclarationMapper.selectById(declarationId);
        if (declaration == null) {
            throw new BusinessException(ErrorCode.MANUAL_DECLARATION_NOT_FOUND);
        }
        if (declaration.getActiveFlag() == null || declaration.getActiveFlag() != 1) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "已失效的申报不可审核");
        }

        String previousStatus = declaration.getVerificationStatus();
        int updated = manualDeclarationMapper.update(null, Wrappers.<ManualDeclaration>lambdaUpdate()
                .eq(ManualDeclaration::getId, declarationId)
                .eq(ManualDeclaration::getActiveFlag, 1)
                .set(ManualDeclaration::getVerificationStatus, normalizedStatus)
                .set(ManualDeclaration::getReviewNote, normalizedNote)
                .set(ManualDeclaration::getReviewedBy, actor.id())
                .set(ManualDeclaration::getReviewedAt, TimeUtils.toUtcLocalDateTime(Instant.now())));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "申报状态已变化，请刷新后重试");
        }

        ManualDeclaration reviewed = manualDeclarationMapper.selectById(declarationId);
        AppUser user = appUserMapper.selectById(reviewed.getUserId());
        AdminUser reviewer = adminUserMapper.selectById(reviewed.getReviewedBy());

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("fromStatus", previousStatus);
        extra.put("toStatus", normalizedStatus);
        if (normalizedNote != null) {
            extra.put("note", normalizedNote);
        }
        operationLogService.record(
                actor.id(),
                "review",
                "manual_declaration",
                declarationId,
                reviewDetail(reviewed, user, normalizedStatus),
                extra,
                metadata);

        return toItem(reviewed, user, reviewer);
    }

    private AdminManualDeclarationItem toItem(ManualDeclaration declaration,
                                              AppUser user,
                                              AdminUser reviewer) {
        UserBrief userBrief = user == null
                ? new UserBrief(String.valueOf(declaration.getUserId()), null, null, null, null)
                : UserBrief.from(user);
        String reviewerName = reviewer == null
                ? null
                : (reviewer.getDisplayName() == null || reviewer.getDisplayName().isBlank()
                        ? reviewer.getUsername()
                        : reviewer.getDisplayName());
        return new AdminManualDeclarationItem(
                String.valueOf(declaration.getId()),
                userBrief,
                user == null ? null : user.getUsername(),
                declaration.getPlatform(),
                declaration.getAccount(),
                declaration.getProfileUrl(),
                declaration.getCount() == null ? 0 : declaration.getCount(),
                declaration.getWeekStartDate(),
                declaration.getVerificationStatus(),
                declaration.getReviewNote(),
                declaration.getReviewedBy() == null ? null : String.valueOf(declaration.getReviewedBy()),
                reviewerName,
                TimeUtils.toInstant(declaration.getReviewedAt()),
                declaration.getActiveFlag() != null && declaration.getActiveFlag() == 1,
                TimeUtils.toInstant(declaration.getCreatedAt()),
                TimeUtils.toInstant(declaration.getUpdatedAt()));
    }

    private String normalizeReviewStatus(String status) {
        String normalized = status == null ? null : status.trim();
        if (!"verified".equals(normalized) && !"rejected".equals(normalized)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_PARAM, "审核状态必须为 verified 或 rejected");
        }
        return normalized;
    }

    private String normalizeReviewNote(String note) {
        if (note == null) {
            return null;
        }
        String normalized = note.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > REVIEW_NOTE_MAX_LENGTH) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_PARAM, "审核备注不能超过 200 个字符");
        }
        return normalized;
    }

    private String reviewDetail(ManualDeclaration declaration, AppUser user, String status) {
        String result = "verified".equals(status) ? "通过" : "拒绝";
        String username = user == null ? String.valueOf(declaration.getUserId()) : user.getUsername();
        return result + "手动申报（用户 " + username
                + "，平台 " + declaration.getPlatform()
                + "，统计周 " + declaration.getWeekStartDate()
                + "，过题数 " + (declaration.getCount() == null ? 0 : declaration.getCount()) + "）";
    }
}
