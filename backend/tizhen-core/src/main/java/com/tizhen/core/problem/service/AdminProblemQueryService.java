package com.tizhen.core.problem.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.problem.entity.Problem;
import com.tizhen.core.problem.entity.ProblemTag;
import com.tizhen.core.problem.mapper.ProblemMapper;
import com.tizhen.core.problem.mapper.ProblemTagMapper;
import com.tizhen.core.problem.model.AdminProblemBrief;
import com.tizhen.core.problem.model.AdminProblemSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台题库只读查询与题目发布状态校验。阶段题目选择器不读取题面、答案与测试数据。
 */
@Service
public class AdminProblemQueryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private static final Set<String> TYPES = Set.of("oj", "objective", "short_answer", "external");
    private static final Set<String> OBJECTIVE_TYPES = Set.of("judge", "single", "multiple");
    private static final Set<String> DIFFICULTIES = Set.of("entry", "basic", "advanced");
    private static final Set<String> STATUSES = Set.of("draft", "published", "offline");

    private final ProblemMapper problemMapper;
    private final ProblemTagMapper problemTagMapper;

    public AdminProblemQueryService(ProblemMapper problemMapper, ProblemTagMapper problemTagMapper) {
        this.problemMapper = problemMapper;
        this.problemTagMapper = problemTagMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<AdminProblemSummary> list(AdminPrincipal actor,
                                                String type,
                                                String objectiveType,
                                                String difficulty,
                                                String status,
                                                String source,
                                                String tag,
                                                String keyword,
                                                int page,
                                                int pageSize) {
        if (actor == null || !actor.hasPermission("problem:manage")) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "无权限查看题库");
        }
        String normalizedType = normalizeEnum(type, TYPES, "type");
        String normalizedObjectiveType = normalizeEnum(objectiveType, OBJECTIVE_TYPES, "objectiveType");
        String normalizedDifficulty = normalizeEnum(difficulty, DIFFICULTIES, "difficulty");
        String normalizedStatus = normalizeEnum(status, STATUSES, "status");
        String normalizedSource = normalizeFilterText(source, 64, "source");
        String normalizedTag = normalizeFilterText(tag, 64, "tag");
        String normalizedKeyword = normalizeFilterText(keyword, MAX_KEYWORD_LENGTH, "keyword");
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<Problem> result = new Page<>(safePage, safePageSize);
        problemMapper.selectAdminProblemPage(result, normalizedType, normalizedObjectiveType,
                normalizedDifficulty, normalizedStatus, normalizedSource, normalizedTag,
                normalizedKeyword);

        List<Long> problemIds = result.getRecords().stream()
                .map(Problem::getId)
                .toList();
        Map<Long, List<String>> tagsByProblem = loadTags(problemIds);
        List<AdminProblemSummary> items = result.getRecords().stream()
                .map(problem -> toSummary(problem, tagsByProblem.getOrDefault(problem.getId(), List.of())))
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    /**
     * 校验题目均存在、未删除且已发布。阶段题目关联只允许引用已发布题目。
     */
    @Transactional(readOnly = true)
    public void assertAllPublished(Collection<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return;
        }
        Map<Long, Problem> problems = problemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, problem -> problem));
        for (Long problemId : problemIds) {
            Problem problem = problems.get(problemId);
            if (problem == null || problem.getDeletedAt() != null) {
                throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
            }
            if (!"published".equals(problem.getStatus())) {
                throw new BusinessException(ErrorCode.PROBLEM_NOT_PUBLISHED);
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, AdminProblemBrief> findBriefs(Collection<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, AdminProblemBrief> briefs = new LinkedHashMap<>();
        for (Problem problem : problemMapper.selectBatchIds(problemIds)) {
            if (problem.getDeletedAt() == null) {
                briefs.put(problem.getId(), toBrief(problem));
            }
        }
        return briefs;
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
                        Collectors.mapping(ProblemTag::getTagValue, Collectors.toList())));
    }

    private AdminProblemSummary toSummary(Problem problem, List<String> tags) {
        return new AdminProblemSummary(
                String.valueOf(problem.getId()),
                problem.getType(),
                problem.getObjectiveType(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getSource(),
                problem.getPlatform(),
                problem.getStatus(),
                tags,
                TimeUtils.toInstant(problem.getCreatedAt()),
                TimeUtils.toInstant(problem.getUpdatedAt()));
    }

    private AdminProblemBrief toBrief(Problem problem) {
        return new AdminProblemBrief(
                String.valueOf(problem.getId()),
                problem.getType(),
                problem.getObjectiveType(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getSource(),
                problem.getPlatform(),
                problem.getStatus());
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
