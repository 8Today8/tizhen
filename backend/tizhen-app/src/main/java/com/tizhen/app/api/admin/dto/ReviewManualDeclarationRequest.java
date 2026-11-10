package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 手动申报审核请求。审核结果仅允许通过或拒绝，备注选填。
 */
public record ReviewManualDeclarationRequest(
        @NotBlank(message = "请选择审核结果")
        @Pattern(regexp = "verified|rejected", message = "审核结果必须为 verified 或 rejected")
        String status,

        @Size(max = 200, message = "审核备注不能超过 200 个字符")
        String note
) {
}
