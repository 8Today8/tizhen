package com.tizhen.core.datagen.model;

import java.time.Instant;

/**
 * 造数据结果文件元数据。不暴露服务器路径。
 */
public record DataGenFileView(
        String id,
        int groupIndex,
        Long inputSizeBytes,
        Long outputSizeBytes,
        Integer stdTimeMs,
        String validationStatus,
        String validationMessage,
        boolean inputAvailable,
        boolean outputAvailable,
        Instant fileDeletedAt
) {
}
