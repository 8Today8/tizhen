package com.tizhen.app.api.media;

import com.tizhen.core.infra.file.AvatarStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 公开头像读取。文件标识随机且不可枚举，仅允许读取服务器签发的头像地址。
 */
@RestController
public class AvatarMediaController {

    private static final String NOSNIFF = "nosniff";

    private final AvatarStorageService avatarStorageService;

    public AvatarMediaController(AvatarStorageService avatarStorageService) {
        this.avatarStorageService = avatarStorageService;
    }

    @GetMapping("/api/v1/media/avatar/**")
    public ResponseEntity<byte[]> getAvatar(HttpServletRequest request) {
        String requestUri = request.getRequestURI().substring(request.getContextPath().length());
        String objectKey = requestUri.substring(AvatarStorageService.PUBLIC_PATH_PREFIX.length());
        AvatarStorageService.AvatarContent content = avatarStorageService.read(objectKey);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
                .header("X-Content-Type-Options", NOSNIFF)
                .body(content.bytes());
    }
}
