package com.tizhen.core.learning.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.model.LoginMetadata;
import com.tizhen.core.admin.service.OperationLogService;
import com.tizhen.core.learning.entity.LearningRoute;
import com.tizhen.core.learning.entity.LearningStage;
import com.tizhen.core.learning.entity.StageCourse;
import com.tizhen.core.learning.entity.StageProblem;
import com.tizhen.core.learning.mapper.LearningProgressMapper;
import com.tizhen.core.learning.mapper.LearningRouteMapper;
import com.tizhen.core.learning.mapper.LearningStageMapper;
import com.tizhen.core.learning.mapper.StageCourseMapper;
import com.tizhen.core.learning.mapper.StageProblemMapper;
import com.tizhen.core.learning.model.AdminCourseView;
import com.tizhen.core.learning.model.AdminRouteDetail;
import com.tizhen.core.learning.model.AdminRouteRow;
import com.tizhen.core.learning.model.AdminRouteView;
import com.tizhen.core.learning.model.AdminStageDetail;
import com.tizhen.core.learning.model.AdminStageProblemRow;
import com.tizhen.core.learning.model.AdminStageProblemView;
import com.tizhen.core.learning.model.AdminStageRow;
import com.tizhen.core.learning.model.AdminStageSummary;
import com.tizhen.core.learning.model.CourseCreateCommand;
import com.tizhen.core.learning.model.CourseUpdateCommand;
import com.tizhen.core.learning.model.RouteCreateCommand;
import com.tizhen.core.learning.model.RouteUpdateCommand;
import com.tizhen.core.learning.model.StageCreateCommand;
import com.tizhen.core.learning.model.StageProblemCommand;
import com.tizhen.core.learning.model.StageUpdateCommand;
import com.tizhen.core.problem.model.AdminProblemBrief;
import com.tizhen.core.problem.service.AdminProblemQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 管理端学习内容维护。路线、阶段、课程使用软删除；阶段题目关联采用整体替换。
 */
@Service
public class AdminLearningService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_FILTER_LENGTH = 100;
    private static final int MAX_ROUTE_NAME_LENGTH = 100;
    private static final int MAX_ROUTE_DESCRIPTION_LENGTH = 10_000;
    private static final int MAX_COVER_URL_LENGTH = 512;
    private static final int MAX_STAGE_NAME_LENGTH = 100;
    private static final int MAX_STAGE_CONTENT_LENGTH = 200_000;
    private static final int MAX_COURSE_TITLE_LENGTH = 200;
    private static final int MAX_COURSE_SOURCE_LENGTH = 128;
    private static final int MAX_STAGE_PROBLEM_COUNT = 200;

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_OFFLINE = "offline";
    private static final Set<String> LEARNING_STATUSES = Set.of(STATUS_DRAFT, STATUS_PUBLISHED, STATUS_OFFLINE);

    private static final Pattern BILIBILI_VIDEO_URL = Pattern.compile(
            "^https?://(?:www\\.|m\\.)?bilibili\\.com/video/(BV[0-9A-Za-z]{10})(?:[/?#].*)?$");

    private static final String PERMISSION_ROUTE = "route:manage";
    private static final String PERMISSION_STAGE = "stage:manage";
    private static final String PERMISSION_COURSE = "course:manage";

    private final LearningRouteMapper learningRouteMapper;
    private final LearningStageMapper learningStageMapper;
    private final StageCourseMapper stageCourseMapper;
    private final StageProblemMapper stageProblemMapper;
    private final LearningProgressMapper learningProgressMapper;
    private final AdminProblemQueryService adminProblemQueryService;
    private final OperationLogService operationLogService;

    public AdminLearningService(LearningRouteMapper learningRouteMapper,
                                LearningStageMapper learningStageMapper,
                                StageCourseMapper stageCourseMapper,
                                StageProblemMapper stageProblemMapper,
                                LearningProgressMapper learningProgressMapper,
                                AdminProblemQueryService adminProblemQueryService,
                                OperationLogService operationLogService) {
        this.learningRouteMapper = learningRouteMapper;
        this.learningStageMapper = learningStageMapper;
        this.stageCourseMapper = stageCourseMapper;
        this.stageProblemMapper = stageProblemMapper;
        this.learningProgressMapper = learningProgressMapper;
        this.adminProblemQueryService = adminProblemQueryService;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public PageResult<AdminRouteView> listRoutes(AdminPrincipal actor,
                                                  String keyword,
                                                  String status,
                                                  int page,
                                                  int pageSize) {
        requirePermission(actor, PERMISSION_ROUTE);
        String normalizedKeyword = normalizeFilterText(keyword);
        String normalizedStatus = normalizeFilterStatus(status);
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<AdminRouteRow> result = new Page<>(safePage, safePageSize);
        learningRouteMapper.selectAdminRoutePage(result, normalizedKeyword, normalizedStatus);
        List<AdminRouteView> items = result.getRecords().stream()
                .map(this::toRouteView)
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional
    public AdminRouteView createRoute(AdminPrincipal actor,
                                      RouteCreateCommand command,
                                      LoginMetadata metadata) {
        requirePermission(actor, PERMISSION_ROUTE);
        String name = requireName(command.name(), MAX_ROUTE_NAME_LENGTH, "路线名称");
        String description = normalizeOptionalText(command.description(),
                MAX_ROUTE_DESCRIPTION_LENGTH, "路线简介");
        String coverUrl = normalizeCoverUrl(command.coverUrl());
        int sortOrder = command.sortOrder() == null ? 0 : command.sortOrder();

        LearningRoute route = new LearningRoute();
        route.setName(name);
        route.setDescription(description);
        route.setCoverUrl(coverUrl);
        route.setSortOrder(sortOrder);
        route.setStatus(STATUS_DRAFT);
        learningRouteMapper.insert(route);

        operationLogService.record(actor.id(), "create", "route", route.getId(),
                "新增学习路线《" + name + "》",
                Map.of("name", name, "status", STATUS_DRAFT), metadata);
        return toRouteView(requireRouteRow(route.getId()));
    }

    @Transactional(readOnly = true)
    public AdminRouteDetail getRoute(AdminPrincipal actor, Long routeId) {
        requirePermission(actor, PERMISSION_ROUTE);
        AdminRouteRow row = requireRouteRow(routeId);
        List<AdminStageRow> stages = learningStageMapper.selectAdminStageRowsByRouteId(routeId);
        return toRouteDetail(row, stages);
    }

    @Transactional
    public AdminRouteView updateRoute(AdminPrincipal actor,
                                      Long routeId,
                                      RouteUpdateCommand command,
                                      LoginMetadata metadata) {
        requirePermission(actor, PERMISSION_ROUTE);
        requireRouteEntity(routeId);
        if (!command.namePresent() && !command.descriptionPresent()
                && !command.coverUrlPresent() && !command.sortOrderPresent()) {
            throw validation("至少提交一个可修改字段");
        }

        var update = Wrappers.<LearningRoute>lambdaUpdate()
                .eq(LearningRoute::getId, routeId)
                .isNull(LearningRoute::getDeletedAt);
        List<String> changedFields = new ArrayList<>();
        if (command.namePresent()) {
            update.set(LearningRoute::getName,
                    requireName(command.name(), MAX_ROUTE_NAME_LENGTH, "路线名称"));
            changedFields.add("name");
        }
        if (command.descriptionPresent()) {
            update.set(LearningRoute::getDescription,
                    normalizeOptionalText(command.description(), MAX_ROUTE_DESCRIPTION_LENGTH, "路线简介"));
            changedFields.add("description");
        }
        if (command.coverUrlPresent()) {
            update.set(LearningRoute::getCoverUrl, normalizeCoverUrl(command.coverUrl()));
            changedFields.add("coverUrl");
        }
        if (command.sortOrderPresent()) {
            update.set(LearningRoute::getSortOrder,
                    requireSortOrder(command.sortOrder(), "排序值"));
            changedFields.add("sortOrder");
        }
        learningRouteMapper.update(null, update);

        operationLogService.record(actor.id(), "update", "route", routeId,
                "修改学习路线",
                Map.of("fields", changedFields), metadata);
        return toRouteView(requireRouteRow(routeId));
    }

    @Transactional
    public void deleteRoute(AdminPrincipal actor, Long routeId, LoginMetadata metadata) {
        requirePermission(actor, PERMISSION_ROUTE);
        LearningRoute route = requireRouteEntity(routeId);
        long stageCount = learningStageMapper.selectCount(Wrappers.<LearningStage>lambdaQuery()
                .eq(LearningStage::getRouteId, routeId)
                .isNull(LearningStage::getDeletedAt));
        long progressCount = learningProgressMapper.countByRouteId(routeId);
        if (stageCount > 0 || progressCount > 0) {
            throw new BusinessException(ErrorCode.LEARNING_CONTENT_HAS_REFERENCES);
        }

        int updated = learningRouteMapper.update(null, Wrappers.<LearningRoute>lambdaUpdate()
                .eq(LearningRoute::getId, routeId)
                .isNull(LearningRoute::getDeletedAt)
                .set(LearningRoute::getDeletedAt, now())
                .set(LearningRoute::getStatus, STATUS_OFFLINE));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.LEARNING_ROUTE_NOT_FOUND);
        }
        operationLogService.record(actor.id(), "delete", "route", routeId,
                "删除学习路线《" + route.getName() + "》", Map.of(), metadata);
    }

    @Transactional
    public AdminRouteView publishRoute(AdminPrincipal actor, Long routeId, LoginMetadata metadata) {
        return changeRouteStatus(actor, routeId, STATUS_PUBLISHED, "publish", "发布", metadata);
    }

    @Transactional
    public AdminRouteView offlineRoute(AdminPrincipal actor, Long routeId, LoginMetadata metadata) {
        return changeRouteStatus(actor, routeId, STATUS_OFFLINE, "offline", "下架", metadata);
    }

    private AdminRouteView changeRouteStatus(AdminPrincipal actor,
                                             Long routeId,
                                             String targetStatus,
                                             String action,
                                             String actionLabel,
                                             LoginMetadata metadata) {
        requirePermission(actor, PERMISSION_ROUTE);
        LearningRoute route = requireRouteEntity(routeId);
        if (!targetStatus.equals(route.getStatus())) {
            learningRouteMapper.update(null, Wrappers.<LearningRoute>lambdaUpdate()
                    .eq(LearningRoute::getId, routeId)
                    .isNull(LearningRoute::getDeletedAt)
                    .set(LearningRoute::getStatus, targetStatus));
            operationLogService.record(actor.id(), action, "route", routeId,
                    actionLabel + "学习路线《" + route.getName() + "》",
                    Map.of("status", targetStatus), metadata);
        }
        return toRouteView(requireRouteRow(routeId));
    }

    @Transactional(readOnly = true)
    public PageResult<AdminStageSummary> listStages(AdminPrincipal actor,
                                                     Long routeId,
                                                     String keyword,
                                                     String status,
                                                     int page,
                                                     int pageSize) {
        requirePermission(actor, PERMISSION_STAGE);
        if (routeId != null && routeId <= 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "路线 ID 非法");
        }
        String normalizedKeyword = normalizeFilterText(keyword);
        String normalizedStatus = normalizeFilterStatus(status);
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<AdminStageRow> result = new Page<>(safePage, safePageSize);
        learningStageMapper.selectAdminStagePage(result, routeId, normalizedKeyword, normalizedStatus);
        List<AdminStageSummary> items = result.getRecords().stream()
                .map(this::toStageSummary)
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional
    public AdminStageDetail createStage(AdminPrincipal actor,
                                        StageCreateCommand command,
                                        LoginMetadata metadata) {
        requirePermission(actor, PERMISSION_STAGE);
        if (command.routeId() == null || command.routeId() <= 0) {
            throw validation("路线 ID 不能为空");
        }
        LearningRoute route = requireRouteEntity(command.routeId());
        String name = requireName(command.name(), MAX_STAGE_NAME_LENGTH, "阶段名称");
        String content = normalizeOptionalText(command.content(), MAX_STAGE_CONTENT_LENGTH, "阶段内容");
        int sortOrder = command.sortOrder() == null ? 0 : command.sortOrder();

        LearningStage stage = new LearningStage();
        stage.setRouteId(route.getId());
        stage.setName(name);
        stage.setContent(content);
        stage.setSortOrder(sortOrder);
        stage.setStatus(STATUS_DRAFT);
        learningStageMapper.insert(stage);

        operationLogService.record(actor.id(), "create", "stage", stage.getId(),
                "在路线《" + route.getName() + "》下新增阶段《" + name + "》",
                Map.of("routeId", String.valueOf(route.getId()), "name", name), metadata);
        return buildStageDetail(stage.getId());
    }

    @Transactional(readOnly = true)
    public AdminStageDetail getStage(AdminPrincipal actor, Long stageId) {
        requirePermission(actor, PERMISSION_STAGE);
        return buildStageDetail(stageId);
    }

    @Transactional
    public AdminStageDetail updateStage(AdminPrincipal actor,
                                        Long stageId,
                                        StageUpdateCommand command,
                                        LoginMetadata metadata) {
        requirePermission(actor, PERMISSION_STAGE);
        LearningStage stage = requireStageEntity(stageId);
        if (!command.namePresent() && !command.contentPresent() && !command.sortOrderPresent()) {
            throw validation("至少提交一个可修改字段");
        }

        var update = Wrappers.<LearningStage>lambdaUpdate()
                .eq(LearningStage::getId, stageId)
                .isNull(LearningStage::getDeletedAt);
        List<String> changedFields = new ArrayList<>();
        if (command.namePresent()) {
            update.set(LearningStage::getName,
                    requireName(command.name(), MAX_STAGE_NAME_LENGTH, "阶段名称"));
            changedFields.add("name");
        }
        if (command.contentPresent()) {
            update.set(LearningStage::getContent,
                    normalizeOptionalText(command.content(), MAX_STAGE_CONTENT_LENGTH, "阶段内容"));
            changedFields.add("content");
        }
        if (command.sortOrderPresent()) {
            update.set(LearningStage::getSortOrder,
                    requireSortOrder(command.sortOrder(), "排序值"));
            changedFields.add("sortOrder");
        }
        learningStageMapper.update(null, update);

        operationLogService.record(actor.id(), "update", "stage", stageId,
                "修改阶段《" + stage.getName() + "》",
                Map.of("fields", changedFields), metadata);
        return buildStageDetail(stageId);
    }

    @Transactional
    public void deleteStage(AdminPrincipal actor, Long stageId, LoginMetadata metadata) {
        requirePermission(actor, PERMISSION_STAGE);
        LearningStage stage = requireStageEntity(stageId);
        long courseCount = stageCourseMapper.selectCount(Wrappers.<StageCourse>lambdaQuery()
                .eq(StageCourse::getStageId, stageId)
                .isNull(StageCourse::getDeletedAt));
        long problemCount = stageProblemMapper.selectCount(Wrappers.<StageProblem>lambdaQuery()
                .eq(StageProblem::getStageId, stageId));
        long progressCount = learningProgressMapper.countByStageId(stageId);
        if (courseCount > 0 || problemCount > 0 || progressCount > 0) {
            throw new BusinessException(ErrorCode.LEARNING_CONTENT_HAS_REFERENCES);
        }

        int updated = learningStageMapper.update(null, Wrappers.<LearningStage>lambdaUpdate()
                .eq(LearningStage::getId, stageId)
                .isNull(LearningStage::getDeletedAt)
                .set(LearningStage::getDeletedAt, now())
                .set(LearningStage::getStatus, STATUS_OFFLINE));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.LEARNING_STAGE_NOT_FOUND);
        }
        operationLogService.record(actor.id(), "delete", "stage", stageId,
                "删除阶段《" + stage.getName() + "》", Map.of(), metadata);
    }

    @Transactional
    public AdminStageDetail publishStage(AdminPrincipal actor, Long stageId, LoginMetadata metadata) {
        return changeStageStatus(actor, stageId, STATUS_PUBLISHED, "publish", "发布", metadata);
    }

    @Transactional
    public AdminStageDetail offlineStage(AdminPrincipal actor, Long stageId, LoginMetadata metadata) {
        return changeStageStatus(actor, stageId, STATUS_OFFLINE, "offline", "下架", metadata);
    }

    private AdminStageDetail changeStageStatus(AdminPrincipal actor,
                                               Long stageId,
                                               String targetStatus,
                                               String action,
                                               String actionLabel,
                                               LoginMetadata metadata) {
        requirePermission(actor, PERMISSION_STAGE);
        LearningStage stage = requireStageEntity(stageId);
        if (!targetStatus.equals(stage.getStatus())) {
            learningStageMapper.update(null, Wrappers.<LearningStage>lambdaUpdate()
                    .eq(LearningStage::getId, stageId)
                    .isNull(LearningStage::getDeletedAt)
                    .set(LearningStage::getStatus, targetStatus));
            operationLogService.record(actor.id(), action, "stage", stageId,
                    actionLabel + "阶段《" + stage.getName() + "》",
                    Map.of("status", targetStatus), metadata);
        }
        return buildStageDetail(stageId);
    }

    /**
     * 整体替换阶段题目。重复题目直接拒绝，未发布题目不允许关联。
     */
    @Transactional
    public List<AdminStageProblemView> replaceStageProblems(AdminPrincipal actor,
                                                            Long stageId,
                                                            List<StageProblemCommand> items,
                                                            LoginMetadata metadata) {
        requirePermission(actor, PERMISSION_STAGE);
        LearningStage stage = requireStageEntity(stageId);
        if (items == null) {
            throw validation("题目列表不能为空");
        }
        if (items.size() > MAX_STAGE_PROBLEM_COUNT) {
            throw validation("单个阶段最多关联 " + MAX_STAGE_PROBLEM_COUNT + " 道题目");
        }

        LinkedHashSet<Long> problemIds = new LinkedHashSet<>();
        List<StageProblem> associations = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            StageProblemCommand item = items.get(index);
            if (item == null || item.problemId() == null || item.problemId() <= 0) {
                throw validation("题目 ID 非法");
            }
            if (!problemIds.add(item.problemId())) {
                throw new BusinessException(ErrorCode.STAGE_PROBLEM_DUPLICATE);
            }
            StageProblem association = new StageProblem();
            association.setStageId(stageId);
            association.setProblemId(item.problemId());
            association.setSortOrder(item.sortOrder() == null ? index : item.sortOrder());
            associations.add(association);
        }
        adminProblemQueryService.assertAllPublished(problemIds);

        stageProblemMapper.deleteByStageId(stageId);
        for (StageProblem association : associations) {
            stageProblemMapper.insert(association);
        }

        operationLogService.record(actor.id(), "update", "stage", stageId,
                "更新阶段《" + stage.getName() + "》的题目关联",
                Map.of("problemCount", associations.size()), metadata);
        return loadStageProblems(stageId);
    }

    @Transactional
    public AdminCourseView createCourse(AdminPrincipal actor,
                                        Long stageId,
                                        CourseCreateCommand command,
                                        LoginMetadata metadata) {
        requirePermission(actor, PERMISSION_COURSE);
        LearningStage stage = requireStageEntity(stageId);
        String title = requireName(command.title(), MAX_COURSE_TITLE_LENGTH, "课程标题");
        String url = normalizeCourseUrl(command.url());
        Integer durationSeconds = normalizeDurationSeconds(command.durationSeconds());
        String source = normalizeOptionalText(command.source(), MAX_COURSE_SOURCE_LENGTH, "课程来源");
        int sortOrder = command.sortOrder() == null ? 0 : command.sortOrder();
        String status = normalizeCourseStatus(command.status(), true);

        StageCourse course = new StageCourse();
        course.setStageId(stageId);
        course.setTitle(title);
        course.setUrl(url);
        course.setDurationSeconds(durationSeconds);
        course.setSource(source);
        course.setSortOrder(sortOrder);
        course.setStatus(status);
        stageCourseMapper.insert(course);

        operationLogService.record(actor.id(), "create", "course", course.getId(),
                "为阶段《" + stage.getName() + "》新增课程《" + title + "》",
                Map.of("stageId", String.valueOf(stageId), "title", title), metadata);
        return toCourseView(requireCourseEntity(course.getId()));
    }

    @Transactional
    public AdminCourseView updateCourse(AdminPrincipal actor,
                                        Long courseId,
                                        CourseUpdateCommand command,
                                        LoginMetadata metadata) {
        requirePermission(actor, PERMISSION_COURSE);
        StageCourse course = requireCourseEntity(courseId);
        requireStageEntity(course.getStageId());
        if (!command.titlePresent() && !command.urlPresent() && !command.durationSecondsPresent()
                && !command.sourcePresent() && !command.sortOrderPresent() && !command.statusPresent()) {
            throw validation("至少提交一个可修改字段");
        }

        var update = Wrappers.<StageCourse>lambdaUpdate()
                .eq(StageCourse::getId, courseId)
                .isNull(StageCourse::getDeletedAt);
        List<String> changedFields = new ArrayList<>();
        if (command.titlePresent()) {
            update.set(StageCourse::getTitle,
                    requireName(command.title(), MAX_COURSE_TITLE_LENGTH, "课程标题"));
            changedFields.add("title");
        }
        if (command.urlPresent()) {
            update.set(StageCourse::getUrl, normalizeCourseUrl(command.url()));
            changedFields.add("url");
        }
        if (command.durationSecondsPresent()) {
            update.set(StageCourse::getDurationSeconds,
                    normalizeDurationSeconds(command.durationSeconds()));
            changedFields.add("durationSeconds");
        }
        if (command.sourcePresent()) {
            update.set(StageCourse::getSource,
                    normalizeOptionalText(command.source(), MAX_COURSE_SOURCE_LENGTH, "课程来源"));
            changedFields.add("source");
        }
        if (command.sortOrderPresent()) {
            update.set(StageCourse::getSortOrder,
                    requireSortOrder(command.sortOrder(), "排序值"));
            changedFields.add("sortOrder");
        }
        if (command.statusPresent()) {
            update.set(StageCourse::getStatus,
                    normalizeCourseStatus(command.status(), false));
            changedFields.add("status");
        }
        stageCourseMapper.update(null, update);

        operationLogService.record(actor.id(), "update", "course", courseId,
                "修改课程《" + course.getTitle() + "》",
                Map.of("fields", changedFields), metadata);
        return toCourseView(requireCourseEntity(courseId));
    }

    @Transactional
    public void deleteCourse(AdminPrincipal actor, Long courseId, LoginMetadata metadata) {
        requirePermission(actor, PERMISSION_COURSE);
        StageCourse course = requireCourseEntity(courseId);
        int updated = stageCourseMapper.update(null, Wrappers.<StageCourse>lambdaUpdate()
                .eq(StageCourse::getId, courseId)
                .isNull(StageCourse::getDeletedAt)
                .set(StageCourse::getDeletedAt, now())
                .set(StageCourse::getStatus, STATUS_OFFLINE));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "课程不存在");
        }
        operationLogService.record(actor.id(), "delete", "course", courseId,
                "删除课程《" + course.getTitle() + "》",
                Map.of("stageId", String.valueOf(course.getStageId())), metadata);
    }

    private AdminStageDetail buildStageDetail(Long stageId) {
        LearningStage stage = requireStageEntity(stageId);
        AdminStageRow row = learningStageMapper.selectAdminStageRow(stageId);
        if (row == null) {
            throw new BusinessException(ErrorCode.LEARNING_STAGE_NOT_FOUND);
        }
        List<StageCourse> courses = stageCourseMapper.selectList(Wrappers.<StageCourse>lambdaQuery()
                .eq(StageCourse::getStageId, stageId)
                .isNull(StageCourse::getDeletedAt)
                .orderByAsc(StageCourse::getSortOrder)
                .orderByAsc(StageCourse::getId));
        return new AdminStageDetail(
                String.valueOf(row.getId()),
                String.valueOf(row.getRouteId()),
                row.getRouteName(),
                stage.getName(),
                stage.getContent(),
                row.getSortOrder(),
                row.getStatus(),
                courses.stream().map(this::toCourseView).toList(),
                loadStageProblems(stageId),
                TimeUtils.toInstant(row.getCreatedAt()),
                TimeUtils.toInstant(row.getUpdatedAt()));
    }

    private List<AdminStageProblemView> loadStageProblems(Long stageId) {
        List<AdminStageProblemRow> rows = stageProblemMapper.selectAdminStageProblemRows(stageId);
        List<AdminStageProblemView> items = new ArrayList<>(rows.size());
        for (AdminStageProblemRow row : rows) {
            AdminProblemBrief problem = new AdminProblemBrief(
                    String.valueOf(row.getProblemId()),
                    row.getType(),
                    row.getObjectiveType(),
                    row.getTitle(),
                    row.getDifficulty(),
                    row.getSource(),
                    row.getPlatform(),
                    row.getStatus());
            items.add(new AdminStageProblemView(problem, row.getSortOrder() == null ? 0 : row.getSortOrder()));
        }
        return items;
    }

    private AdminRouteDetail toRouteDetail(AdminRouteRow row, List<AdminStageRow> stages) {
        List<AdminStageSummary> summaries = stages.stream()
                .map(this::toStageSummary)
                .toList();
        return new AdminRouteDetail(
                String.valueOf(row.getId()),
                row.getName(),
                row.getDescription(),
                row.getCoverUrl(),
                row.getSortOrder(),
                row.getStatus(),
                summaries.size(),
                TimeUtils.toInstant(row.getCreatedAt()),
                TimeUtils.toInstant(row.getUpdatedAt()),
                summaries);
    }

    private AdminRouteView toRouteView(AdminRouteRow row) {
        return new AdminRouteView(
                String.valueOf(row.getId()),
                row.getName(),
                row.getDescription(),
                row.getCoverUrl(),
                row.getSortOrder(),
                row.getStatus(),
                row.getStageCount() == null ? 0L : row.getStageCount(),
                TimeUtils.toInstant(row.getCreatedAt()),
                TimeUtils.toInstant(row.getUpdatedAt()));
    }

    private AdminStageSummary toStageSummary(AdminStageRow row) {
        return new AdminStageSummary(
                String.valueOf(row.getId()),
                String.valueOf(row.getRouteId()),
                row.getRouteName(),
                row.getName(),
                row.getSortOrder(),
                row.getStatus(),
                row.getCourseCount() == null ? 0L : row.getCourseCount(),
                row.getProblemCount() == null ? 0L : row.getProblemCount(),
                TimeUtils.toInstant(row.getCreatedAt()),
                TimeUtils.toInstant(row.getUpdatedAt()));
    }

    private AdminCourseView toCourseView(StageCourse course) {
        return new AdminCourseView(
                String.valueOf(course.getId()),
                course.getTitle(),
                course.getUrl(),
                course.getDurationSeconds(),
                course.getSource(),
                course.getSortOrder(),
                course.getStatus());
    }

    private LearningRoute requireRouteEntity(Long routeId) {
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(ErrorCode.LEARNING_ROUTE_NOT_FOUND);
        }
        LearningRoute route = learningRouteMapper.selectById(routeId);
        if (route == null || route.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.LEARNING_ROUTE_NOT_FOUND);
        }
        return route;
    }

    private AdminRouteRow requireRouteRow(Long routeId) {
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(ErrorCode.LEARNING_ROUTE_NOT_FOUND);
        }
        AdminRouteRow row = learningRouteMapper.selectAdminRouteRow(routeId);
        if (row == null) {
            throw new BusinessException(ErrorCode.LEARNING_ROUTE_NOT_FOUND);
        }
        return row;
    }

    private LearningStage requireStageEntity(Long stageId) {
        if (stageId == null || stageId <= 0) {
            throw new BusinessException(ErrorCode.LEARNING_STAGE_NOT_FOUND);
        }
        LearningStage stage = learningStageMapper.selectById(stageId);
        if (stage == null || stage.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.LEARNING_STAGE_NOT_FOUND);
        }
        return stage;
    }

    private StageCourse requireCourseEntity(Long courseId) {
        if (courseId == null || courseId <= 0) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "课程不存在");
        }
        StageCourse course = stageCourseMapper.selectById(courseId);
        if (course == null || course.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "课程不存在");
        }
        return course;
    }

    private String requireName(String value, int maxLength, String fieldName) {
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

    private Integer requireSortOrder(Integer value, String fieldName) {
        if (value == null) {
            throw validation(fieldName + "不能为空");
        }
        return value;
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

    private String normalizeCoverUrl(String value) {
        String url = normalizeOptionalText(value, MAX_COVER_URL_LENGTH, "封面地址");
        if (url == null) {
            return null;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null
                    || uri.getHost().isBlank()) {
                throw validation("封面地址必须是 http 或 https 链接");
            }
        } catch (URISyntaxException exception) {
            throw validation("封面地址格式非法");
        }
        return url;
    }

    private String normalizeCourseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COURSE_URL_INVALID);
        }
        Matcher matcher = BILIBILI_VIDEO_URL.matcher(value.trim());
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCode.COURSE_URL_INVALID);
        }
        return "https://www.bilibili.com/video/" + matcher.group(1);
    }

    private Integer normalizeDurationSeconds(Integer value) {
        if (value != null && value < 0) {
            throw validation("课程时长不能为负数");
        }
        return value;
    }

    private String normalizeCourseStatus(String value, boolean useDefaultWhenMissing) {
        if (value == null || value.isBlank()) {
            if (useDefaultWhenMissing) {
                return STATUS_PUBLISHED;
            }
            throw validation("课程状态不能为空");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!LEARNING_STATUSES.contains(normalized)) {
            throw validation("课程状态取值非法");
        }
        return normalized;
    }

    private String normalizeFilterStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!LEARNING_STATUSES.contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "状态取值非法");
        }
        return normalized;
    }

    private String normalizeFilterText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_FILTER_LENGTH) {
            throw validation("关键词不能超过 " + MAX_FILTER_LENGTH + " 个字符");
        }
        return trimmed;
    }

    private void requirePermission(AdminPrincipal actor, String permission) {
        if (actor == null || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "无权限操作学习内容");
        }
    }

    private BusinessException validation(String message) {
        return new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, message);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
