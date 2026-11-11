package com.tizhen.app.api.auth;

import com.tizhen.app.api.auth.dto.UpdateProfileRequest;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.core.account.model.PlayerPrincipal;
import com.tizhen.core.account.model.PlayerProfile;
import com.tizhen.core.account.model.PlayerStats;
import com.tizhen.core.account.service.PlayerAuthService;
import com.tizhen.core.account.service.PlayerProfileService;
import com.tizhen.core.account.service.PlayerStatsService;
import com.tizhen.core.infra.file.AvatarStorageService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 当前选手资料与统计。接口契约见 docs/design/04.API接口设计.md §6.1.1 ME-01～ME-04。
 */
@RestController
@RequestMapping("/api/v1/me")
public class PlayerProfileController {

    private final PlayerAuthService playerAuthService;
    private final PlayerProfileService playerProfileService;
    private final PlayerStatsService playerStatsService;
    private final AvatarStorageService avatarStorageService;

    public PlayerProfileController(PlayerAuthService playerAuthService,
                                   PlayerProfileService playerProfileService,
                                   PlayerStatsService playerStatsService,
                                   AvatarStorageService avatarStorageService) {
        this.playerAuthService = playerAuthService;
        this.playerProfileService = playerProfileService;
        this.playerStatsService = playerStatsService;
        this.avatarStorageService = avatarStorageService;
    }

    @GetMapping
    public ApiResponse<PlayerProfile> me(@AuthenticationPrincipal PlayerPrincipal principal) {
        return ApiResponse.ok(playerAuthService.loadProfile(principal.id()));
    }

    @PatchMapping
    public ApiResponse<PlayerProfile> updateMe(@AuthenticationPrincipal PlayerPrincipal principal,
                                               @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(playerProfileService.updateProfile(principal.id(), request.toUpdate()));
    }

    /**
     * 选手上传头像：校验并落盘后立即绑定到当前账号，返回最新资料。
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PlayerProfile> uploadAvatar(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "请选择要上传的头像图片");
        }
        try {
            String avatarUrl = avatarStorageService.store(
                    file.getInputStream(), file.getSize(), file.getOriginalFilename());
            try {
                return ApiResponse.ok(playerProfileService.updateAvatar(principal.id(), avatarUrl));
            } catch (RuntimeException exception) {
                // 绑定失败时清理刚写入的文件，避免产生孤立头像。
                avatarStorageService.deleteByUrl(avatarUrl);
                throw exception;
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "头像上传失败，请稍后重试");
        }
    }

    @GetMapping("/stats")
    public ApiResponse<PlayerStats> stats(@AuthenticationPrincipal PlayerPrincipal principal) {
        return ApiResponse.ok(playerStatsService.getStats(principal.id()));
    }
}
