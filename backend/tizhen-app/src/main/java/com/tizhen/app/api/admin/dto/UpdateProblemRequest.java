package com.tizhen.app.api.admin.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.tizhen.core.problem.model.ProblemUpdateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 修改题目请求。setter 记录字段是否出现，以区分未提交与显式清空；
 * 题型不可修改，因此请求体中没有 type 字段。
 */
public class UpdateProblemRequest {

    private final Set<String> presentFields = new LinkedHashSet<>();

    @Size(max = 200, message = "题目名称不能超过 200 个字符")
    private String title;

    @Size(max = 100_000, message = "题面不能超过 100000 个字符")
    private String statement;

    private String difficulty;

    @Size(max = 64, message = "来源不能超过 64 个字符")
    private String source;

    @Size(max = 20, message = "标签最多 20 个")
    private List<@Size(max = 64, message = "单个标签不能超过 64 个字符") String> tags;

    @Size(max = 100_000, message = "输入格式不能超过 100000 个字符")
    private String inputFormat;

    @Size(max = 100_000, message = "输出格式不能超过 100000 个字符")
    private String outputFormat;

    @Min(value = 100, message = "时限不能少于 100 毫秒")
    @Max(value = 60_000, message = "时限不能超过 60000 毫秒")
    private Integer timeLimitMs;

    @Min(value = 16, message = "内存限制不能少于 16 MB")
    @Max(value = 2_048, message = "内存限制不能超过 2048 MB")
    private Integer memoryLimitMb;

    @Valid
    @Size(max = 20, message = "样例最多 20 组")
    private List<ProblemSampleRequest> samples;

    private String objectiveType;

    @Valid
    @Size(max = 10, message = "选项最多 10 个")
    private List<ObjectiveOptionRequest> options;

    @Size(max = 100_000, message = "解析不能超过 100000 个字符")
    private String explanation;

    @Size(max = 20_000, message = "参考答案不能超过 20000 个字符")
    private String referenceAnswer;

    @Valid
    @Size(max = 20, message = "评分要点最多 20 项")
    private List<ScoringRubricRequest> scoringRubric;

    private Integer maxScore;

    @Size(max = 512, message = "外链地址不能超过 512 个字符")
    private String externalUrl;

    @Size(max = 32, message = "平台不能超过 32 个字符")
    private String platform;

    public String getTitle() {
        return title;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = title;
        presentFields.add("title");
    }

    public String getStatement() {
        return statement;
    }

    @JsonSetter("statement")
    public void setStatement(String statement) {
        this.statement = statement;
        presentFields.add("statement");
    }

    public String getDifficulty() {
        return difficulty;
    }

    @JsonSetter("difficulty")
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
        presentFields.add("difficulty");
    }

    public String getSource() {
        return source;
    }

    @JsonSetter("source")
    public void setSource(String source) {
        this.source = source;
        presentFields.add("source");
    }

    public List<String> getTags() {
        return tags;
    }

    @JsonSetter("tags")
    public void setTags(List<String> tags) {
        this.tags = tags;
        presentFields.add("tags");
    }

    public String getInputFormat() {
        return inputFormat;
    }

    @JsonSetter("inputFormat")
    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
        presentFields.add("inputFormat");
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    @JsonSetter("outputFormat")
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
        presentFields.add("outputFormat");
    }

    public Integer getTimeLimitMs() {
        return timeLimitMs;
    }

    @JsonSetter("timeLimitMs")
    public void setTimeLimitMs(Integer timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
        presentFields.add("timeLimitMs");
    }

    public Integer getMemoryLimitMb() {
        return memoryLimitMb;
    }

    @JsonSetter("memoryLimitMb")
    public void setMemoryLimitMb(Integer memoryLimitMb) {
        this.memoryLimitMb = memoryLimitMb;
        presentFields.add("memoryLimitMb");
    }

    public List<ProblemSampleRequest> getSamples() {
        return samples;
    }

    @JsonSetter("samples")
    public void setSamples(List<ProblemSampleRequest> samples) {
        this.samples = samples;
        presentFields.add("samples");
    }

    public String getObjectiveType() {
        return objectiveType;
    }

    @JsonSetter("objectiveType")
    public void setObjectiveType(String objectiveType) {
        this.objectiveType = objectiveType;
        presentFields.add("objectiveType");
    }

    public List<ObjectiveOptionRequest> getOptions() {
        return options;
    }

    @JsonSetter("options")
    public void setOptions(List<ObjectiveOptionRequest> options) {
        this.options = options;
        presentFields.add("options");
    }

    public String getExplanation() {
        return explanation;
    }

    @JsonSetter("explanation")
    public void setExplanation(String explanation) {
        this.explanation = explanation;
        presentFields.add("explanation");
    }

    public String getReferenceAnswer() {
        return referenceAnswer;
    }

    @JsonSetter("referenceAnswer")
    public void setReferenceAnswer(String referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
        presentFields.add("referenceAnswer");
    }

    public List<ScoringRubricRequest> getScoringRubric() {
        return scoringRubric;
    }

    @JsonSetter("scoringRubric")
    public void setScoringRubric(List<ScoringRubricRequest> scoringRubric) {
        this.scoringRubric = scoringRubric;
        presentFields.add("scoringRubric");
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    @JsonSetter("maxScore")
    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
        presentFields.add("maxScore");
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    @JsonSetter("externalUrl")
    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
        presentFields.add("externalUrl");
    }

    public String getPlatform() {
        return platform;
    }

    @JsonSetter("platform")
    public void setPlatform(String platform) {
        this.platform = platform;
        presentFields.add("platform");
    }

    public ProblemUpdateCommand toCommand() {
        return new ProblemUpdateCommand(
                title,
                statement,
                difficulty,
                source,
                tags,
                inputFormat,
                outputFormat,
                timeLimitMs,
                memoryLimitMb,
                CreateProblemRequest.toSampleInputs(samples),
                objectiveType,
                CreateProblemRequest.toOptionInputs(options),
                explanation,
                referenceAnswer,
                CreateProblemRequest.toRubricItems(scoringRubric),
                maxScore,
                externalUrl,
                platform,
                presentFields);
    }
}
