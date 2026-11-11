package com.tizhen.core.datagen.model;

import java.util.List;

/**
 * 创建造数据任务的内部命令。
 */
public record DataGenCreateCommand(
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
        int timeLimitMs,
        int memoryLimitMb
) {
}
