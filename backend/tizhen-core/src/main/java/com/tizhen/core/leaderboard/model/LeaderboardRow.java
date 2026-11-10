package com.tizhen.core.leaderboard.model;

import java.time.LocalDateTime;

/**
 * 榜单聚合查询行。分数口径由 SQL 按周榜/总榜动态切换。
 */
public class LeaderboardRow {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String school;
    private String email;
    private String cfHandle;
    private Integer internalSolved;
    private Integer cfSolved;
    private Integer manualSolved;
    private Integer manualDeclarationCount;
    private Integer totalScore;
    private Integer rank;
    private String cfVerificationStatus;
    private String cfDataSource;
    private LocalDateTime cfLastSyncAt;
    private LocalDateTime manualUpdatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCfHandle() {
        return cfHandle;
    }

    public void setCfHandle(String cfHandle) {
        this.cfHandle = cfHandle;
    }

    public Integer getInternalSolved() {
        return internalSolved;
    }

    public void setInternalSolved(Integer internalSolved) {
        this.internalSolved = internalSolved;
    }

    public Integer getCfSolved() {
        return cfSolved;
    }

    public void setCfSolved(Integer cfSolved) {
        this.cfSolved = cfSolved;
    }

    public Integer getManualSolved() {
        return manualSolved;
    }

    public void setManualSolved(Integer manualSolved) {
        this.manualSolved = manualSolved;
    }

    public Integer getManualDeclarationCount() {
        return manualDeclarationCount;
    }

    public void setManualDeclarationCount(Integer manualDeclarationCount) {
        this.manualDeclarationCount = manualDeclarationCount;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getCfVerificationStatus() {
        return cfVerificationStatus;
    }

    public void setCfVerificationStatus(String cfVerificationStatus) {
        this.cfVerificationStatus = cfVerificationStatus;
    }

    public String getCfDataSource() {
        return cfDataSource;
    }

    public void setCfDataSource(String cfDataSource) {
        this.cfDataSource = cfDataSource;
    }

    public LocalDateTime getCfLastSyncAt() {
        return cfLastSyncAt;
    }

    public void setCfLastSyncAt(LocalDateTime cfLastSyncAt) {
        this.cfLastSyncAt = cfLastSyncAt;
    }

    public LocalDateTime getManualUpdatedAt() {
        return manualUpdatedAt;
    }

    public void setManualUpdatedAt(LocalDateTime manualUpdatedAt) {
        this.manualUpdatedAt = manualUpdatedAt;
    }
}
