package com.tizhen.core.external.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 选手外部平台账号。解绑记录保留历史数据。
 */
@TableName("external_account")
public class ExternalAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String platform;

    private String handle;

    private String profileUrl;

    private String verificationStatus;

    private String dataSource;

    private Integer weeklyCount;

    private LocalDate weeklyWeekStart;

    private Integer allTimeCount;

    private LocalDateTime lastSyncAt;

    private String status;

    private LocalDateTime unboundAt;

    private Integer activeFlag;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public Integer getWeeklyCount() {
        return weeklyCount;
    }

    public void setWeeklyCount(Integer weeklyCount) {
        this.weeklyCount = weeklyCount;
    }

    public LocalDate getWeeklyWeekStart() {
        return weeklyWeekStart;
    }

    public void setWeeklyWeekStart(LocalDate weeklyWeekStart) {
        this.weeklyWeekStart = weeklyWeekStart;
    }

    public Integer getAllTimeCount() {
        return allTimeCount;
    }

    public void setAllTimeCount(Integer allTimeCount) {
        this.allTimeCount = allTimeCount;
    }

    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(LocalDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUnboundAt() {
        return unboundAt;
    }

    public void setUnboundAt(LocalDateTime unboundAt) {
        this.unboundAt = unboundAt;
    }

    public Integer getActiveFlag() {
        return activeFlag;
    }

    public void setActiveFlag(Integer activeFlag) {
        this.activeFlag = activeFlag;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
