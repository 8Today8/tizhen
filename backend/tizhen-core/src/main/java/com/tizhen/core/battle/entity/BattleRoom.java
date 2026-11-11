package com.tizhen.core.battle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 对战房间。房间结束后直接作为对战记录，双方提交通过 submission.battle_id 关联。
 */
@TableName("battle_room")
public class BattleRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roomCode;

    private Long creatorId;

    private Long opponentId;

    private Long problemId;

    private String filterDifficulty;

    private String filterTag;

    private Integer creatorReady;

    private Integer opponentReady;

    private String status;

    private String result;

    private String endReason;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public Long getOpponentId() {
        return opponentId;
    }

    public void setOpponentId(Long opponentId) {
        this.opponentId = opponentId;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public String getFilterDifficulty() {
        return filterDifficulty;
    }

    public void setFilterDifficulty(String filterDifficulty) {
        this.filterDifficulty = filterDifficulty;
    }

    public String getFilterTag() {
        return filterTag;
    }

    public void setFilterTag(String filterTag) {
        this.filterTag = filterTag;
    }

    public Integer getCreatorReady() {
        return creatorReady;
    }

    public void setCreatorReady(Integer creatorReady) {
        this.creatorReady = creatorReady;
    }

    public Integer getOpponentReady() {
        return opponentReady;
    }

    public void setOpponentReady(Integer opponentReady) {
        this.opponentReady = opponentReady;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getEndReason() {
        return endReason;
    }

    public void setEndReason(String endReason) {
        this.endReason = endReason;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
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
