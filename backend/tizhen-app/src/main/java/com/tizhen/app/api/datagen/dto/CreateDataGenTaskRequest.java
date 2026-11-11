package com.tizhen.app.api.datagen.dto;

import com.tizhen.core.datagen.model.DataGenCreateCommand;
import com.tizhen.core.datagen.model.DataRangeItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建造数据任务。字段约束与 docs/design/04.API接口设计.md §6.8.1 DG-02 一致。
 */
public record CreateDataGenTaskRequest(
        @NotBlank(message = "任务名称不能为空")
        @Size(max = 200, message = "任务名称不能超过 200 字")
        String title,

        @NotBlank(message = "题面不能为空")
        @Size(max = 100_000, message = "题面不能超过 100000 字")
        String statement,

        @NotBlank(message = "输入格式不能为空")
        @Size(max = 20_000, message = "输入格式不能超过 20000 字")
        String inputFormat,

        @NotBlank(message = "输出格式不能为空")
        @Size(max = 20_000, message = "输出格式不能超过 20000 字")
        String outputFormat,

        @NotBlank(message = "标准程序不能为空")
        @Size(max = 65_536, message = "标准程序不能超过 64 KB")
        String stdCode,

        @NotBlank(message = "标准程序语言不能为空")
        String stdLanguage,

        @NotNull(message = "数据范围不能为空")
        @Size(min = 1, max = 20, message = "数据范围必须包含 1–20 个变量")
        List<@Valid DataRangeItemRequest> dataRange,

        @Min(value = 1, message = "数据组数不能小于 1")
        @Max(value = 50, message = "数据组数不能超过 50")
        int groupCount,

        @NotBlank(message = "生成方式不能为空")
        String generationMode,

        @Size(max = 2_000, message = "特殊构造要求不能超过 2000 字")
        String specialRequirements,

        @Min(value = 100, message = "时间限制不能小于 100 毫秒")
        @Max(value = 60_000, message = "时间限制不能超过 60000 毫秒")
        int timeLimitMs,

        @Min(value = 16, message = "内存限制不能小于 16 MB")
        @Max(value = 2_048, message = "内存限制不能超过 2048 MB")
        int memoryLimitMb
) {

    public DataGenCreateCommand toCommand() {
        return new DataGenCreateCommand(
                title,
                statement,
                inputFormat,
                outputFormat,
                stdCode,
                stdLanguage,
                dataRange == null ? List.of() : dataRange.stream()
                        .map(item -> new DataRangeItem(item.name(), item.type(), item.min(), item.max()))
                        .toList(),
                groupCount,
                generationMode,
                specialRequirements,
                timeLimitMs,
                memoryLimitMb);
    }

    public record DataRangeItemRequest(
            @NotBlank(message = "变量名不能为空")
            String name,

            @NotBlank(message = "变量类型不能为空")
            String type,

            @NotNull(message = "变量最小值不能为空")
            BigDecimal min,

            @NotNull(message = "变量最大值不能为空")
            BigDecimal max
    ) {
    }
}
