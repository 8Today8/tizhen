package com.tizhen.core.problem.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.model.LoginMetadata;
import com.tizhen.core.admin.service.OperationLogService;
import com.tizhen.core.problem.entity.ObjectiveOption;
import com.tizhen.core.problem.entity.Problem;
import com.tizhen.core.problem.entity.ProblemSample;
import com.tizhen.core.problem.entity.ProblemTag;
import com.tizhen.core.problem.entity.TestData;
import com.tizhen.core.problem.mapper.ObjectiveOptionMapper;
import com.tizhen.core.problem.mapper.ProblemMapper;
import com.tizhen.core.problem.mapper.ProblemSampleMapper;
import com.tizhen.core.problem.mapper.ProblemTagMapper;
import com.tizhen.core.problem.mapper.TestDataMapper;
import com.tizhen.core.problem.model.AdminProblemDetail;
import com.tizhen.core.problem.model.AdminProblemUsage;
import com.tizhen.core.problem.model.ObjectiveOptionInput;
import com.tizhen.core.problem.model.ObjectiveOptionView;
import com.tizhen.core.problem.model.ProblemCreateCommand;
import com.tizhen.core.problem.model.ProblemSampleInput;
import com.tizhen.core.problem.model.ProblemSampleView;
import com.tizhen.core.problem.model.ProblemUpdateCommand;
import com.tizhen.core.problem.model.ScoringRubricItem;
import com.tizhen.core.problem.model.TestDataInput;
import com.tizhen.core.problem.model.TestDataView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 管理端题目维护。四类题目共用 problem 主表，题型配置字段与子表在服务层按 type 严格校验。
 */
@Service
public class AdminProblemService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_STATEMENT_LENGTH = 100_000;
    private static final int MAX_SOURCE_LENGTH = 64;
    private static final int MAX_TAG_COUNT = 20;
    private static final int MAX_TAG_LENGTH = 64;
    private static final int MAX_LONG_TEXT_LENGTH = 100_000;
    private static final int MAX_SAMPLE_COUNT = 20;
    private static final int MIN_OPTION_COUNT = 2;
    private static final int MAX_OPTION_COUNT = 10;
    private static final int MAX_RUBRIC_COUNT = 20;
    private static final int MAX_RUBRIC_POINT_LENGTH = 200;
    private static final int MAX_RUBRIC_DESCRIPTION_LENGTH = 500;
    private static final int MAX_REFERENCE_ANSWER_LENGTH = 20_000;
    private static final int MAX_EXTERNAL_URL_LENGTH = 512;
    private static final int MAX_PLATFORM_LENGTH = 32;
    private static final int MIN_TIME_LIMIT_MS = 100;
    private static final int MAX_TIME_LIMIT_MS = 60_000;
    private static final int MIN_MEMORY_LIMIT_MB = 16;
    private static final int MAX_MEMORY_LIMIT_MB = 2_048;
    private static final int MAX_SORT_ORDER = 65_535;

    private static final int MAX_TEST_DATA_COUNT = 500;
    private static final int MAX_TEST_DATA_ITEM_BYTES = 1_048_576;
    private static final long MAX_TEST_DATA_TOTAL_BYTES = 10_485_760L;

    private static final String TYPE_OJ = "oj";
    private static final String TYPE_OBJECTIVE = "objective";
    private static final String TYPE_SHORT_ANSWER = "short_answer";
    private static final String TYPE_EXTERNAL = "external";
    private static final Set<String> PROBLEM_TYPES = Set.of(
            TYPE_OJ, TYPE_OBJECTIVE, TYPE_SHORT_ANSWER, TYPE_EXTERNAL);

    private static final String OBJECTIVE_JUDGE = "judge";
    private static final String OBJECTIVE_SINGLE = "single";
    private static final String OBJECTIVE_MULTIPLE = "multiple";
    private static final Set<String> OBJECTIVE_TYPES = Set.of(
            OBJECTIVE_JUDGE, OBJECTIVE_SINGLE, OBJECTIVE_MULTIPLE);

    private static final Set<String> DIFFICULTIES = Set.of("entry", "basic", "advanced");

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_OFFLINE = "offline";

    private static final String PERMISSION = "problem:manage";

    private static final TypeReference<List<ScoringRubricItem>> RUBRIC_TYPE =
            new TypeReference<>() {
            };

    private final ProblemMapper problemMapper;
    private final ProblemTagMapper problemTagMapper;
    private final ProblemSampleMapper problemSampleMapper;
    private final TestDataMapper testDataMapper;
    private final ObjectiveOptionMapper objectiveOptionMapper;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public AdminProblemService(ProblemMapper problemMapper,
                               ProblemTagMapper problemTagMapper,
                               ProblemSampleMapper problemSampleMapper,
                               TestDataMapper testDataMapper,
                               ObjectiveOptionMapper objectiveOptionMapper,
                               OperationLogService operationLogService,
                               ObjectMapper objectMapper) {
        this.problemMapper = problemMapper;
        this.problemTagMapper = problemTagMapper;
        this.problemSampleMapper = problemSampleMapper;
        this.testDataMapper = testDataMapper;
        this.objectiveOptionMapper = objectiveOptionMapper;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AdminProblemDetail create(AdminPrincipal actor,
                                     ProblemCreateCommand command,
                                     LoginMetadata metadata) {
        requirePermission(actor);
        if (command == null) {
            throw validation("请求体不能为空");
        }

        String type = normalizeType(command.type());
        String title = requireText(command.title(), MAX_TITLE_LENGTH, "题目名称");
        String difficulty = normalizeDifficulty(command.difficulty());
        String source = requireText(command.source(), MAX_SOURCE_LENGTH, "来源");
        String statement = normalizeStatement(type, command.statement());
        List<String> tags = normalizeTags(command.tags());
        rejectMismatchedCreateFields(type, command);

        String inputFormat = null;
        String outputFormat = null;
        Integer timeLimitMs = null;
        Integer memoryLimitMb = null;
        List<ProblemSampleInput> samples = List.of();
        String objectiveType = null;
        List<ObjectiveOptionInput> options = List.of();
        String explanation = null;
        String referenceAnswer = null;
        List<ScoringRubricItem> scoringRubric = List.of();
        String externalUrl = null;
        String platform = null;

        switch (type) {
            case TYPE_OJ -> {
                inputFormat = normalizeOptionalText(command.inputFormat(),
                        MAX_LONG_TEXT_LENGTH, "输入格式");
                outputFormat = normalizeOptionalText(command.outputFormat(),
                        MAX_LONG_TEXT_LENGTH, "输出格式");
                timeLimitMs = normalizeTimeLimit(command.timeLimitMs());
                memoryLimitMb = normalizeMemoryLimit(command.memoryLimitMb());
                samples = normalizeSamples(command.samples());
            }
            case TYPE_OBJECTIVE -> {
                objectiveType = normalizeObjectiveTypeRequired(command.objectiveType());
                options = normalizeOptions(objectiveType, command.options());
                explanation = normalizeOptionalText(command.explanation(),
                        MAX_LONG_TEXT_LENGTH, "解析");
            }
            case TYPE_SHORT_ANSWER -> {
                referenceAnswer = requireText(command.referenceAnswer(),
                        MAX_REFERENCE_ANSWER_LENGTH, "参考答案");
                scoringRubric = normalizeRubric(command.scoringRubric());
                normalizeMaxScore(command.maxScore());
            }
            case TYPE_EXTERNAL -> {
                externalUrl = normalizeExternalUrl(command.externalUrl());
                platform = requireText(command.platform(), MAX_PLATFORM_LENGTH, "外链平台");
            }
            default -> throw validation("题型取值非法");
        }

        Problem problem = new Problem();
        problem.setType(type);
        problem.setObjectiveType(TYPE_OBJECTIVE.equals(type) ? objectiveType : null);
        problem.setTitle(title);
        problem.setStatement(statement);
        problem.setInputFormat(TYPE_OJ.equals(type) ? inputFormat : null);
        problem.setOutputFormat(TYPE_OJ.equals(type) ? outputFormat : null);
        problem.setExplanation(TYPE_OBJECTIVE.equals(type) ? explanation : null);
        problem.setReferenceAnswer(TYPE_SHORT_ANSWER.equals(type) ? referenceAnswer : null);
        problem.setScoringRubric(TYPE_SHORT_ANSWER.equals(type) ? writeRubric(scoringRubric) : null);
        problem.setMaxScore(100);
        problem.setDifficulty(difficulty);
        problem.setSource(source);
        problem.setExternalUrl(TYPE_EXTERNAL.equals(type) ? externalUrl : null);
        problem.setPlatform(TYPE_EXTERNAL.equals(type) ? platform : null);
        problem.setTimeLimitMs(TYPE_OJ.equals(type) ? timeLimitMs : null);
        problem.setMemoryLimitMb(TYPE_OJ.equals(type) ? memoryLimitMb : null);
        problem.setStatus(STATUS_DRAFT);
        problemMapper.insert(problem);

        replaceTags(problem.getId(), tags);
        if (TYPE_OJ.equals(type)) {
            insertSamples(problem.getId(), samples);
        }
        if (TYPE_OBJECTIVE.equals(type)) {
            insertOptions(problem.getId(), options);
        }

        operationLogService.record(actor.id(), "create", "problem", problem.getId(),
                "新增题目《" + title + "》",
                Map.of("type", type, "status", STATUS_DRAFT), metadata);
        return buildDetail(problem.getId());
    }

    @Transactional(readOnly = true)
    public AdminProblemDetail detail(AdminPrincipal actor, Long problemId) {
        requirePermission(actor);
        requireProblem(problemId);
        return buildDetail(problemId);
    }

    @Transactional
    public AdminProblemDetail update(AdminPrincipal actor,
                                     Long problemId,
                                     ProblemUpdateCommand command,
                                     LoginMetadata metadata) {
        requirePermission(actor);
        Problem existing = requireProblem(problemId);
        if (command == null || command.isEmpty()) {
            throw validation("至少提交一个可修改字段");
        }

        String type = existing.getType();
        rejectMismatchedUpdateFields(type, command);

        String title = command.has("title")
                ? requireText(command.title(), MAX_TITLE_LENGTH, "题目名称")
                : existing.getTitle();
        String difficulty = command.has("difficulty")
                ? normalizeDifficulty(command.difficulty())
                : existing.getDifficulty();
        String source = command.has("source")
                ? requireText(command.source(), MAX_SOURCE_LENGTH, "来源")
                : existing.getSource();
        String statement = command.has("statement")
                ? normalizeStatement(type, command.statement())
                : existing.getStatement();
        List<String> tagsToReplace = command.has("tags")
                ? normalizeTags(command.tags())
                : null;

        String inputFormat = null;
        String outputFormat = null;
        Integer timeLimitMs = null;
        Integer memoryLimitMb = null;
        List<ProblemSampleInput> samplesToReplace = null;
        String objectiveType = null;
        List<ObjectiveOptionInput> optionsToReplace = null;
        String explanation = null;
        String referenceAnswer = null;
        String rubricJson = null;
        List<ScoringRubricItem> rubricEffective = List.of();
        String externalUrl = null;
        String platform = null;

        long sampleCount = 0;
        long testDataCount = 0;
        List<ObjectiveOptionInput> optionsEffective = List.of();

        switch (type) {
            case TYPE_OJ -> {
                inputFormat = command.has("inputFormat")
                        ? normalizeOptionalText(command.inputFormat(),
                        MAX_LONG_TEXT_LENGTH, "输入格式")
                        : existing.getInputFormat();
                outputFormat = command.has("outputFormat")
                        ? normalizeOptionalText(command.outputFormat(),
                        MAX_LONG_TEXT_LENGTH, "输出格式")
                        : existing.getOutputFormat();
                timeLimitMs = command.has("timeLimitMs")
                        ? normalizeTimeLimit(command.timeLimitMs())
                        : existing.getTimeLimitMs();
                memoryLimitMb = command.has("memoryLimitMb")
                        ? normalizeMemoryLimit(command.memoryLimitMb())
                        : existing.getMemoryLimitMb();
                if (command.has("samples")) {
                    samplesToReplace = normalizeSamples(command.samples());
                }
                if (STATUS_PUBLISHED.equals(existing.getStatus())) {
                    sampleCount = samplesToReplace != null
                            ? samplesToReplace.size()
                            : countSamples(problemId);
                    testDataCount = countTestData(problemId);
                }
            }
            case TYPE_OBJECTIVE -> {
                objectiveType = command.has("objectiveType")
                        ? normalizeObjectiveTypeRequired(command.objectiveType())
                        : existing.getObjectiveType();
                if (objectiveType == null) {
                    throw validation("客观题类型不能为空");
                }
                if (command.has("options")) {
                    optionsToReplace = normalizeOptions(objectiveType, command.options());
                }
                explanation = command.has("explanation")
                        ? normalizeOptionalText(command.explanation(),
                        MAX_LONG_TEXT_LENGTH, "解析")
                        : existing.getExplanation();
                if (command.has("objectiveType") && !command.has("options")) {
                    // 题型切换后即使仍是草稿，也要保证现有选项与新的客观题类型匹配。
                    normalizeOptions(objectiveType, loadOptionsAsInput(problemId));
                }
                if (STATUS_PUBLISHED.equals(existing.getStatus())) {
                    optionsEffective = optionsToReplace != null
                            ? optionsToReplace
                            : loadOptionsAsInput(problemId);
                }
            }
            case TYPE_SHORT_ANSWER -> {
                referenceAnswer = command.has("referenceAnswer")
                        ? requireText(command.referenceAnswer(),
                        MAX_REFERENCE_ANSWER_LENGTH, "参考答案")
                        : existing.getReferenceAnswer();
                rubricEffective = command.has("scoringRubric")
                        ? normalizeRubric(command.scoringRubric())
                        : parseRubric(existing.getScoringRubric());
                rubricJson = writeRubric(rubricEffective);
                if (command.has("maxScore")) {
                    normalizeMaxScore(command.maxScore());
                }
            }
            case TYPE_EXTERNAL -> {
                externalUrl = command.has("externalUrl")
                        ? normalizeExternalUrl(command.externalUrl())
                        : existing.getExternalUrl();
                platform = command.has("platform")
                        ? requireText(command.platform(), MAX_PLATFORM_LENGTH, "外链平台")
                        : existing.getPlatform();
                if (!hasText(externalUrl) || !hasText(platform)) {
                    throw validation("外链题必须配置外链地址与平台");
                }
            }
            default -> throw validation("题型取值非法");
        }

        String effectiveObjectiveType = TYPE_OBJECTIVE.equals(type) ? objectiveType : null;
        String effectiveReferenceAnswer = TYPE_SHORT_ANSWER.equals(type) ? referenceAnswer : null;
        List<ScoringRubricItem> effectiveRubric = TYPE_SHORT_ANSWER.equals(type)
                ? rubricEffective
                : List.of();
        String effectiveExternalUrl = TYPE_EXTERNAL.equals(type) ? externalUrl : null;
        String effectivePlatform = TYPE_EXTERNAL.equals(type) ? platform : null;

        if (STATUS_PUBLISHED.equals(existing.getStatus())) {
            assertPublishable(type, timeLimitMs, memoryLimitMb, sampleCount, testDataCount,
                    effectiveObjectiveType, optionsEffective, effectiveReferenceAnswer,
                    effectiveRubric, effectiveExternalUrl, effectivePlatform);
        }

        var update = Wrappers.<Problem>lambdaUpdate()
                .eq(Problem::getId, problemId)
                .isNull(Problem::getDeletedAt)
                .set(Problem::getTitle, title)
                .set(Problem::getStatement, statement)
                .set(Problem::getDifficulty, difficulty)
                .set(Problem::getSource, source)
                .set(Problem::getObjectiveType, effectiveObjectiveType)
                .set(Problem::getInputFormat, TYPE_OJ.equals(type) ? inputFormat : null)
                .set(Problem::getOutputFormat, TYPE_OJ.equals(type) ? outputFormat : null)
                .set(Problem::getExplanation, TYPE_OBJECTIVE.equals(type) ? explanation : null)
                .set(Problem::getReferenceAnswer, effectiveReferenceAnswer)
                .set(Problem::getScoringRubric, TYPE_SHORT_ANSWER.equals(type) ? rubricJson : null)
                .set(Problem::getMaxScore, 100)
                .set(Problem::getExternalUrl, effectiveExternalUrl)
                .set(Problem::getPlatform, effectivePlatform)
                .set(Problem::getTimeLimitMs, TYPE_OJ.equals(type) ? timeLimitMs : null)
                .set(Problem::getMemoryLimitMb, TYPE_OJ.equals(type) ? memoryLimitMb : null)
                .set(Problem::getUpdatedAt, now());
        int updated = problemMapper.update(null, update);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        if (tagsToReplace != null) {
            replaceTags(problemId, tagsToReplace);
        }
        if (samplesToReplace != null) {
            replaceSamples(problemId, samplesToReplace);
        }
        if (optionsToReplace != null) {
            replaceOptions(problemId, optionsToReplace);
        }

        operationLogService.record(actor.id(), "update", "problem", problemId,
                "修改题目《" + title + "》",
                Map.of("fields", List.copyOf(command.presentFields())), metadata);
        return buildDetail(problemId);
    }

    @Transactional
    public void delete(AdminPrincipal actor, Long problemId, LoginMetadata metadata) {
        requirePermission(actor);
        Problem problem = requireProblem(problemId);
        AdminProblemUsage usage = loadUsage(problemId);
        if (usage.hasReferences()) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_DELETABLE);
        }
        int updated = problemMapper.update(null, Wrappers.<Problem>lambdaUpdate()
                .eq(Problem::getId, problemId)
                .isNull(Problem::getDeletedAt)
                .set(Problem::getDeletedAt, now())
                .set(Problem::getStatus, STATUS_OFFLINE)
                .set(Problem::getUpdatedAt, now()));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }
        operationLogService.record(actor.id(), "delete", "problem", problemId,
                "删除题目《" + problem.getTitle() + "》", Map.of(), metadata);
    }

    @Transactional
    public AdminProblemDetail publish(AdminPrincipal actor,
                                      Long problemId,
                                      LoginMetadata metadata) {
        requirePermission(actor);
        Problem problem = requireProblem(problemId);
        String type = problem.getType();
        List<ObjectiveOptionInput> options = TYPE_OBJECTIVE.equals(type)
                ? loadOptionsAsInput(problemId)
                : List.of();
        List<ScoringRubricItem> rubric = TYPE_SHORT_ANSWER.equals(type)
                ? parseRubric(problem.getScoringRubric())
                : List.of();
        assertPublishable(type,
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb(),
                countSamples(problemId),
                countTestData(problemId),
                problem.getObjectiveType(),
                options,
                problem.getReferenceAnswer(),
                rubric,
                problem.getExternalUrl(),
                problem.getPlatform());

        if (!STATUS_PUBLISHED.equals(problem.getStatus())) {
            problemMapper.update(null, Wrappers.<Problem>lambdaUpdate()
                    .eq(Problem::getId, problemId)
                    .isNull(Problem::getDeletedAt)
                    .set(Problem::getStatus, STATUS_PUBLISHED)
                    .set(Problem::getUpdatedAt, now()));
            operationLogService.record(actor.id(), "publish", "problem", problemId,
                    "发布题目《" + problem.getTitle() + "》",
                    Map.of("status", STATUS_PUBLISHED), metadata);
        }
        return buildDetail(problemId);
    }

    @Transactional
    public AdminProblemDetail offline(AdminPrincipal actor,
                                      Long problemId,
                                      LoginMetadata metadata) {
        requirePermission(actor);
        Problem problem = requireProblem(problemId);
        if (!STATUS_OFFLINE.equals(problem.getStatus())) {
            problemMapper.update(null, Wrappers.<Problem>lambdaUpdate()
                    .eq(Problem::getId, problemId)
                    .isNull(Problem::getDeletedAt)
                    .set(Problem::getStatus, STATUS_OFFLINE)
                    .set(Problem::getUpdatedAt, now()));
            operationLogService.record(actor.id(), "offline", "problem", problemId,
                    "下架题目《" + problem.getTitle() + "》",
                    Map.of("status", STATUS_OFFLINE), metadata);
        }
        return buildDetail(problemId);
    }

    @Transactional(readOnly = true)
    public PageResult<TestDataView> listTestData(AdminPrincipal actor,
                                                 Long problemId,
                                                 int page,
                                                 int pageSize) {
        requirePermission(actor);
        Problem problem = requireProblem(problemId);
        requireOj(problem);

        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        Page<TestData> result = new Page<>(safePage, safePageSize);
        testDataMapper.selectPage(result, Wrappers.<TestData>lambdaQuery()
                .eq(TestData::getProblemId, problemId)
                .orderByAsc(TestData::getSortOrder)
                .orderByAsc(TestData::getId));
        List<TestDataView> items = result.getRecords().stream()
                .map(this::toTestDataView)
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional
    public List<TestDataView> replaceTestData(AdminPrincipal actor,
                                              Long problemId,
                                              List<TestDataInput> items,
                                              LoginMetadata metadata) {
        requirePermission(actor);
        Problem problem = requireProblem(problemId);
        requireOj(problem);
        List<TestDataInput> normalized = normalizeTestData(items);

        if (STATUS_PUBLISHED.equals(problem.getStatus())) {
            assertPublishable(TYPE_OJ,
                    problem.getTimeLimitMs(),
                    problem.getMemoryLimitMb(),
                    countSamples(problemId),
                    normalized.size(),
                    null,
                    List.of(),
                    null,
                    List.of(),
                    null,
                    null);
        }

        testDataMapper.delete(Wrappers.<TestData>lambdaQuery()
                .eq(TestData::getProblemId, problemId));

        List<TestDataView> views = new ArrayList<>(normalized.size());
        for (int index = 0; index < normalized.size(); index++) {
            TestDataInput input = normalized.get(index);
            TestData entity = new TestData();
            entity.setProblemId(problemId);
            entity.setInputData(input.inputData());
            entity.setOutputData(input.outputData());
            entity.setSortOrder(index);
            testDataMapper.insert(entity);
            views.add(toTestDataView(entity));
        }

        operationLogService.record(actor.id(), "update", "problem", problemId,
                "更新题目《" + problem.getTitle() + "》的测试数据",
                Map.of("testDataCount", views.size()), metadata);
        return views;
    }

    private AdminProblemDetail buildDetail(Long problemId) {
        Problem problem = requireProblem(problemId);
        String type = problem.getType();
        List<String> tags = loadTags(problemId);
        List<ProblemSampleView> samples = TYPE_OJ.equals(type)
                ? loadSampleViews(problemId)
                : List.of();
        List<ObjectiveOptionView> options = TYPE_OBJECTIVE.equals(type)
                ? loadOptionViews(problemId)
                : List.of();
        List<ScoringRubricItem> rubric = TYPE_SHORT_ANSWER.equals(type)
                ? parseRubric(problem.getScoringRubric())
                : List.of();
        long testDataCount = TYPE_OJ.equals(type) ? countTestData(problemId) : 0;
        AdminProblemUsage usage = loadUsage(problemId);

        return new AdminProblemDetail(
                String.valueOf(problem.getId()),
                type,
                problem.getObjectiveType(),
                problem.getTitle(),
                problem.getStatement(),
                problem.getDifficulty(),
                problem.getSource(),
                tags,
                problem.getInputFormat(),
                problem.getOutputFormat(),
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb(),
                samples,
                options,
                problem.getExplanation(),
                problem.getReferenceAnswer(),
                rubric,
                problem.getMaxScore() == null ? 100 : problem.getMaxScore(),
                problem.getExternalUrl(),
                problem.getPlatform(),
                testDataCount,
                problem.getStatus(),
                TimeUtils.toInstant(problem.getCreatedAt()),
                TimeUtils.toInstant(problem.getUpdatedAt()),
                usage,
                !usage.hasReferences());
    }

    private void rejectMismatchedCreateFields(String type, ProblemCreateCommand command) {
        if (!TYPE_OJ.equals(type)) {
            if (hasText(command.inputFormat()) || hasText(command.outputFormat())
                    || command.timeLimitMs() != null || command.memoryLimitMb() != null
                    || (command.samples() != null && !command.samples().isEmpty())) {
                throw mismatch("输入输出格式、运行限制与样例仅适用于 OJ 题");
            }
        }
        if (!TYPE_OBJECTIVE.equals(type)) {
            if (hasText(command.objectiveType())
                    || (command.options() != null && !command.options().isEmpty())
                    || hasText(command.explanation())) {
                throw mismatch("客观题类型、选项与解析仅适用于客观题");
            }
        }
        if (!TYPE_SHORT_ANSWER.equals(type)) {
            if (hasText(command.referenceAnswer())
                    || (command.scoringRubric() != null && !command.scoringRubric().isEmpty())
                    || command.maxScore() != null) {
                throw mismatch("参考答案、评分要点与满分仅适用于简答题");
            }
        }
        if (!TYPE_EXTERNAL.equals(type)) {
            if (hasText(command.externalUrl()) || hasText(command.platform())) {
                throw mismatch("外链地址与平台仅适用于外链题");
            }
        }
    }

    private void rejectMismatchedUpdateFields(String type, ProblemUpdateCommand command) {
        if (!TYPE_OJ.equals(type)) {
            if ((command.has("inputFormat") && hasText(command.inputFormat()))
                    || (command.has("outputFormat") && hasText(command.outputFormat()))
                    || (command.has("timeLimitMs") && command.timeLimitMs() != null)
                    || (command.has("memoryLimitMb") && command.memoryLimitMb() != null)
                    || (command.has("samples") && command.samples() != null
                    && !command.samples().isEmpty())) {
                throw mismatch("输入输出格式、运行限制与样例仅适用于 OJ 题");
            }
        }
        if (!TYPE_OBJECTIVE.equals(type)) {
            if ((command.has("objectiveType") && hasText(command.objectiveType()))
                    || (command.has("options") && command.options() != null
                    && !command.options().isEmpty())
                    || (command.has("explanation") && hasText(command.explanation()))) {
                throw mismatch("客观题类型、选项与解析仅适用于客观题");
            }
        }
        if (!TYPE_SHORT_ANSWER.equals(type)) {
            if ((command.has("referenceAnswer") && hasText(command.referenceAnswer()))
                    || (command.has("scoringRubric") && command.scoringRubric() != null
                    && !command.scoringRubric().isEmpty())
                    || (command.has("maxScore") && command.maxScore() != null)) {
                throw mismatch("参考答案、评分要点与满分仅适用于简答题");
            }
        }
        if (!TYPE_EXTERNAL.equals(type)) {
            if ((command.has("externalUrl") && hasText(command.externalUrl()))
                    || (command.has("platform") && hasText(command.platform()))) {
                throw mismatch("外链地址与平台仅适用于外链题");
            }
        }
    }

    private void assertPublishable(String type,
                                   Integer timeLimitMs,
                                   Integer memoryLimitMb,
                                   long sampleCount,
                                   long testDataCount,
                                   String objectiveType,
                                   List<ObjectiveOptionInput> options,
                                   String referenceAnswer,
                                   List<ScoringRubricItem> rubric,
                                   String externalUrl,
                                   String platform) {
        switch (type) {
            case TYPE_OJ -> {
                if (timeLimitMs == null || timeLimitMs < MIN_TIME_LIMIT_MS
                        || timeLimitMs > MAX_TIME_LIMIT_MS) {
                    throw configIncomplete("OJ 题发布前需配置 100–60000 毫秒的时限");
                }
                if (memoryLimitMb == null || memoryLimitMb < MIN_MEMORY_LIMIT_MB
                        || memoryLimitMb > MAX_MEMORY_LIMIT_MB) {
                    throw configIncomplete("OJ 题发布前需配置 16–2048 MB 的内存限制");
                }
                if (sampleCount < 1) {
                    throw configIncomplete("OJ 题发布前至少配置 1 组样例");
                }
                if (testDataCount < 1) {
                    throw new BusinessException(ErrorCode.PROBLEM_TEST_DATA_REQUIRED);
                }
            }
            case TYPE_OBJECTIVE -> {
                if (!isOptionSetValid(objectiveType, options)) {
                    throw configIncomplete("客观题选项或正确答案数量不满足发布条件");
                }
            }
            case TYPE_SHORT_ANSWER -> {
                if (!hasText(referenceAnswer) || !isRubricValid(rubric)) {
                    throw configIncomplete("简答题需配置参考答案，且评分要点分数之和为 100");
                }
            }
            case TYPE_EXTERNAL -> {
                if (!isValidHttpUrl(externalUrl) || !hasText(platform)) {
                    throw configIncomplete("外链题需配置合法的外链地址与平台");
                }
            }
            default -> throw validation("题型取值非法");
        }
    }

    private boolean isOptionSetValid(String objectiveType, List<ObjectiveOptionInput> options) {
        if (objectiveType == null || !OBJECTIVE_TYPES.contains(objectiveType)
                || options == null || options.size() < MIN_OPTION_COUNT
                || options.size() > MAX_OPTION_COUNT) {
            return false;
        }
        Set<String> keys = new HashSet<>();
        int correctCount = 0;
        for (ObjectiveOptionInput option : options) {
            if (option == null || !hasText(option.optionKey()) || !hasText(option.content())) {
                return false;
            }
            String key = option.optionKey().trim().toUpperCase(Locale.ROOT);
            if (!key.matches("[A-Z]") || !keys.add(key)) {
                return false;
            }
            if (Boolean.TRUE.equals(option.isCorrect())) {
                correctCount++;
            }
        }
        if (OBJECTIVE_MULTIPLE.equals(objectiveType)) {
            return correctCount >= 1;
        }
        return correctCount == 1;
    }

    private boolean isRubricValid(List<ScoringRubricItem> rubric) {
        if (rubric == null || rubric.isEmpty() || rubric.size() > MAX_RUBRIC_COUNT) {
            return false;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (ScoringRubricItem item : rubric) {
            if (item == null || !hasText(item.point())
                    || item.point().trim().length() > MAX_RUBRIC_POINT_LENGTH
                    || item.score() == null || item.score().compareTo(BigDecimal.ZERO) < 0) {
                return false;
            }
            sum = sum.add(item.score());
        }
        return sum.compareTo(BigDecimal.valueOf(100)) == 0;
    }

    private List<ProblemSampleInput> normalizeSamples(List<ProblemSampleInput> values) {
        if (values == null) {
            return List.of();
        }
        if (values.size() > MAX_SAMPLE_COUNT) {
            throw validation("样例最多 " + MAX_SAMPLE_COUNT + " 组");
        }
        Set<Integer> sortOrders = new HashSet<>();
        List<ProblemSampleInput> normalized = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            ProblemSampleInput value = values.get(index);
            if (value == null) {
                throw validation("样例内容不能为空");
            }
            int sortOrder = value.sortOrder() == null ? index : value.sortOrder();
            validateSortOrder(sortOrder, "样例顺序");
            if (!sortOrders.add(sortOrder)) {
                throw validation("样例顺序不能重复");
            }
            normalized.add(new ProblemSampleInput(
                    nullToEmpty(value.inputData()),
                    nullToEmpty(value.outputData()),
                    normalizeOptionalText(value.explanation(),
                            MAX_LONG_TEXT_LENGTH, "样例说明"),
                    sortOrder));
        }
        return normalized;
    }

    private List<ObjectiveOptionInput> normalizeOptions(String objectiveType,
                                                        List<ObjectiveOptionInput> values) {
        if (objectiveType == null || !OBJECTIVE_TYPES.contains(objectiveType)) {
            throw optionsInvalid("客观题类型不能为空或取值非法");
        }
        if (values == null || values.size() < MIN_OPTION_COUNT
                || values.size() > MAX_OPTION_COUNT) {
            throw optionsInvalid("客观题需配置 " + MIN_OPTION_COUNT + "–"
                    + MAX_OPTION_COUNT + " 个选项");
        }
        Set<String> keys = new HashSet<>();
        int correctCount = 0;
        List<ObjectiveOptionInput> normalized = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            ObjectiveOptionInput value = values.get(index);
            if (value == null) {
                throw optionsInvalid("选项内容不能为空");
            }
            String key = value.optionKey() == null
                    ? ""
                    : value.optionKey().trim().toUpperCase(Locale.ROOT);
            if (!key.matches("[A-Z]")) {
                throw optionsInvalid("选项键必须是 A–Z 单个字母");
            }
            if (!keys.add(key)) {
                throw optionsInvalid("选项键不能重复");
            }
            String content = value.content() == null ? "" : value.content().trim();
            if (content.isEmpty()) {
                throw optionsInvalid("选项内容不能为空");
            }
            boolean isCorrect = Boolean.TRUE.equals(value.isCorrect());
            if (isCorrect) {
                correctCount++;
            }
            int sortOrder = value.sortOrder() == null ? index : value.sortOrder();
            validateSortOrder(sortOrder, "选项顺序");
            normalized.add(new ObjectiveOptionInput(key, content, isCorrect, sortOrder));
        }
        if (OBJECTIVE_MULTIPLE.equals(objectiveType)) {
            if (correctCount < 1) {
                throw optionsInvalid("多选题至少需要 1 个正确选项");
            }
        } else if (correctCount != 1) {
            throw optionsInvalid("判断/单选题必须且只能有 1 个正确选项");
        }
        return normalized;
    }

    private List<ScoringRubricItem> normalizeRubric(List<ScoringRubricItem> values) {
        if (values == null || values.isEmpty()) {
            throw validation("简答题至少配置 1 项评分要点");
        }
        if (values.size() > MAX_RUBRIC_COUNT) {
            throw validation("评分要点最多 " + MAX_RUBRIC_COUNT + " 项");
        }
        BigDecimal sum = BigDecimal.ZERO;
        List<ScoringRubricItem> normalized = new ArrayList<>(values.size());
        for (ScoringRubricItem value : values) {
            if (value == null) {
                throw validation("评分要点不能为空");
            }
            String point = requireText(value.point(), MAX_RUBRIC_POINT_LENGTH, "评分要点");
            if (value.score() == null || value.score().compareTo(BigDecimal.ZERO) < 0) {
                throw validation("评分要点分数必须大于等于 0");
            }
            sum = sum.add(value.score());
            normalized.add(new ScoringRubricItem(
                    point,
                    value.score(),
                    normalizeOptionalText(value.description(),
                            MAX_RUBRIC_DESCRIPTION_LENGTH, "评分说明")));
        }
        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw validation("评分要点分数之和必须等于 100");
        }
        return normalized;
    }

    private List<TestDataInput> normalizeTestData(List<TestDataInput> values) {
        if (values == null || values.isEmpty()) {
            throw testDataInvalid("测试数据至少需要 1 组");
        }
        if (values.size() > MAX_TEST_DATA_COUNT) {
            throw testDataInvalid("测试数据最多 " + MAX_TEST_DATA_COUNT + " 组");
        }
        long totalBytes = 0;
        List<TestDataInput> normalized = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            TestDataInput value = values.get(index);
            if (value == null || value.inputData() == null || value.outputData() == null) {
                throw testDataInvalid("第 " + (index + 1) + " 组测试数据的输入或输出为空");
            }
            long inputBytes = value.inputData().getBytes(StandardCharsets.UTF_8).length;
            long outputBytes = value.outputData().getBytes(StandardCharsets.UTF_8).length;
            if (inputBytes > MAX_TEST_DATA_ITEM_BYTES || outputBytes > MAX_TEST_DATA_ITEM_BYTES) {
                throw testDataInvalid("第 " + (index + 1)
                        + " 组测试数据的输入或输出超过 1 MB");
            }
            totalBytes += inputBytes + outputBytes;
            normalized.add(value);
        }
        if (totalBytes > MAX_TEST_DATA_TOTAL_BYTES) {
            throw testDataInvalid("测试数据总量超过 10 MB");
        }
        return normalized;
    }

    private List<String> normalizeTags(List<String> values) {
        if (values == null) {
            return List.of();
        }
        if (values.size() > MAX_TAG_COUNT) {
            throw validation("标签最多 " + MAX_TAG_COUNT + " 个");
        }
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String tag = value.trim();
            if (tag.isEmpty()) {
                continue;
            }
            if (tag.length() > MAX_TAG_LENGTH) {
                throw validation("单个标签不能超过 " + MAX_TAG_LENGTH + " 个字符");
            }
            tags.add(tag);
        }
        return List.copyOf(tags);
    }

    private String normalizeStatement(String type, String value) {
        if (TYPE_EXTERNAL.equals(type)) {
            String statement = normalizeOptionalText(value, MAX_STATEMENT_LENGTH, "题面");
            return statement == null ? "" : statement;
        }
        return requireText(value, MAX_STATEMENT_LENGTH, "题面");
    }

    private String normalizeType(String value) {
        if (value == null || value.isBlank()) {
            throw validation("题型不能为空");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!PROBLEM_TYPES.contains(normalized)) {
            throw validation("题型取值非法");
        }
        return normalized;
    }

    private String normalizeDifficulty(String value) {
        if (value == null || value.isBlank()) {
            throw validation("难度不能为空");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!DIFFICULTIES.contains(normalized)) {
            throw validation("难度取值非法");
        }
        return normalized;
    }

    private String normalizeObjectiveTypeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw validation("客观题类型不能为空");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!OBJECTIVE_TYPES.contains(normalized)) {
            throw optionsInvalid("客观题类型取值非法");
        }
        return normalized;
    }

    private Integer normalizeTimeLimit(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < MIN_TIME_LIMIT_MS || value > MAX_TIME_LIMIT_MS) {
            throw validation("时限必须在 " + MIN_TIME_LIMIT_MS + "–"
                    + MAX_TIME_LIMIT_MS + " 毫秒之间");
        }
        return value;
    }

    private Integer normalizeMemoryLimit(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < MIN_MEMORY_LIMIT_MB || value > MAX_MEMORY_LIMIT_MB) {
            throw validation("内存限制必须在 " + MIN_MEMORY_LIMIT_MB + "–"
                    + MAX_MEMORY_LIMIT_MB + " MB 之间");
        }
        return value;
    }

    private void normalizeMaxScore(Integer value) {
        if (value != null && value != 100) {
            throw validation("简答题满分固定为 100");
        }
    }

    private String normalizeExternalUrl(String value) {
        String url = requireText(value, MAX_EXTERNAL_URL_LENGTH, "外链地址");
        if (!isValidHttpUrl(url)) {
            throw validation("外链地址必须是合法的 http 或 https 链接");
        }
        return url;
    }

    private boolean isValidHttpUrl(String value) {
        if (!hasText(value)) {
            return false;
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            return scheme != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private String requireText(String value, int maxLength, String fieldName) {
        if (value == null) {
            throw validation(fieldName + "不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw validation(fieldName + "不能为空");
        }
        if (trimmed.length() > maxLength) {
            throw validation(fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }

    private String normalizeOptionalText(String value, int maxLength, String fieldName) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw validation(fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void validateSortOrder(int value, String fieldName) {
        if (value < 0 || value > MAX_SORT_ORDER) {
            throw validation(fieldName + "必须在 0–" + MAX_SORT_ORDER + " 之间");
        }
    }

    private void replaceTags(Long problemId, List<String> tags) {
        problemTagMapper.delete(Wrappers.<ProblemTag>lambdaQuery()
                .eq(ProblemTag::getProblemId, problemId));
        for (String tag : tags) {
            ProblemTag entity = new ProblemTag();
            entity.setProblemId(problemId);
            entity.setTagType("topic");
            entity.setTagValue(tag);
            problemTagMapper.insert(entity);
        }
    }

    private void insertSamples(Long problemId, List<ProblemSampleInput> samples) {
        for (ProblemSampleInput input : samples) {
            ProblemSample entity = new ProblemSample();
            entity.setProblemId(problemId);
            entity.setInputData(input.inputData());
            entity.setOutputData(input.outputData());
            entity.setExplanation(input.explanation());
            entity.setSortOrder(input.sortOrder());
            problemSampleMapper.insert(entity);
        }
    }

    private void replaceSamples(Long problemId, List<ProblemSampleInput> samples) {
        problemSampleMapper.delete(Wrappers.<ProblemSample>lambdaQuery()
                .eq(ProblemSample::getProblemId, problemId));
        insertSamples(problemId, samples);
    }

    private void insertOptions(Long problemId, List<ObjectiveOptionInput> options) {
        for (ObjectiveOptionInput input : options) {
            ObjectiveOption entity = new ObjectiveOption();
            entity.setProblemId(problemId);
            entity.setOptionKey(input.optionKey());
            entity.setContent(input.content());
            entity.setIsCorrect(Boolean.TRUE.equals(input.isCorrect()) ? 1 : 0);
            entity.setSortOrder(input.sortOrder());
            objectiveOptionMapper.insert(entity);
        }
    }

    private void replaceOptions(Long problemId, List<ObjectiveOptionInput> options) {
        objectiveOptionMapper.delete(Wrappers.<ObjectiveOption>lambdaQuery()
                .eq(ObjectiveOption::getProblemId, problemId));
        insertOptions(problemId, options);
    }

    private List<String> loadTags(Long problemId) {
        return problemTagMapper.selectList(Wrappers.<ProblemTag>lambdaQuery()
                        .eq(ProblemTag::getProblemId, problemId)
                        .eq(ProblemTag::getTagType, "topic")
                        .orderByAsc(ProblemTag::getTagValue))
                .stream()
                .map(ProblemTag::getTagValue)
                .toList();
    }

    private List<ProblemSampleView> loadSampleViews(Long problemId) {
        return problemSampleMapper.selectList(Wrappers.<ProblemSample>lambdaQuery()
                        .eq(ProblemSample::getProblemId, problemId)
                        .orderByAsc(ProblemSample::getSortOrder)
                        .orderByAsc(ProblemSample::getId))
                .stream()
                .map(sample -> new ProblemSampleView(
                        String.valueOf(sample.getId()),
                        sample.getInputData(),
                        sample.getOutputData(),
                        sample.getExplanation(),
                        sample.getSortOrder() == null ? 0 : sample.getSortOrder()))
                .toList();
    }

    private List<ObjectiveOptionView> loadOptionViews(Long problemId) {
        return objectiveOptionMapper.selectList(Wrappers.<ObjectiveOption>lambdaQuery()
                        .eq(ObjectiveOption::getProblemId, problemId)
                        .orderByAsc(ObjectiveOption::getSortOrder)
                        .orderByAsc(ObjectiveOption::getId))
                .stream()
                .map(option -> new ObjectiveOptionView(
                        String.valueOf(option.getId()),
                        option.getOptionKey(),
                        option.getContent(),
                        option.getIsCorrect() != null && option.getIsCorrect() == 1,
                        option.getSortOrder() == null ? 0 : option.getSortOrder()))
                .toList();
    }

    private List<ObjectiveOptionInput> loadOptionsAsInput(Long problemId) {
        return objectiveOptionMapper.selectList(Wrappers.<ObjectiveOption>lambdaQuery()
                        .eq(ObjectiveOption::getProblemId, problemId)
                        .orderByAsc(ObjectiveOption::getSortOrder)
                        .orderByAsc(ObjectiveOption::getId))
                .stream()
                .map(option -> new ObjectiveOptionInput(
                        option.getOptionKey(),
                        option.getContent(),
                        option.getIsCorrect() != null && option.getIsCorrect() == 1,
                        option.getSortOrder() == null ? 0 : option.getSortOrder()))
                .toList();
    }

    private String writeRubric(List<ScoringRubricItem> rubric) {
        if (rubric == null || rubric.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(rubric);
        } catch (JsonProcessingException exception) {
            throw validation("评分要点格式非法");
        }
    }

    private List<ScoringRubricItem> parseRubric(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<ScoringRubricItem> rubric = objectMapper.readValue(json, RUBRIC_TYPE);
            return rubric == null ? List.of() : rubric;
        } catch (JsonProcessingException exception) {
            // 历史数据异常时按未配置处理，发布校验会阻止继续发布。
            return List.of();
        }
    }

    private long countSamples(Long problemId) {
        return problemSampleMapper.selectCount(Wrappers.<ProblemSample>lambdaQuery()
                .eq(ProblemSample::getProblemId, problemId));
    }

    private long countTestData(Long problemId) {
        return testDataMapper.selectCount(Wrappers.<TestData>lambdaQuery()
                .eq(TestData::getProblemId, problemId));
    }

    private TestDataView toTestDataView(TestData entity) {
        return new TestDataView(
                String.valueOf(entity.getId()),
                entity.getInputData(),
                entity.getOutputData(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder());
    }

    private AdminProblemUsage loadUsage(Long problemId) {
        AdminProblemUsage usage = problemMapper.selectUsage(problemId);
        return usage == null ? new AdminProblemUsage() : usage;
    }

    private Problem requireProblem(Long problemId) {
        if (problemId == null || problemId <= 0) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null || problem.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }
        return problem;
    }

    private void requireOj(Problem problem) {
        if (!TYPE_OJ.equals(problem.getType())) {
            throw new BusinessException(ErrorCode.PROBLEM_TYPE_MISMATCH, "仅 OJ 题可操作测试数据");
        }
    }

    private void requirePermission(AdminPrincipal actor) {
        if (actor == null || !actor.hasPermission(PERMISSION)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "无权限操作题库");
        }
    }

    private BusinessException validation(String message) {
        return new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, message);
    }

    private BusinessException mismatch(String message) {
        return new BusinessException(ErrorCode.PROBLEM_TYPE_MISMATCH, message);
    }

    private BusinessException optionsInvalid(String message) {
        return new BusinessException(ErrorCode.PROBLEM_OPTIONS_INVALID, message);
    }

    private BusinessException testDataInvalid(String message) {
        return new BusinessException(ErrorCode.PROBLEM_TEST_DATA_INVALID, message);
    }

    private BusinessException configIncomplete(String message) {
        return new BusinessException(ErrorCode.PROBLEM_CONFIG_INCOMPLETE, message);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
