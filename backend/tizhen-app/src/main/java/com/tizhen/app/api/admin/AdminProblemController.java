package com.tizhen.app.api.admin;

import com.tizhen.app.api.admin.dto.CreateProblemRequest;
import com.tizhen.app.api.admin.dto.ReplaceTestDataRequest;
import com.tizhen.app.api.admin.dto.TestDataListView;
import com.tizhen.app.api.admin.dto.UpdateProblemRequest;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.problem.model.AdminProblemDetail;
import com.tizhen.core.problem.model.AdminProblemSummary;
import com.tizhen.core.problem.model.TestDataView;
import com.tizhen.core.problem.service.AdminProblemQueryService;
import com.tizhen.core.problem.service.AdminProblemService;
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

/**
 * M04 管理端题库接口。接口契约见 docs/design/04.API接口设计.md §6.4.2。
 */
@RestController
@RequestMapping("/api/v1/admin/problems")
public class AdminProblemController {

    private final AdminProblemQueryService adminProblemQueryService;
    private final AdminProblemService adminProblemService;

    public AdminProblemController(AdminProblemQueryService adminProblemQueryService,
                                  AdminProblemService adminProblemService) {
        this.adminProblemQueryService = adminProblemQueryService;
        this.adminProblemService = adminProblemService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminProblemSummary>> list(
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String objectiveType,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(adminProblemQueryService.list(
                principal, type, objectiveType, difficulty, status, source, tag, keyword, page, pageSize));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminProblemDetail>> create(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody CreateProblemRequest body,
            HttpServletRequest request) {
        AdminProblemDetail detail = adminProblemService.create(
                principal, body.toCommand(), AdminRequestMetadata.of(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(detail));
    }

    @GetMapping("/{problemId}")
    public ApiResponse<AdminProblemDetail> detail(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long problemId) {
        return ApiResponse.ok(adminProblemService.detail(principal, problemId));
    }

    @PatchMapping("/{problemId}")
    public ApiResponse<AdminProblemDetail> update(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long problemId,
            @Valid @RequestBody UpdateProblemRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(adminProblemService.update(
                principal, problemId, body.toCommand(), AdminRequestMetadata.of(request)));
    }

    @DeleteMapping("/{problemId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long problemId,
            HttpServletRequest request) {
        adminProblemService.delete(principal, problemId, AdminRequestMetadata.of(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{problemId}/publish")
    public ApiResponse<AdminProblemDetail> publish(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long problemId,
            HttpServletRequest request) {
        return ApiResponse.ok(adminProblemService.publish(
                principal, problemId, AdminRequestMetadata.of(request)));
    }

    @PostMapping("/{problemId}/offline")
    public ApiResponse<AdminProblemDetail> offline(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long problemId,
            HttpServletRequest request) {
        return ApiResponse.ok(adminProblemService.offline(
                principal, problemId, AdminRequestMetadata.of(request)));
    }

    @GetMapping("/{problemId}/test-data")
    public ApiResponse<PageResult<TestDataView>> listTestData(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long problemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(adminProblemService.listTestData(principal, problemId, page, pageSize));
    }

    @PutMapping("/{problemId}/test-data")
    public ApiResponse<TestDataListView> replaceTestData(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long problemId,
            @Valid @RequestBody ReplaceTestDataRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(new TestDataListView(adminProblemService.replaceTestData(
                principal, problemId, body.toCommand(), AdminRequestMetadata.of(request))));
    }
}
