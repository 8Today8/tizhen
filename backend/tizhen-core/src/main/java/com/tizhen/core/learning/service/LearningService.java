package com.tizhen.core.learning.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.learning.entity.ExternalCompletion;
import com.tizhen.core.learning.entity.LearningProgress;
import com.tizhen.core.learning.entity.LearningRoute;
import com.tizhen.core.learning.entity.LearningStage;
import com.tizhen.core.learning.entity.StageCourse;
import com.tizhen.core.learning.mapper.ExternalCompletionMapper;
import com.tizhen.core.learning.mapper.LearningProgressMapper;
import com.tizhen.core.learning.mapper.LearningRouteMapper;
import com.tizhen.core.learning.mapper.LearningStageMapper;
import com.tizhen.core.learning.mapper.StageCourseMapper;
import com.tizhen.core.learning.mapper.StageProblemMapper;
import com.tizhen.core.learning.model.ClientStageProblemRow;
import com.tizhen.core.learning.model.CourseView;
import com.tizhen.core.learning.model.ExternalCompletionView;
import com.tizhen.core.learning.model.RouteDetail;
import com.tizhen.core.learning.model.RouteProgressCount;
import com.tizhen.core.learning.model.RouteSummary;
import com.tizhen.core.learning.model.StageDetail;
import com.tizhen.core.learning.model.StageProblemCount;
import com.tizhen.core.learning.model.StageProblemSummary;
import com.tizhen.core.learning.model.StageProblemView;
import com.tizhen.core.learning.model.StageProgressView;
import com.tizhen.core.learning.model.StageSummary;
import com.tizhen.core.problem.entity.Problem;
import com.tizhen.core.problem.mapper.ProblemMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 选手端学习路线浏览与进度维护。只暴露已发布内容，进度按阶段完成数计算。
 */
@Service
public class LearningService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String STATUS_PUBLISHED = "published";
    private static final String TYPE_EXTERNAL = "external";
    private static final String SOURCE_MANUAL = "manual";
    private static final int COMPLETED = 1;
    private static final int NOT_COMPLETED = 0;

    private final LearningRouteMapper learningRouteMapper;
    private final LearningStageMapper learningStageMapper;
    private final StageCourseMapper stageCourseMapper;
    private final StageProblemMapper stageProblemMapper;
    private final LearningProgressMapper learningProgressMapper;
    private final ExternalCompletionMapper externalCompletionMapper;
    private final ProblemMapper problemMapper;

    public LearningService(LearningRouteMapper learningRouteMapper,
                           LearningStageMapper learningStageMapper,
                           StageCourseMapper stageCourseMapper,
                           StageProblemMapper stageProblemMapper,
                           LearningProgressMapper learningProgressMapper,
                           ExternalCompletionMapper externalCompletionMapper,
                           ProblemMapper problemMapper) {
        this.learningRouteMapper = learningRouteMapper;
        this.learningStageMapper = learningStageMapper;
        this.stageCourseMapper = stageCourseMapper;
        this.stageProblemMapper = stageProblemMapper;
        this.learningProgressMapper = learningProgressMapper;
        this.externalCompletionMapper = externalCompletionMapper;
        this.problemMapper = problemMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<RouteSummary> listRoutes(Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<LearningRoute> result = new Page<>(safePage, safePageSize);
        learningRouteMapper.selectPage(result, Wrappers.<LearningRoute>lambdaQuery()
                .eq(LearningRoute::getStatus, STATUS_PUBLISHED)
                .isNull(LearningRoute::getDeletedAt)
                .orderByAsc(LearningRoute::getSortOrder)
                .orderByAsc(LearningRoute::getId));

        List<LearningRoute> routes = result.getRecords();
        Map<Long, RouteProgressCount> progressCounts = loadRouteProgressCounts(userId, routes);
        List<RouteSummary> items = routes.stream()
                .map(route -> toRouteSummary(route, progressCounts.get(route.getId())))
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional(readOnly = true)
    public RouteDetail getRoute(Long userId, Long routeId) {
        LearningRoute route = requirePublishedRoute(routeId);
        List<LearningStage> stages = learningStageMapper.selectList(Wrappers.<LearningStage>lambdaQuery()
                .eq(LearningStage::getRouteId, routeId)
                .eq(LearningStage::getStatus, STATUS_PUBLISHED)
                .isNull(LearningStage::getDeletedAt)
                .orderByAsc(LearningStage::getSortOrder)
                .orderByAsc(LearningStage::getId));

        List<Long> stageIds = stages.stream().map(LearningStage::getId).toList();
        Map<Long, List<StageCourse>> coursesByStage = loadCoursesByStage(stageIds);
        Map<Long, LearningProgress> progressByStage = loadCompletedProgress(userId, stageIds);
        Map<Long, Long> problemCounts = loadProblemCounts(stageIds);

        List<StageSummary> stageSummaries = stages.stream()
                .map(stage -> {
                    List<StageCourse> courses = coursesByStage.getOrDefault(stage.getId(), List.of());
                    LearningProgress progress = progressByStage.get(stage.getId());
                    boolean completed = isCompleted(progress);
                    return new StageSummary(
                            String.valueOf(stage.getId()),
                            stage.getName(),
                            sortOrder(stage.getSortOrder()),
                            completed,
                            completed ? TimeUtils.toInstant(progress.getCompletedAt()) : null,
                            courses.size(),
                            problemCounts.getOrDefault(stage.getId(), 0L),
                            courses.stream().map(this::toCourseView).toList());
                })
                .toList();

        long completedStageCount = stageSummaries.stream().filter(StageSummary::completed).count();
        long stageCount = stageSummaries.size();
        return new RouteDetail(
                String.valueOf(route.getId()),
                route.getName(),
                route.getDescription(),
                route.getCoverUrl(),
                sortOrder(route.getSortOrder()),
                stageCount,
                completedStageCount,
                progressPercent(completedStageCount, stageCount),
                stageSummaries);
    }

    @Transactional(readOnly = true)
    public StageDetail getStage(Long userId, Long stageId) {
        LearningStage stage = requirePublishedStage(stageId);
        LearningRoute route = requirePublishedRoute(stage.getRouteId());

        LearningProgress progress = learningProgressMapper.selectOne(Wrappers.<LearningProgress>lambdaQuery()
                .eq(LearningProgress::getUserId, userId)
                .eq(LearningProgress::getStageId, stageId)
                .last("LIMIT 1"));
        List<StageCourse> courses = stageCourseMapper.selectList(Wrappers.<StageCourse>lambdaQuery()
                .eq(StageCourse::getStageId, stageId)
                .eq(StageCourse::getStatus, STATUS_PUBLISHED)
                .isNull(StageCourse::getDeletedAt)
                .orderByAsc(StageCourse::getSortOrder)
                .orderByAsc(StageCourse::getId));
        List<ClientStageProblemRow> problemRows =
                stageProblemMapper.selectPublishedStageProblems(stageId, userId);

        boolean completed = isCompleted(progress);
        List<StageProblemView> problems = problemRows.stream()
                .map(this::toStageProblemView)
                .toList();
        return new StageDetail(
                String.valueOf(stage.getId()),
                String.valueOf(route.getId()),
                route.getName(),
                stage.getName(),
                stage.getContent(),
                completed,
                completed ? TimeUtils.toInstant(progress.getCompletedAt()) : null,
                courses.stream().map(this::toCourseView).toList(),
                problems);
    }

    @Transactional
    public StageProgressView updateStageProgress(Long userId, Long stageId, boolean completed) {
        LearningStage stage = requirePublishedStage(stageId);
        requirePublishedRoute(stage.getRouteId());

        LearningProgress progress = findStageProgress(userId, stageId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (completed) {
            progress = markStageCompleted(userId, stageId, progress, now);
        } else if (progress != null && isCompleted(progress)) {
            // 显式 set 为空，避免 MyBatis-Plus 默认忽略 null 字段而留下旧完成时间。
            learningProgressMapper.update(null, Wrappers.<LearningProgress>lambdaUpdate()
                    .eq(LearningProgress::getId, progress.getId())
                    .set(LearningProgress::getCompleted, NOT_COMPLETED)
                    .set(LearningProgress::getCompletedAt, null)
                    .set(LearningProgress::getUpdatedAt, now));
        }

        return new StageProgressView(
                String.valueOf(stageId),
                completed,
                completed ? TimeUtils.toInstant(progress.getCompletedAt()) : null);
    }

    @Transactional
    public ExternalCompletionView updateExternalCompletion(Long userId, Long problemId, boolean completed) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null
                || problem.getDeletedAt() != null
                || !STATUS_PUBLISHED.equals(problem.getStatus())) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }
        if (!TYPE_EXTERNAL.equals(problem.getType())) {
            throw new BusinessException(ErrorCode.PROBLEM_TYPE_MISMATCH);
        }

        ExternalCompletion record = findExternalCompletion(userId, problemId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (completed) {
            record = markExternalCompleted(userId, problemId, record, now);
        } else if (record != null && isCompleted(record.getCompleted())) {
            externalCompletionMapper.update(null, Wrappers.<ExternalCompletion>lambdaUpdate()
                    .eq(ExternalCompletion::getId, record.getId())
                    .set(ExternalCompletion::getCompleted, NOT_COMPLETED)
                    .set(ExternalCompletion::getCompletionSource, SOURCE_MANUAL)
                    .set(ExternalCompletion::getCompletedAt, null)
                    .set(ExternalCompletion::getUpdatedAt, now));
        }

        return new ExternalCompletionView(
                String.valueOf(problemId),
                completed,
                SOURCE_MANUAL,
                completed ? TimeUtils.toInstant(record.getCompletedAt()) : null);
    }

    private RouteSummary toRouteSummary(LearningRoute route, RouteProgressCount count) {
        long stageCount = count == null || count.getStageCount() == null ? 0L : count.getStageCount();
        long completedStageCount = count == null || count.getCompletedStageCount() == null
                ? 0L
                : count.getCompletedStageCount();
        return new RouteSummary(
                String.valueOf(route.getId()),
                route.getName(),
                route.getDescription(),
                route.getCoverUrl(),
                sortOrder(route.getSortOrder()),
                stageCount,
                completedStageCount,
                progressPercent(completedStageCount, stageCount));
    }

    private Map<Long, RouteProgressCount> loadRouteProgressCounts(Long userId, List<LearningRoute> routes) {
        if (routes.isEmpty()) {
            return Map.of();
        }
        List<Long> routeIds = routes.stream().map(LearningRoute::getId).toList();
        return learningRouteMapper.selectRouteProgressCounts(userId, routeIds).stream()
                .collect(Collectors.toMap(RouteProgressCount::getRouteId, Function.identity()));
    }

    private Map<Long, List<StageCourse>> loadCoursesByStage(List<Long> stageIds) {
        if (stageIds.isEmpty()) {
            return Map.of();
        }
        return stageCourseMapper.selectList(Wrappers.<StageCourse>lambdaQuery()
                        .in(StageCourse::getStageId, stageIds)
                        .eq(StageCourse::getStatus, STATUS_PUBLISHED)
                        .isNull(StageCourse::getDeletedAt)
                        .orderByAsc(StageCourse::getStageId)
                        .orderByAsc(StageCourse::getSortOrder)
                        .orderByAsc(StageCourse::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        StageCourse::getStageId,
                        LinkedHashMap::new,
                        Collectors.toCollection(ArrayList::new)));
    }

    private Map<Long, LearningProgress> loadCompletedProgress(Long userId, List<Long> stageIds) {
        if (stageIds.isEmpty()) {
            return Map.of();
        }
        return learningProgressMapper.selectList(Wrappers.<LearningProgress>lambdaQuery()
                        .eq(LearningProgress::getUserId, userId)
                        .in(LearningProgress::getStageId, stageIds)
                        .eq(LearningProgress::getCompleted, COMPLETED))
                .stream()
                .collect(Collectors.toMap(LearningProgress::getStageId, Function.identity()));
    }

    private Map<Long, Long> loadProblemCounts(List<Long> stageIds) {
        if (stageIds.isEmpty()) {
            return Map.of();
        }
        return stageProblemMapper.countPublishedProblemsByStageIds(stageIds).stream()
                .collect(Collectors.toMap(StageProblemCount::getStageId, StageProblemCount::getProblemCount));
    }

    private CourseView toCourseView(StageCourse course) {
        return new CourseView(
                String.valueOf(course.getId()),
                course.getTitle(),
                course.getUrl(),
                course.getDurationSeconds(),
                course.getSource(),
                sortOrder(course.getSortOrder()));
    }

    private StageProblemView toStageProblemView(ClientStageProblemRow row) {
        boolean external = TYPE_EXTERNAL.equals(row.getType());
        StageProblemSummary problem = new StageProblemSummary(
                String.valueOf(row.getProblemId()),
                row.getType(),
                row.getObjectiveType(),
                row.getTitle(),
                row.getDifficulty(),
                row.getSource(),
                external ? row.getPlatform() : null,
                external ? row.getExternalUrl() : null);
        return new StageProblemView(
                problem,
                sortOrder(row.getSortOrder()),
                external ? Boolean.TRUE.equals(row.getExternalCompleted()) : null,
                external ? TimeUtils.toInstant(row.getExternalCompletedAt()) : null);
    }

    private LearningProgress markStageCompleted(Long userId,
                                                Long stageId,
                                                LearningProgress progress,
                                                LocalDateTime now) {
        if (progress == null) {
            LearningProgress created = new LearningProgress();
            created.setUserId(userId);
            created.setStageId(stageId);
            created.setCompleted(COMPLETED);
            created.setCompletedAt(now);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            try {
                learningProgressMapper.insert(created);
                return created;
            } catch (DuplicateKeyException exception) {
                // 并发标记时由唯一键兜底。当前读可看到并发事务刚提交的记录。
                LearningProgress existing = findStageProgressForUpdate(userId, stageId);
                if (existing == null) {
                    throw exception;
                }
                return markStageCompleted(userId, stageId, existing, now);
            }
        }

        if (!isCompleted(progress)) {
            progress.setCompleted(COMPLETED);
            progress.setCompletedAt(now);
            progress.setUpdatedAt(now);
            learningProgressMapper.updateById(progress);
        }
        return progress;
    }

    private ExternalCompletion markExternalCompleted(Long userId,
                                                     Long problemId,
                                                     ExternalCompletion record,
                                                     LocalDateTime now) {
        if (record == null) {
            ExternalCompletion created = new ExternalCompletion();
            created.setUserId(userId);
            created.setProblemId(problemId);
            created.setCompleted(COMPLETED);
            created.setCompletionSource(SOURCE_MANUAL);
            created.setCompletedAt(now);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            try {
                externalCompletionMapper.insert(created);
                return created;
            } catch (DuplicateKeyException exception) {
                ExternalCompletion existing = findExternalCompletionForUpdate(userId, problemId);
                if (existing == null) {
                    throw exception;
                }
                return markExternalCompleted(userId, problemId, existing, now);
            }
        }

        if (!isCompleted(record.getCompleted())) {
            record.setCompleted(COMPLETED);
            record.setCompletionSource(SOURCE_MANUAL);
            record.setCompletedAt(now);
            record.setUpdatedAt(now);
            externalCompletionMapper.updateById(record);
        }
        return record;
    }

    private LearningProgress findStageProgress(Long userId, Long stageId) {
        return learningProgressMapper.selectOne(Wrappers.<LearningProgress>lambdaQuery()
                .eq(LearningProgress::getUserId, userId)
                .eq(LearningProgress::getStageId, stageId)
                .last("LIMIT 1"));
    }

    private LearningProgress findStageProgressForUpdate(Long userId, Long stageId) {
        return learningProgressMapper.selectOne(Wrappers.<LearningProgress>lambdaQuery()
                .eq(LearningProgress::getUserId, userId)
                .eq(LearningProgress::getStageId, stageId)
                .last("FOR UPDATE"));
    }

    private ExternalCompletion findExternalCompletion(Long userId, Long problemId) {
        return externalCompletionMapper.selectOne(Wrappers.<ExternalCompletion>lambdaQuery()
                .eq(ExternalCompletion::getUserId, userId)
                .eq(ExternalCompletion::getProblemId, problemId)
                .last("LIMIT 1"));
    }

    private ExternalCompletion findExternalCompletionForUpdate(Long userId, Long problemId) {
        return externalCompletionMapper.selectOne(Wrappers.<ExternalCompletion>lambdaQuery()
                .eq(ExternalCompletion::getUserId, userId)
                .eq(ExternalCompletion::getProblemId, problemId)
                .last("FOR UPDATE"));
    }

    private LearningRoute requirePublishedRoute(Long routeId) {
        LearningRoute route = learningRouteMapper.selectById(routeId);
        if (route == null
                || route.getDeletedAt() != null
                || !STATUS_PUBLISHED.equals(route.getStatus())) {
            throw new BusinessException(ErrorCode.LEARNING_ROUTE_NOT_FOUND);
        }
        return route;
    }

    private LearningStage requirePublishedStage(Long stageId) {
        LearningStage stage = learningStageMapper.selectById(stageId);
        if (stage == null
                || stage.getDeletedAt() != null
                || !STATUS_PUBLISHED.equals(stage.getStatus())) {
            throw new BusinessException(ErrorCode.LEARNING_STAGE_NOT_FOUND);
        }
        return stage;
    }

    private boolean isCompleted(LearningProgress progress) {
        return progress != null && isCompleted(progress.getCompleted());
    }

    private boolean isCompleted(Integer completed) {
        return completed != null && completed == COMPLETED;
    }

    private int sortOrder(Integer value) {
        return value == null ? 0 : value;
    }

    private int progressPercent(long completedStageCount, long stageCount) {
        if (stageCount <= 0) {
            return 0;
        }
        long percent = Math.round(completedStageCount * 100.0 / stageCount);
        return (int) Math.max(0, Math.min(100, percent));
    }
}
