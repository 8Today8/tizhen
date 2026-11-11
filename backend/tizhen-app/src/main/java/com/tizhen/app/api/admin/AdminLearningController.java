package com.tizhen.app.api.admin;

import com.tizhen.app.api.admin.dto.CreateCourseRequest;
import com.tizhen.app.api.admin.dto.CreateRouteRequest;
import com.tizhen.app.api.admin.dto.CreateStageRequest;
import com.tizhen.app.api.admin.dto.ReplaceStageProblemsRequest;
import com.tizhen.app.api.admin.dto.StageProblemListView;
import com.tizhen.app.api.admin.dto.UpdateCourseRequest;
import com.tizhen.app.api.admin.dto.UpdateRouteRequest;
import com.tizhen.app.api.admin.dto.UpdateStageRequest;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.learning.model.AdminCourseView;
import com.tizhen.core.learning.model.AdminRouteDetail;
import com.tizhen.core.learning.model.AdminRouteView;
import com.tizhen.core.learning.model.AdminStageDetail;
import com.tizhen.core.learning.model.AdminStageSummary;
import com.tizhen.core.learning.model.CourseCreateCommand;
import com.tizhen.core.learning.model.RouteCreateCommand;
import com.tizhen.core.learning.model.StageCreateCommand;
import com.tizhen.core.learning.model.StageProblemCommand;
import com.tizhen.core.learning.service.AdminLearningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * M03 管理端学习内容接口。接口契约见 docs/design/04.API接口设计.md §6.3.2。
 */
@RestController
@RequestMapping("/api/v1/admin/learning")
public class AdminLearningController {

    private final AdminLearningService adminLearningService;

    public AdminLearningController(AdminLearningService adminLearningService) {
        this.adminLearningService = adminLearningService;
    }

    @GetMapping("/routes")
    public ApiResponse<PageResult<AdminRouteView>> listRoutes(
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(adminLearningService.listRoutes(principal, keyword, status, page, pageSize));
    }

    @PostMapping("/routes")
    public ResponseEntity<ApiResponse<AdminRouteView>> createRoute(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody CreateRouteRequest body,
            HttpServletRequest request) {
        AdminRouteView route = adminLearningService.createRoute(
                principal,
                new RouteCreateCommand(body.name(), body.description(), body.coverUrl(), body.sortOrder()),
                AdminRequestMetadata.of(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(route));
    }

    @GetMapping("/routes/{routeId}")
    public ApiResponse<AdminRouteDetail> routeDetail(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long routeId) {
        return ApiResponse.ok(adminLearningService.getRoute(principal, routeId));
    }

    @PatchMapping("/routes/{routeId}")
    public ApiResponse<AdminRouteView> updateRoute(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long routeId,
            @Valid @RequestBody UpdateRouteRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(adminLearningService.updateRoute(
                principal, routeId, body.toCommand(), AdminRequestMetadata.of(request)));
    }

    @DeleteMapping("/routes/{routeId}")
    public ResponseEntity<Void> deleteRoute(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long routeId,
            HttpServletRequest request) {
        adminLearningService.deleteRoute(principal, routeId, AdminRequestMetadata.of(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/routes/{routeId}/publish")
    public ApiResponse<AdminRouteView> publishRoute(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long routeId,
            HttpServletRequest request) {
        return ApiResponse.ok(adminLearningService.publishRoute(
                principal, routeId, AdminRequestMetadata.of(request)));
    }

    @PostMapping("/routes/{routeId}/offline")
    public ApiResponse<AdminRouteView> offlineRoute(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long routeId,
            HttpServletRequest request) {
        return ApiResponse.ok(adminLearningService.offlineRoute(
                principal, routeId, AdminRequestMetadata.of(request)));
    }

    @GetMapping("/stages")
    public ApiResponse<PageResult<AdminStageSummary>> listStages(
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(adminLearningService.listStages(
                principal, routeId, keyword, status, page, pageSize));
    }

    @PostMapping("/stages")
    public ResponseEntity<ApiResponse<AdminStageDetail>> createStage(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody CreateStageRequest body,
            HttpServletRequest request) {
        AdminStageDetail stage = adminLearningService.createStage(
                principal,
                new StageCreateCommand(body.routeId(), body.name(), body.content(), body.sortOrder()),
                AdminRequestMetadata.of(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(stage));
    }

    @GetMapping("/stages/{stageId}")
    public ApiResponse<AdminStageDetail> stageDetail(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long stageId) {
        return ApiResponse.ok(adminLearningService.getStage(principal, stageId));
    }

    @PatchMapping("/stages/{stageId}")
    public ApiResponse<AdminStageDetail> updateStage(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long stageId,
            @Valid @RequestBody UpdateStageRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(adminLearningService.updateStage(
                principal, stageId, body.toCommand(), AdminRequestMetadata.of(request)));
    }

    @DeleteMapping("/stages/{stageId}")
    public ResponseEntity<Void> deleteStage(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long stageId,
            HttpServletRequest request) {
        adminLearningService.deleteStage(principal, stageId, AdminRequestMetadata.of(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/stages/{stageId}/publish")
    public ApiResponse<AdminStageDetail> publishStage(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long stageId,
            HttpServletRequest request) {
        return ApiResponse.ok(adminLearningService.publishStage(
                principal, stageId, AdminRequestMetadata.of(request)));
    }

    @PostMapping("/stages/{stageId}/offline")
    public ApiResponse<AdminStageDetail> offlineStage(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long stageId,
            HttpServletRequest request) {
        return ApiResponse.ok(adminLearningService.offlineStage(
                principal, stageId, AdminRequestMetadata.of(request)));
    }

    @PutMapping("/stages/{stageId}/problems")
    public ApiResponse<StageProblemListView> replaceStageProblems(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long stageId,
            @Valid @RequestBody ReplaceStageProblemsRequest body,
            HttpServletRequest request) {
        List<StageProblemCommand> commands = body.items().stream()
                .map(item -> new StageProblemCommand(item.problemId(), item.sortOrder()))
                .toList();
        return ApiResponse.ok(new StageProblemListView(adminLearningService.replaceStageProblems(
                principal, stageId, commands, AdminRequestMetadata.of(request))));
    }

    @PostMapping("/stages/{stageId}/courses")
    public ResponseEntity<ApiResponse<AdminCourseView>> createCourse(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long stageId,
            @Valid @RequestBody CreateCourseRequest body,
            HttpServletRequest request) {
        AdminCourseView course = adminLearningService.createCourse(
                principal,
                stageId,
                new CourseCreateCommand(body.title(), body.url(), body.durationSeconds(),
                        body.source(), body.sortOrder(), body.status()),
                AdminRequestMetadata.of(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(course));
    }

    @PatchMapping("/courses/{courseId}")
    public ApiResponse<AdminCourseView> updateCourse(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(adminLearningService.updateCourse(
                principal, courseId, body.toCommand(), AdminRequestMetadata.of(request)));
    }

    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long courseId,
            HttpServletRequest request) {
        adminLearningService.deleteCourse(principal, courseId, AdminRequestMetadata.of(request));
        return ResponseEntity.noContent().build();
    }
}
