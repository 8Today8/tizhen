package com.tizhen.app.api.external.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增或覆盖当前自然周的手动申报。统计周由服务端计算，客户端不可提交。
 */
public class UpsertManualDeclarationRequest {

    @NotBlank(message = "平台不能为空")
    @Size(max = 32, message = "平台名称不能超过 32 个字符")
    private String platform;

    @NotBlank(message = "平台账号不能为空")
    @Size(max = 128, message = "平台账号不能超过 128 个字符")
    private String account;

    @Size(max = 512, message = "主页地址不能超过 512 个字符")
    private String profileUrl;

    @NotNull(message = "过题数不能为空")
    @Min(value = 0, message = "过题数不能小于 0")
    @Max(value = 100000, message = "过题数不能超过 100000")
    private Integer count;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
