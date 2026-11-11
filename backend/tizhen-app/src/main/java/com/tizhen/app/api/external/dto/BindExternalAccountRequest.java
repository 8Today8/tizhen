package com.tizhen.app.api.external.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 绑定外部平台账号。首期仅支持 codeforces，具体格式由服务层进一步校验。
 */
public class BindExternalAccountRequest {

    @NotBlank(message = "平台不能为空")
    @Size(max = 32, message = "平台名称不能超过 32 个字符")
    private String platform;

    @NotBlank(message = "账号不能为空")
    @Size(max = 24, message = "Codeforces Handle 不能超过 24 个字符")
    private String handle;

    @Size(max = 512, message = "主页地址不能超过 512 个字符")
    private String profileUrl;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }
}
