package com.tizhen.app.api.problem;

import com.tizhen.app.api.problem.dto.ProblemTagListView;
import com.tizhen.app.api.problem.dto.SubmissionCreateRequest;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.account.model.PlayerPrincipal;
import com.tizhen.core.judge.model.SubmissionCreateCommand;
import com.tizhen.core.judge.model.SubmissionDetailView;
import com.tizhen.core.judge.service.SubmissionService;
import com.tizhen.core.problem.model.ClientProblemDetail;
import com.tizhen.core.problem.model.ClientProblemSummary;
import com.tizhen.core.problem.service.ClientProblemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选手端题库。接口契约见 docs/design/04.API接口设计.md §6.4 PRB-01～PRB-04、SUB-01。
 */
@RestController
@RequestMapping("/api/v1/problems")
public class ProblemController {

    private final ClientProblemService clientProblemService;
    private final SubmissionService submissionService;

    public ProblemController(ClientProblemService clientProblemService,
                             SubmissionService submissionService) {
        this.clientProblemService = clientProblemService;
        this.submissionService = submissionService;
    }

    @GetMapping
    public ApiResponse<PageResult<ClientProblemSummary>> list(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String objectiveType,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(clientProblemService.list(principal.id(), type, objectiveType,
                difficulty, source, tag, keyword, page, pageSize));
    }

    @GetMapping("/tags")
    public ApiResponse<ProblemTagListView> tags() {
        return ApiResponse.ok(new ProblemTagListView(clientProblemService.listTags()));
    }

    @GetMapping("/random")
    public ApiResponse<ClientProblemSummary> random(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String tag) {
        return ApiResponse.ok(clientProblemService.random(principal.id(), type, difficulty, tag));
    }

    @GetMapping("/{problemId}")
    public ApiResponse<ClientProblemDetail> detail(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable Long problemId) {
        return ApiResponse.ok(clientProblemService.detail(principal.id(), problemId));
    }

    @PostMapping("/{problemId}/submissions")
    public ResponseEntity<ApiResponse<SubmissionDetailView>> submit(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable Long problemId,
            @RequestBody SubmissionCreateRequest request) {
        SubmissionDetailView detail = submissionService.create(principal.id(), problemId,
                new SubmissionCreateCommand(
                        request.language(),
                        request.code(),
                        request.selectedOptions(),
                        request.answerText()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(detail));
    }
}
