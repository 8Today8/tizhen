package com.tizhen.app.api.auth.dto;

import com.tizhen.core.account.model.PlayerProfileUpdate;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

/**
 * 当前选手资料的部分更新请求。字段是否出现由 setter 记录，
 * 用于区分“未提交”与“显式传 null 清空”。
 */
public class UpdateProfileRequest {

    private final Set<String> presentFields = new HashSet<>();

    @Size(max = 64, message = "昵称不能超过 64 个字符")
    private String nickname;

    @Size(max = 254, message = "邮箱不能超过 254 个字符")
    private String email;

    @Size(max = 32, message = "手机号不能超过 32 个字符")
    private String phone;

    @Size(max = 512, message = "头像地址不能超过 512 个字符")
    private String avatarUrl;

    @Size(max = 128, message = "学校不能超过 128 个字符")
    private String school;

    public PlayerProfileUpdate toUpdate() {
        return new PlayerProfileUpdate(nickname, email, phone, avatarUrl, school, Set.copyOf(presentFields));
    }

    public Set<String> getPresentFields() {
        return Set.copyOf(presentFields);
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        presentFields.add("nickname");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        presentFields.add("email");
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        presentFields.add("phone");
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        presentFields.add("avatarUrl");
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
        presentFields.add("school");
    }
}
