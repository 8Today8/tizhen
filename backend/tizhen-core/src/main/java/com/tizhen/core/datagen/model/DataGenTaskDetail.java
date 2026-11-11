package com.tizhen.core.datagen.model;

import java.time.Instant;
import java.util.List;

/**
 * 选手造数据任务详情。刻意不包含 generatorCode。
 */
public record DataGenTaskDetail(
        String id,
        String title,
        String statement,
        String inputFormat,
        String outputFormat,
        String stdCode,
        String stdLanguage,
        List<DataRangeItem> dataRange,
        int groupCount,
        String generationMode,
        String specialRequirements,
        String llmProvider,
        String llmModel,
        String generatorStatus,
        String llmDataStatus,
        String status,
        String failureStage,
        String failureReason,
        Object resultSummary,
        int timeLimitMs,
        int memoryLimitMb,
        Instant startedAt,
        Instant finishedAt,
        Instant filesExpireAt,
        Instant filesCleanedAt,
        List<DataGenFileView> files
) {
}
