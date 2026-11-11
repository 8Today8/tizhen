package com.tizhen.core.account.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.core.account.entity.AppUser;
import com.tizhen.core.account.mapper.AppUserMapper;
import com.tizhen.core.account.model.PlayerProfile;
import com.tizhen.core.account.model.PlayerProfileUpdate;
import com.tizhen.core.infra.file.AvatarStorageService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 当前选手资料维护。PATCH 语义下只更新客户端实际提交的字段，
 * 手机号/头像/学校允许显式传 null 清空。
 */
@Service
public class PlayerProfileService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final String ACTIVE_STATUS = "active";

    private final AppUserMapper appUserMapper;
    private final AvatarStorageService avatarStorageService;

    public PlayerProfileService(AppUserMapper appUserMapper,
                                AvatarStorageService avatarStorageService) {
        this.appUserMapper = appUserMapper;
        this.avatarStorageService = avatarStorageService;
    }

    @Transactional
    public PlayerProfile updateProfile(Long userId, PlayerProfileUpdate update) {
        if (update.presentFields().isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "至少提供一个需要修改的字段");
        }
        AppUser user = requireActiveUser(userId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LambdaUpdateWrapper<AppUser> wrapper = Wrappers.<AppUser>lambdaUpdate()
                .eq(AppUser::getId, userId);

        if (update.hasField("nickname")) {
            wrapper.set(AppUser::getNickname, normalizeNickname(update.nickname()));
        }
        if (update.hasField("email")) {
            String email = normalizeEmail(update.email());
            assertEmailAvailable(userId, email);
            wrapper.set(AppUser::getEmail, email);
        }
        if (update.hasField("phone")) {
            wrapper.set(AppUser::getPhone, normalizePhone(update.phone()));
        }

        String avatarUrl = update.hasField("avatarUrl")
                ? normalizeAvatarUrl(update.avatarUrl())
                : user.getAvatarUrl();
        if (update.hasField("avatarUrl")) {
            wrapper.set(AppUser::getAvatarUrl, avatarUrl);
        }
        if (update.hasField("school")) {
            wrapper.set(AppUser::getSchool, normalizeSchool(update.school()));
        }
        wrapper.set(AppUser::getUpdatedAt, now);

        try {
            appUserMapper.update(null, wrapper);
        } catch (DuplicateKeyException exception) {
            // 并发修改邮箱时由数据库唯一约束兜底。
            throw new BusinessException(ErrorCode.AUTH_EMAIL_TAKEN);
        }

        if (!Objects.equals(user.getAvatarUrl(), avatarUrl) && user.getAvatarUrl() != null) {
            avatarStorageService.deleteByUrl(user.getAvatarUrl());
        }
        return PlayerProfile.from(requireActiveUser(userId));
    }

    /**
     * 绑定新上传的头像；旧头像文件在替换成功后清理。
     */
    @Transactional
    public PlayerProfile updateAvatar(Long userId, String avatarUrl) {
        AppUser user = requireActiveUser(userId);
        String normalized = avatarStorageService.normalizeAvatarUrl(avatarUrl);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "头像地址不合法");
        }
        appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                .eq(AppUser::getId, userId)
                .set(AppUser::getAvatarUrl, normalized)
                .set(AppUser::getUpdatedAt, LocalDateTime.now(ZoneOffset.UTC)));
        if (!Objects.equals(user.getAvatarUrl(), normalized)) {
            avatarStorageService.deleteByUrl(user.getAvatarUrl());
        }
        return PlayerProfile.from(requireActiveUser(userId));
    }

    private AppUser requireActiveUser(Long userId) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null || user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_UNAUTHENTICATED);
        }
        if (!ACTIVE_STATUS.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        return user;
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "昵称不能为空");
        }
        String normalized = nickname.trim();
        if (normalized.isEmpty() || normalized.length() > 64) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "昵称需为 1–64 个字符");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "邮箱不能为空");
        }
        String normalized = email.trim();
        if (normalized.length() > 254 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "邮箱格式不正确");
        }
        return normalized;
    }

    private void assertEmailAvailable(Long userId, String email) {
        long emailCount = appUserMapper.selectCount(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getEmail, email)
                .ne(AppUser::getId, userId));
        if (emailCount > 0) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_TAKEN);
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.trim();
        if (normalized.isEmpty() || normalized.length() < 6 || normalized.length() > 32) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "手机号需为 6–32 个字符");
        }
        return normalized;
    }

    /**
     * 头像只接受 http/https 绝对地址或本服务签发的头像路径。
     */
    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl == null) {
            return null;
        }
        String normalized = avatarUrl.trim();
        if (normalized.isEmpty() || normalized.length() > 512) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "头像地址不合法");
        }
        if (normalized.startsWith(AvatarStorageService.PUBLIC_PATH_PREFIX)) {
            return avatarStorageService.normalizeAvatarUrl(normalized);
        }
        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "头像地址必须为 http/https URL");
            }
            return normalized;
        } catch (URISyntaxException exception) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "头像地址不合法");
        }
    }

    private String normalizeSchool(String school) {
        if (school == null || school.isBlank()) {
            return null;
        }
        String normalized = school.trim();
        if (normalized.length() > 128) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "学校不能超过 128 个字符");
        }
        return normalized;
    }
}
