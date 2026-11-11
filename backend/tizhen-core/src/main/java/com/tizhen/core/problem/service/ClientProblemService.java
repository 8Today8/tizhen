package com.tizhen.core.problem.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.judge.mapper.SubmissionMapper;
import com.tizhen.core.judge.model.ProblemAttemptStats;
import com.tizhen.core.problem.entity.ObjectiveOption;
import com.tizhen.core.problem.entity.ProblemSample;
import com.tizhen.core.problem.entity.ProblemTag;
import com.tizhen.core.problem.mapper.ObjectiveOptionMapper;
import com.tizhen.core.problem.mapper.ProblemMapper;
import com.tizhen.core.problem.mapper.ProblemSampleMapper;
import com.tizhen.core.problem.mapper.ProblemTagMapper;
import com.tizhen.core.problem.model.ClientProblemDetail;
import com.tizhen.core.problem.model.ClientProblemOption;
import com.tizhen.core.problem.model.ClientProblemProgress;
import com.tizhen.core.problem.model.ClientProblemRow;
import com.tizhen.core.problem.model.ClientProblemSample;
import com.tizhen.core.problem.model.ClientProblemSummary;
import com.tizhen.core.problem.model.ProblemTagCountRow;
import com.tizhen.core.problem.model.ProblemTagOption;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 选手端题库读取。只暴露已发布题目，并统一裁剪答案、解析、评分要点与测试数据。
 */
@Service
public class ClientProblemService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_FILTER_LENGTH = 64;

    private static final String TYPE_OJ = "oj";
    private static final String TYPE_OBJECTIVE = "objective";
    private static final String TYPE_SHORT_ANSWER = "short_answer";
    private static final String TYPE_EXTERNAL = "external";

    private static final Set<String> TYPES =
            Set.of(TYPE_OJ, TYPE_OBJECTIVE, TYPE_SHORT_ANSWER, TYPE_EXTERNAL);
    private static final Set<String> OBJECTIVE_TYPES = Set.of("judge", "single", "multiple");
    private static final Set<String> DIFFICULTIES = Set.of("entry", "basic", "advanced");

    private final ProblemMapper problemMapper;
    private final ProblemTagMapper problemTagMapper;
    private final ProblemSampleMapper problemSampleMapper;
    private final ObjectiveOptionMapper objectiveOptionMapper;
    private final SubmissionMapper submissionMapper;

    public ClientProblemService(ProblemMapper problemMapper,
                                ProblemTagMapper problemTagMapper,
                                ProblemSampleMapper problemSampleMapper,
                                ObjectiveOptionMapper objectiveOptionMapper,
                                SubmissionMapper submissionMapper) {
        this.problemMapper = problemMapper;
        this.problemTagMapper = problemTagMapper;
        this.problemSampleMapper = problemSampleMapper;
        this.objectiveOptionMapper = objectiveOptionMapper;
        this.submissionMapper = submissionMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<ClientProblemSummary> list(Long userId,
                                                 String type,
                                                 String objectiveType,
                                                 String difficulty,
                                                 String source,
                                                 String tag,
                                                 String keyword,
                                                 int page,
                                                 int pageSize) {
        String normalizedType = normalizeEnum(type, TYPES, "type");
        String normalizedObjectiveType = normalizeEnum(objectiveType, OBJECTIVE_TYPES, "objectiveType");
        String normalizedDifficulty = normalizeEnum(difficulty, DIFFICULTIES, "difficulty");
        String normalizedSource = normalizeFilterText(source, MAX_FILTER_LENGTH, "source");
        String normalizedTag = normalizeFilterText(tag, MAX_FILTER_LENGTH, "tag");
        String normalizedKeyword = normalizeFilterText(keyword, MAX_KEYWORD_LENGTH, "keyword");
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<ClientProblemRow> result = new Page<>(safePage, safePageSize);
        problemMapper.selectPublishedProblemPage(result, userId, normalizedType,
                normalizedObjectiveType, normalizedDifficulty, normalizedSource,
                normalizedTag, normalizedKeyword);

        List<Long> problemIds = result.getRecords().stream()
                .map(ClientProblemRow::getId)
                .toList();
        Map<Long, List<String>> tagsByProblem = loadTags(problemIds);
        List<ClientProblemSummary> items = result.getRecords().stream()
                .map(row -> toSummary(row, tagsByProblem.getOrDefault(row.getId(), List.of())))
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional(readOnly = true)
    public List<ProblemTagOption> listTags() {
        return problemMapper.selectPublishedTagCounts().stream()
                .map(this::toTagOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientProblemSummary random(Long userId, String type, String difficulty, String tag) {
        String normalizedType = normalizeEnum(type, TYPES, "type");
        String normalizedDifficulty = normalizeEnum(difficulty, DIFFICULTIES, "difficulty");
        String normalizedTag = normalizeFilterText(tag, MAX_FILTER_LENGTH, "tag");
        ClientProblemRow row = problemMapper.selectRandomPublishedProblem(
                userId, normalizedType, normalizedDifficulty, normalizedTag);
        if (row == null) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }
        List<String> tags = loadTags(List.of(row.getId())).getOrDefault(row.getId(), List.of());
        return toSummary(row, tags);
    }

    @Transactional(readOnly = true)
    public ClientProblemDetail detail(Long userId, Long problemId) {
        ClientProblemRow row = problemMapper.selectPublishedProblemRow(userId, problemId);
        if (row == null) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }
        List<String> tags = loadTags(List.of(problemId)).getOrDefault(problemId, List.of());
        List<ClientProblemSample> samples = TYPE_OJ.equals(row.getType())
                ? loadSamples(problemId)
                : List.of();
        List<ClientProblemOption> options = TYPE_OBJECTIVE.equals(row.getType())
                ? loadOptions(problemId)
                : List.of();

        ProblemAttemptStats stats = submissionMapper.selectAttemptStats(userId, problemId);
        long attemptCount = stats == null || stats.getAttemptCount() == null
                ? 0L
                : stats.getAttemptCount();
        boolean firstAc = isTrue(row.getSolvedByMe());
        ClientProblemProgress progress = new ClientProblemProgress(
                attemptCount,
                firstAc,
                TimeUtils.toInstant(row.getFirstAcAt()),
                stats == null ? null : stats.getLastVerdict(),
                TimeUtils.toInstant(row.getLastSubmittedAt()));

        boolean oj = TYPE_OJ.equals(row.getType());
        boolean objective = TYPE_OBJECTIVE.equals(row.getType());
        boolean shortAnswer = TYPE_SHORT_ANSWER.equals(row.getType());
        boolean external = TYPE_EXTERNAL.equals(row.getType());
        return new ClientProblemDetail(
                String.valueOf(row.getId()),
                row.getType(),
                objective ? row.getObjectiveType() : null,
                row.getTitle(),
                row.getStatement(),
                oj ? row.getInputFormat() : null,
                oj ? row.getOutputFormat() : null,
                row.getDifficulty(),
                row.getSource(),
                tags,
                oj ? row.getTimeLimitMs() : null,
                oj ? row.getMemoryLimitMb() : null,
                shortAnswer ? row.getMaxScore() : null,
                external ? row.getExternalUrl() : null,
                external ? row.getPlatform() : null,
                external ? isTrue(row.getExternalCompleted()) : null,
                samples,
                options,
                progress);
    }

    private ClientProblemSummary toSummary(ClientProblemRow row, List<String> tags) {
        boolean oj = TYPE_OJ.equals(row.getType());
        boolean objective = TYPE_OBJECTIVE.equals(row.getType());
        boolean external = TYPE_EXTERNAL.equals(row.getType());
        return new ClientProblemSummary(
                String.valueOf(row.getId()),
                row.getType(),
                objective ? row.getObjectiveType() : null,
                row.getTitle(),
                row.getDifficulty(),
                row.getSource(),
                external ? row.getPlatform() : null,
                external ? row.getExternalUrl() : null,
                tags,
                oj && isTrue(row.getSolvedByMe()),
                isTrue(row.getAttemptedByMe()),
                external ? isTrue(row.getExternalCompleted()) : null);
    }

    private ProblemTagOption toTagOption(ProblemTagCountRow row) {
        return new ProblemTagOption(row.getValue(), row.getTagCount() == null ? 0L : row.getTagCount());
    }

    private List<ClientProblemSample> loadSamples(Long problemId) {
        return problemSampleMapper.selectList(Wrappers.<ProblemSample>lambdaQuery()
                        .eq(ProblemSample::getProblemId, problemId)
                        .orderByAsc(ProblemSample::getSortOrder)
                        .orderByAsc(ProblemSample::getId))
                .stream()
                .map(sample -> new ClientProblemSample(
                        sample.getInputData(),
                        sample.getOutputData(),
                        sample.getExplanation(),
                        sample.getSortOrder() == null ? 0 : sample.getSortOrder()))
                .toList();
    }

    private List<ClientProblemOption> loadOptions(Long problemId) {
        return objectiveOptionMapper.selectList(Wrappers.<ObjectiveOption>lambdaQuery()
                        .eq(ObjectiveOption::getProblemId, problemId)
                        .orderByAsc(ObjectiveOption::getSortOrder)
                        .orderByAsc(ObjectiveOption::getOptionKey))
                .stream()
                .map(option -> new ClientProblemOption(
                        option.getOptionKey(),
                        option.getContent(),
                        option.getSortOrder() == null ? 0 : option.getSortOrder()))
                .toList();
    }

    private Map<Long, List<String>> loadTags(List<Long> problemIds) {
        if (problemIds.isEmpty()) {
            return Map.of();
        }
        return problemTagMapper.selectList(Wrappers.<ProblemTag>lambdaQuery()
                        .in(ProblemTag::getProblemId, problemIds)
                        .eq(ProblemTag::getTagType, "topic")
                        .orderByAsc(ProblemTag::getTagValue))
                .stream()
                .collect(Collectors.groupingBy(
                        ProblemTag::getProblemId,
                        LinkedHashMap::new,
                        Collectors.mapping(ProblemTag::getTagValue, Collectors.toList())));
    }

    private boolean isTrue(Integer value) {
        return value != null && value == 1;
    }

    private String normalizeEnum(String value, Set<String> allowed, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, fieldName + " 取值非法");
        }
        return normalized;
    }

    private String normalizeFilterText(String value, int maxLength, String fieldName) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED,
                    fieldName + " 不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }
}
