package com.tizhen.core.judge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.judge.entity.FirstAc;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.mapper.FirstAcMapper;
import com.tizhen.core.judge.mapper.SubmissionMapper;
import com.tizhen.core.judge.mapper.WrongBookMapper;
import com.tizhen.core.judge.model.SubmissionCreateCommand;
import com.tizhen.core.judge.model.SubmissionDetailView;
import com.tizhen.core.judge.model.SubmissionListItem;
import com.tizhen.core.judge.model.SubmissionListRow;
import com.tizhen.core.judge.model.SubmissionProblemBrief;
import com.tizhen.core.problem.entity.ObjectiveOption;
import com.tizhen.core.problem.entity.Problem;
import com.tizhen.core.problem.mapper.ObjectiveOptionMapper;
import com.tizhen.core.problem.mapper.ProblemMapper;
import com.tizhen.core.problem.model.ScoringRubricItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 选手提交与作答记录。客观题同步判分；OJ 与简答题只落库，
 * 交由 worker 异步判题/判分，提交响应不假装已经完成。
 */
@Service
public class SubmissionService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final String TYPE_OJ = "oj";
    private static final String TYPE_OBJECTIVE = "objective";
    private static final String TYPE_SHORT_ANSWER = "short_answer";

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_JUDGING = "judging";
    private static final String STATUS_GRADING = "grading";
    private static final String STATUS_FINISHED = "finished";
    private static final String STATUS_FAILED = "failed";

    private static final String VERDICT_AC = "AC";
    private static final String VERDICT_WA = "WA";

    private static final Set<String> SUBMITTABLE_TYPES =
            Set.of(TYPE_OJ, TYPE_OBJECTIVE, TYPE_SHORT_ANSWER);
    private static final Set<String> LANGUAGES = Set.of("cpp17", "python310");
    private static final Set<String> STATUSES =
            Set.of(STATUS_PENDING, STATUS_JUDGING, STATUS_GRADING, STATUS_FINISHED, STATUS_FAILED);

    private static final int MAX_CODE_BYTES = 64 * 1024;
    private static final int MAX_ANSWER_TEXT_LENGTH = 10_000;
    private static final int MAX_SELECTED_OPTIONS = 10;

    private static final BigDecimal FULL_SCORE = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO_SCORE = BigDecimal.ZERO;

    private final SubmissionMapper submissionMapper;
    private final FirstAcMapper firstAcMapper;
    private final WrongBookMapper wrongBookMapper;
    private final ProblemMapper problemMapper;
    private final ObjectiveOptionMapper objectiveOptionMapper;
    private final SubmissionRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public SubmissionService(SubmissionMapper submissionMapper,
                             FirstAcMapper firstAcMapper,
                             WrongBookMapper wrongBookMapper,
                             ProblemMapper problemMapper,
                             ObjectiveOptionMapper objectiveOptionMapper,
                             SubmissionRateLimiter rateLimiter,
                             ObjectMapper objectMapper) {
        this.submissionMapper = submissionMapper;
        this.firstAcMapper = firstAcMapper;
        this.wrongBookMapper = wrongBookMapper;
        this.problemMapper = problemMapper;
        this.objectiveOptionMapper = objectiveOptionMapper;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SubmissionDetailView create(Long userId, Long problemId, SubmissionCreateCommand command) {
        return create(userId, problemId, null, command);
    }

    /**
     * 对战提交与普通提交共用判题、首次 AC 与错题本链路，仅额外写入 battleId。
     */
    @Transactional
    public SubmissionDetailView createForBattle(Long userId,
                                                Long problemId,
                                                Long battleId,
                                                SubmissionCreateCommand command) {
        if (battleId == null || battleId <= 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "battleId 非法");
        }
        return create(userId, problemId, battleId, command);
    }

    private SubmissionDetailView create(Long userId,
                                        Long problemId,
                                        Long battleId,
                                        SubmissionCreateCommand command) {
        Problem problem = requirePublishedProblem(problemId);
        if (!SUBMITTABLE_TYPES.contains(problem.getType())) {
            throw new BusinessException(ErrorCode.PROBLEM_TYPE_MISMATCH, "该题型不支持站内提交");
        }
        assertPayloadMatchesType(problem.getType(), command);
        rateLimiter.assertAllowed(userId);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setProblemId(problemId);
        submission.setProblemType(problem.getType());
        submission.setBattleId(battleId);
        submission.setJudgeAttempts(0);
        submission.setCreatedAt(now);

        if (TYPE_OJ.equals(problem.getType())) {
            submission.setLanguage(command.language().trim().toLowerCase(Locale.ROOT));
            submission.setCode(command.code());
            submission.setStatus(STATUS_PENDING);
            submissionMapper.insert(submission);
        } else if (TYPE_OBJECTIVE.equals(problem.getType())) {
            submission = gradeObjective(problem, submission, command.selectedOptions(), now);
        } else {
            submission.setAnswerText(command.answerText());
            submission.setStatus(STATUS_GRADING);
            submissionMapper.insert(submission);
        }

        return getDetail(userId, submission.getId());
    }

    @Transactional(readOnly = true)
    public PageResult<SubmissionListItem> list(Long userId,
                                               Long problemId,
                                               String problemType,
                                               String status,
                                               int page,
                                               int pageSize) {
        if (problemId != null && problemId <= 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "problemId 非法");
        }
        String normalizedType = normalizeEnum(problemType, SUBMITTABLE_TYPES, "problemType");
        String normalizedStatus = normalizeEnum(status, STATUSES, "status");
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<SubmissionListRow> result = new Page<>(safePage, safePageSize);
        submissionMapper.selectUserSubmissionPage(result, userId, problemId,
                normalizedType, normalizedStatus);

        List<SubmissionListItem> items = result.getRecords().stream()
                .map(row -> new SubmissionListItem(
                        String.valueOf(row.getId()),
                        new SubmissionProblemBrief(
                                String.valueOf(row.getProblemId()),
                                row.getProblemType(),
                                row.getProblemTitle() == null ? "题目" : row.getProblemTitle()),
                        row.getBattleId() == null ? null : String.valueOf(row.getBattleId()),
                        row.getLanguage(),
                        row.getStatus(),
                        row.getVerdict(),
                        row.getScore(),
                        TimeUtils.toInstant(row.getCreatedAt()),
                        TimeUtils.toInstant(row.getJudgedAt())))
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional(readOnly = true)
    public SubmissionDetailView getDetail(Long userId, Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null || !submission.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND);
        }
        Problem problem = problemMapper.selectById(submission.getProblemId());
        if (problem == null) {
            throw new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND);
        }

        boolean inProgress = STATUS_PENDING.equals(submission.getStatus())
                || STATUS_JUDGING.equals(submission.getStatus())
                || STATUS_GRADING.equals(submission.getStatus());
        boolean finished = STATUS_FINISHED.equals(submission.getStatus());
        boolean objective = TYPE_OBJECTIVE.equals(submission.getProblemType());
        boolean shortAnswer = TYPE_SHORT_ANSWER.equals(submission.getProblemType());

        List<String> correctOptions = finished && objective
                ? loadCorrectOptions(problem.getId())
                : null;
        String explanation = finished && objective ? problem.getExplanation() : null;
        String referenceAnswer = finished && shortAnswer ? problem.getReferenceAnswer() : null;
        List<ScoringRubricItem> scoringRubric = finished && shortAnswer
                ? parseRubric(problem.getScoringRubric())
                : null;

        boolean firstAc = firstAcMapper.selectCount(Wrappers.<FirstAc>lambdaQuery()
                .eq(FirstAc::getSubmissionId, submissionId)) > 0;

        return new SubmissionDetailView(
                String.valueOf(submission.getId()),
                new SubmissionProblemBrief(
                        String.valueOf(problem.getId()),
                        submission.getProblemType(),
                        problem.getTitle()),
                submission.getBattleId() == null ? null : String.valueOf(submission.getBattleId()),
                submission.getLanguage(),
                inProgress ? null : submission.getCode(),
                inProgress ? null : parseAnswerJson(submission.getAnswerJson()),
                inProgress ? null : submission.getAnswerText(),
                submission.getStatus(),
                submission.getVerdict(),
                submission.getScore(),
                finished ? submission.getAiFeedback() : null,
                submission.getFailureReason(),
                inProgress ? null : submission.getJudgeDetail(),
                inProgress ? null : submission.getCompileOutput(),
                submission.getJudgeTimeMs(),
                submission.getMemoryKb(),
                TimeUtils.toInstant(submission.getJudgeStartedAt()),
                TimeUtils.toInstant(submission.getJudgedAt()),
                TimeUtils.toInstant(submission.getCreatedAt()),
                firstAc,
                correctOptions,
                explanation,
                referenceAnswer,
                scoringRubric);
    }

    private Submission gradeObjective(Problem problem,
                                      Submission submission,
                                      List<String> selectedOptions,
                                      LocalDateTime now) {
        List<ObjectiveOption> options = objectiveOptionMapper.selectList(
                Wrappers.<ObjectiveOption>lambdaQuery()
                        .eq(ObjectiveOption::getProblemId, problem.getId()));
        if (options.isEmpty()) {
            throw new BusinessException(ErrorCode.PROBLEM_CONFIG_INCOMPLETE, "题目选项未配置");
        }
        Set<String> validKeys = new HashSet<>();
        Set<String> correctKeys = new HashSet<>();
        for (ObjectiveOption option : options) {
            validKeys.add(option.getOptionKey());
            if (option.getIsCorrect() != null && option.getIsCorrect() == 1) {
                correctKeys.add(option.getOptionKey());
            }
        }
        if (correctKeys.isEmpty()) {
            throw new BusinessException(ErrorCode.PROBLEM_CONFIG_INCOMPLETE, "题目未配置正确答案");
        }

        List<String> normalized = normalizeSelectedOptions(selectedOptions);
        String objectiveType = problem.getObjectiveType();
        if (("judge".equals(objectiveType) || "single".equals(objectiveType))
                && normalized.size() != 1) {
            throw new BusinessException(ErrorCode.SUBMISSION_ANSWER_INVALID, "该题只能选择一个选项");
        }
        if (!validKeys.containsAll(normalized)) {
            throw new BusinessException(ErrorCode.SUBMISSION_ANSWER_INVALID, "所选选项不属于该题");
        }

        boolean correct = new HashSet<>(normalized).equals(correctKeys);
        submission.setAnswerJson(writeAnswerJson(normalized));
        submission.setStatus(STATUS_FINISHED);
        submission.setVerdict(correct ? VERDICT_AC : VERDICT_WA);
        submission.setScore(correct ? FULL_SCORE : ZERO_SCORE);
        submission.setJudgedAt(now);
        submissionMapper.insert(submission);

        if (!correct) {
            wrongBookMapper.upsert(submission.getUserId(), problem.getId(), VERDICT_WA, null, now);
        }
        return submission;
    }

    private List<String> normalizeSelectedOptions(List<String> selectedOptions) {
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            throw new BusinessException(ErrorCode.SUBMISSION_ANSWER_INVALID, "请至少选择一个选项");
        }
        if (selectedOptions.size() > MAX_SELECTED_OPTIONS) {
            throw new BusinessException(ErrorCode.SUBMISSION_ANSWER_INVALID, "所选选项数量过多");
        }
        List<String> normalized = selectedOptions.stream()
                .map(value -> value == null ? "" : value.trim())
                .toList();
        if (normalized.stream().anyMatch(String::isEmpty)) {
            throw new BusinessException(ErrorCode.SUBMISSION_ANSWER_INVALID, "选项键不能为空");
        }
        if (new HashSet<>(normalized).size() != normalized.size()) {
            throw new BusinessException(ErrorCode.SUBMISSION_ANSWER_INVALID, "选项键不能重复");
        }
        return normalized;
    }

    private void assertPayloadMatchesType(String type, SubmissionCreateCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.SUBMISSION_ANSWER_INVALID);
        }
        boolean hasOj = command.language() != null || command.code() != null;
        boolean hasObjective = command.selectedOptions() != null;
        boolean hasShort = command.answerText() != null;

        if (TYPE_OJ.equals(type)) {
            if (!hasOj && (hasObjective || hasShort)) {
                throw new BusinessException(ErrorCode.PROBLEM_TYPE_MISMATCH);
            }
            if (command.language() == null || command.language().isBlank()
                    || command.code() == null || command.code().isBlank()) {
                throw new BusinessException(ErrorCode.SUBMISSION_ANSWER_INVALID, "请选择语言并填写代码");
            }
            String language = command.language().trim().toLowerCase(Locale.ROOT);
            if (!LANGUAGES.contains(language)) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "language 取值非法");
            }
            if (command.code().getBytes(StandardCharsets.UTF_8).length > MAX_CODE_BYTES) {
                throw new BusinessException(ErrorCode.SUBMISSION_ANSWER_INVALID, "代码不能超过 64 KB");
            }
        } else if (TYPE_OBJECTIVE.equals(type)) {
            if (!hasObjective && (hasOj || hasShort)) {
                throw new BusinessException(ErrorCode.PROBLEM_TYPE_MISMATCH);
            }
            if (!hasObjective) {
                throw new BusinessException(ErrorCode.SUBMISSION_ANSWER_INVALID, "请选择答案选项");
            }
        } else {
            if (!hasShort && (hasOj || hasObjective)) {
                throw new BusinessException(ErrorCode.PROBLEM_TYPE_MISMATCH);
            }
            if (command.answerText() == null || command.answerText().isBlank()) {
                throw new BusinessException(ErrorCode.SUBMISSION_ANSWER_INVALID, "请填写答案");
            }
            if (command.answerText().length() > MAX_ANSWER_TEXT_LENGTH) {
                throw new BusinessException(ErrorCode.SUBMISSION_ANSWER_INVALID,
                        "答案不能超过 10000 个字符");
            }
        }
    }

    private Problem requirePublishedProblem(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null || problem.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }
        if (!"published".equals(problem.getStatus())) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_PUBLISHED);
        }
        return problem;
    }

    private List<String> loadCorrectOptions(Long problemId) {
        return objectiveOptionMapper.selectList(Wrappers.<ObjectiveOption>lambdaQuery()
                        .eq(ObjectiveOption::getProblemId, problemId)
                        .eq(ObjectiveOption::getIsCorrect, 1)
                        .orderByAsc(ObjectiveOption::getSortOrder)
                        .orderByAsc(ObjectiveOption::getOptionKey))
                .stream()
                .map(ObjectiveOption::getOptionKey)
                .toList();
    }

    private String writeAnswerJson(List<String> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }

    private List<String> parseAnswerJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private List<ScoringRubricItem> parseRubric(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<ScoringRubricItem>>() {
            });
        } catch (JsonProcessingException exception) {
            return null;
        }
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
}
