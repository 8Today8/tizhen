package com.tizhen.core.account.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.core.account.entity.AppUser;
import com.tizhen.core.account.mapper.AppUserMapper;
import com.tizhen.core.account.model.PlayerProfile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

/**
 * 选手账号注册与认证。登录失败统一返回相同错误，避免暴露账号是否存在。
 */
@Service
public class PlayerAuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final String ACTIVE_STATUS = "active";

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final PlayerLoginRateLimiter rateLimiter;
    private final String dummyPasswordHash;

    public PlayerAuthService(
            AppUserMapper appUserMapper,
            PasswordEncoder passwordEncoder,
            PlayerLoginRateLimiter rateLimiter) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.dummyPasswordHash = passwordEncoder.encode("tizhen-player-dummy-password");
    }

    @Transactional
    public PlayerProfile register(String username, String nickname, String password, String confirmPassword) {
        String normalizedUsername = normalizeNewUsername(username);
        String normalizedNickname = normalizeNickname(nickname);
        validatePassword(password);
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "两次输入的密码不一致");
        }

        long usernameCount = appUserMapper.selectCount(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getUsername, normalizedUsername));
        if (usernameCount > 0) {
            throw new BusinessException(ErrorCode.AUTH_USERNAME_TAKEN);
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        AppUser user = new AppUser();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(normalizedNickname);
        user.setStatus(ACTIVE_STATUS);
        user.setLastLoginAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        try {
            appUserMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            // 并发注册时由数据库唯一约束兜底。
            throw new BusinessException(ErrorCode.AUTH_USERNAME_TAKEN);
        }
        return PlayerProfile.from(user);
    }

    @Transactional
    public PlayerProfile login(String username, String password, String ipAddress) {
        String normalizedUsername = username == null ? "" : username.trim();
        rateLimiter.assertNotLocked(normalizedUsername, ipAddress);

        AppUser user = findActiveCandidate(normalizedUsername);
        if (user == null) {
            passwordEncoder.matches(password == null ? "" : password, dummyPasswordHash);
            throw invalidCredentials(normalizedUsername, ipAddress);
        }
        if (!passwordEncoder.matches(password == null ? "" : password, user.getPasswordHash())) {
            throw invalidCredentials(normalizedUsername, ipAddress);
        }
        if (!ACTIVE_STATUS.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        rateLimiter.reset(normalizedUsername, ipAddress);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                .eq(AppUser::getId, user.getId())
                .set(AppUser::getLastLoginAt, now));
        user.setLastLoginAt(now);
        return PlayerProfile.from(user);
    }

    @Transactional(readOnly = true)
    public PlayerProfile loadProfile(Long userId) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null || user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_UNAUTHENTICATED);
        }
        if (!ACTIVE_STATUS.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        return PlayerProfile.from(user);
    }

    private AppUser findActiveCandidate(String username) {
        if (username.isEmpty()) {
            return null;
        }
        return appUserMapper.selectOne(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getUsername, username)
                .isNull(AppUser::getDeletedAt)
                .last("LIMIT 1"));
    }

    private String normalizeNewUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED,
                    "用户名需为 3–20 位字母、数字或下划线");
        }
        return normalized;
    }

    private String normalizeNickname(String nickname) {
        String normalized = nickname == null ? "" : nickname.trim();
        if (normalized.isEmpty() || normalized.length() > 64) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "昵称需为 1–64 个字符");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 64) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "密码需为 6–64 个字符");
        }
    }

    private BusinessException invalidCredentials(String username, String ipAddress) {
        boolean locked = rateLimiter.recordFailure(username, ipAddress);
        if (locked) {
            return new BusinessException(ErrorCode.COMMON_RATE_LIMITED, "登录失败次数过多，请稍后重试");
        }
        return new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }
}
