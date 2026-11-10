package com.tizhen.core.datagen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 造数据结果文件元数据。存储路径只在服务内部使用。
 */
@TableName("data_gen_file")
public class DataGenFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Integer groupIndex;

    private String inputPath;

    private String outputPath;

    private Long inputSizeBytes;

    private Long outputSizeBytes;

    private Integer stdTimeMs;

    private String validationStatus;

    private String validationMessage;

    private LocalDateTime fileDeletedAt;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getGroupIndex() {
        return groupIndex;
    }

    public void setGroupIndex(Integer groupIndex) {
        this.groupIndex = groupIndex;
    }

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public Long getInputSizeBytes() {
        return inputSizeBytes;
    }

    public void setInputSizeBytes(Long inputSizeBytes) {
        this.inputSizeBytes = inputSizeBytes;
    }

    public Long getOutputSizeBytes() {
        return outputSizeBytes;
    }

    public void setOutputSizeBytes(Long outputSizeBytes) {
        this.outputSizeBytes = outputSizeBytes;
    }

    public Integer getStdTimeMs() {
        return stdTimeMs;
    }

    public void setStdTimeMs(Integer stdTimeMs) {
        this.stdTimeMs = stdTimeMs;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    public LocalDateTime getFileDeletedAt() {
        return fileDeletedAt;
    }

    public void setFileDeletedAt(LocalDateTime fileDeletedAt) {
        this.fileDeletedAt = fileDeletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
