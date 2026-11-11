package com.tizhen.app.api.problem;

import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.account.model.PlayerPrincipal;
import com.tizhen.core.judge.model.SubmissionDetailView;
import com.tizhen.core.judge.model.SubmissionListItem;
import com.tizhen.core.judge.service.SubmissionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选手本人提交历史与判题结果。接口契约见 docs/design/04.API接口设计.md §6.4 SUB-02、SUB-03。
 */
@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping
    public ApiResponse<PageResult<SubmissionListItem>> list(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) String problemType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(submissionService.list(principal.id(), problemId, problemType,
                status, page, pageSize));
    }

    @GetMapping("/{submissionId}")
    public ApiResponse<SubmissionDetailView> detail(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable Long submissionId) {
        return ApiResponse.ok(submissionService.getDetail(principal.id(), submissionId));
    }
}
