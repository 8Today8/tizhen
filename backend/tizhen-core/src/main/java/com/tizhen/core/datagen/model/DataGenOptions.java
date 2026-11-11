package com.tizhen.core.datagen.model;

import com.tizhen.core.account.model.LlmCredentialView;

import java.util.List;

/**
 * 造数据工作台选项。
 */
public record DataGenOptions(
        List<ModeOption> modes,
        List<EnumOption> stdLanguages,
        List<String> dataRangeTypes,
        Limits limits,
        LlmCredentialView llmCredential,
        boolean hasActiveTask
) {

    public record ModeOption(String code, boolean requiresCredential) {
    }

    public record EnumOption(String value, String label) {
    }

    public record Limits(Range groupCount, Range timeLimitMs, Range memoryLimitMb) {
    }

    public record Range(int min, int max) {
    }
}
