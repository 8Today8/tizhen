package com.tizhen.app.api.admin;

import com.tizhen.app.api.admin.dto.AvatarUploadView;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.core.infra.file.AvatarStorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 后台图片上传。当前只开放头像场景，上传结果由业务接口落库。
 */
@RestController
@RequestMapping("/api/v1/admin/uploads")
public class AdminUploadController {

    private final AvatarStorageService avatarStorageService;

    public AdminUploadController(AvatarStorageService avatarStorageService) {
        this.avatarStorageService = avatarStorageService;
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AvatarUploadView> uploadAvatar(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "请选择要上传的头像图片");
        }
        try {
            String url = avatarStorageService.store(
                    file.getInputStream(), file.getSize(), file.getOriginalFilename());
            return ApiResponse.ok(new AvatarUploadView(url));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "头像上传失败，请稍后重试");
        }
    }
}
