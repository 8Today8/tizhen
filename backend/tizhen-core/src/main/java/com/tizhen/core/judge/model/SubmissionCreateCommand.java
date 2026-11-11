package com.tizhen.core.judge.model;

import java.util.List;

/**
 * 选手提交命令。按题型只使用其中一组字段。
 */
public record SubmissionCreateCommand(
        String language,
        String code,
        List<String> selectedOptions,
        String answerText
) {
}
