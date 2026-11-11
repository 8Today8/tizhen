package com.tizhen.app.api.admin.dto;

import com.tizhen.core.problem.model.ObjectiveOptionInput;
import com.tizhen.core.problem.model.ProblemCreateCommand;
import com.tizhen.core.problem.model.ProblemSampleInput;
import com.tizhen.core.problem.model.ScoringRubricItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增题目请求。状态由服务端固定为草稿。
 */
public record CreateProblemRequest(
        @NotBlank(message = "题型不能为空")
        @Size(max = 16, message = "题型取值非法")
        String type,

        @NotBlank(message = "题目名称不能为空")
        @Size(max = 200, message = "题目名称不能超过 200 个字符")
        String title,

        @Size(max = 100_000, message = "题面不能超过 100000 个字符")
        String statement,

        @NotBlank(message = "难度不能为空")
        String difficulty,

        @NotBlank(message = "来源不能为空")
        @Size(max = 64, message = "来源不能超过 64 个字符")
        String source,

        @Size(max = 20, message = "标签最多 20 个")
        List<@Size(max = 64, message = "单个标签不能超过 64 个字符") String> tags,

        @Size(max = 100_000, message = "输入格式不能超过 100000 个字符")
        String inputFormat,

        @Size(max = 100_000, message = "输出格式不能超过 100000 个字符")
        String outputFormat,

        @Min(value = 100, message = "时限不能少于 100 毫秒")
        @Max(value = 60_000, message = "时限不能超过 60000 毫秒")
        Integer timeLimitMs,

        @Min(value = 16, message = "内存限制不能少于 16 MB")
        @Max(value = 2_048, message = "内存限制不能超过 2048 MB")
        Integer memoryLimitMb,

        @Valid
        @Size(max = 20, message = "样例最多 20 组")
        List<ProblemSampleRequest> samples,

        String objectiveType,

        @Valid
        @Size(max = 10, message = "选项最多 10 个")
        List<ObjectiveOptionRequest> options,

        @Size(max = 100_000, message = "解析不能超过 100000 个字符")
        String explanation,

        @Size(max = 20_000, message = "参考答案不能超过 20000 个字符")
        String referenceAnswer,

        @Valid
        @Size(max = 20, message = "评分要点最多 20 项")
        List<ScoringRubricRequest> scoringRubric,

        Integer maxScore,

        @Size(max = 512, message = "外链地址不能超过 512 个字符")
        String externalUrl,

        @Size(max = 32, message = "平台不能超过 32 个字符")
        String platform
) {

    public ProblemCreateCommand toCommand() {
        return new ProblemCreateCommand(
                type,
                title,
                statement,
                difficulty,
                source,
                tags,
                inputFormat,
                outputFormat,
                timeLimitMs,
                memoryLimitMb,
                toSampleInputs(samples),
                objectiveType,
                toOptionInputs(options),
                explanation,
                referenceAnswer,
                toRubricItems(scoringRubric),
                maxScore,
                externalUrl,
                platform);
    }

    static List<ProblemSampleInput> toSampleInputs(List<ProblemSampleRequest> samples) {
        if (samples == null) {
            return null;
        }
        return samples.stream()
                .map(sample -> sample == null ? null : new ProblemSampleInput(
                        sample.inputData(), sample.outputData(),
                        sample.explanation(), sample.sortOrder()))
                .toList();
    }

    static List<ObjectiveOptionInput> toOptionInputs(List<ObjectiveOptionRequest> options) {
        if (options == null) {
            return null;
        }
        return options.stream()
                .map(option -> option == null ? null : new ObjectiveOptionInput(
                        option.optionKey(), option.content(),
                        option.isCorrect(), option.sortOrder()))
                .toList();
    }

    static List<ScoringRubricItem> toRubricItems(List<ScoringRubricRequest> rubric) {
        if (rubric == null) {
            return null;
        }
        return rubric.stream()
                .map(item -> item == null ? null : new ScoringRubricItem(
                        item.point(), item.score(), item.description()))
                .toList();
    }
}
