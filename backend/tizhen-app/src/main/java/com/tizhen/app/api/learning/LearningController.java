package com.tizhen.app.api.learning;

import com.tizhen.app.api.learning.dto.ProgressUpdateRequest;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.account.model.PlayerPrincipal;
import com.tizhen.core.learning.model.ExternalCompletionView;
import com.tizhen.core.learning.model.RouteDetail;
import com.tizhen.core.learning.model.RouteSummary;
import com.tizhen.core.learning.model.StageDetail;
import com.tizhen.core.learning.model.StageProgressView;
import com.tizhen.core.learning.service.LearningService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选手端学习路线。接口契约见 docs/design/04.API接口设计.md §6.2 LRN-01～LRN-05。
 */
@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {

    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @GetMapping("/routes")
    public ApiResponse<PageResult<RouteSummary>> listRoutes(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(learningService.listRoutes(principal.id(), page, pageSize));
    }

    @GetMapping("/routes/{routeId}")
    public ApiResponse<RouteDetail> getRoute(@AuthenticationPrincipal PlayerPrincipal principal,
                                             @PathVariable Long routeId) {
        return ApiResponse.ok(learningService.getRoute(principal.id(), routeId));
    }

    @GetMapping("/stages/{stageId}")
    public ApiResponse<StageDetail> getStage(@AuthenticationPrincipal PlayerPrincipal principal,
                                             @PathVariable Long stageId) {
        return ApiResponse.ok(learningService.getStage(principal.id(), stageId));
    }

    @PutMapping("/stages/{stageId}/progress")
    public ApiResponse<StageProgressView> updateStageProgress(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable Long stageId,
            @Valid @RequestBody ProgressUpdateRequest request) {
        return ApiResponse.ok(learningService.updateStageProgress(
                principal.id(), stageId, Boolean.TRUE.equals(request.getCompleted())));
    }

    @PutMapping("/external-problems/{problemId}/completion")
    public ApiResponse<ExternalCompletionView> updateExternalCompletion(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable Long problemId,
            @Valid @RequestBody ProgressUpdateRequest request) {
        return ApiResponse.ok(learningService.updateExternalCompletion(
                principal.id(), problemId, Boolean.TRUE.equals(request.getCompleted())));
    }
}
