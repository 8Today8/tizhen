package com.tizhen.app.api.admin.dto;

/**
 * 头像上传结果。url 为站内相对地址，可直接用于 img src。
 */
public record AvatarUploadView(String url) {
}
