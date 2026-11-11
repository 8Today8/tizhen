package com.tizhen.app.api.datagen;

import com.tizhen.app.api.datagen.dto.CreateDataGenTaskRequest;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.account.model.PlayerPrincipal;
import com.tizhen.core.datagen.model.DataGenOptions;
import com.tizhen.core.datagen.model.DataGenTaskDetail;
import com.tizhen.core.datagen.model.DataGenTaskListItem;
import com.tizhen.core.datagen.model.DownloadPayload;
import com.tizhen.core.datagen.service.DataGenService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * M08 选手造数据接口。任务异步执行，创建成功返回 202。
 */
@RestController
@RequestMapping("/api/v1/datagen")
public class DataGenController {

    private final DataGenService dataGenService;

    public DataGenController(DataGenService dataGenService) {
        this.dataGenService = dataGenService;
    }

    @GetMapping("/options")
    public ApiResponse<DataGenOptions> options(@AuthenticationPrincipal PlayerPrincipal principal) {
        return ApiResponse.ok(dataGenService.options(principal.id()));
    }

    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<DataGenTaskDetail>> create(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @Valid @RequestBody CreateDataGenTaskRequest request) {
        DataGenTaskDetail detail = dataGenService.create(principal.id(), request.toCommand());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(detail));
    }

    @GetMapping("/tasks")
    public ApiResponse<PageResult<DataGenTaskListItem>> list(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String generationMode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(dataGenService.list(
                principal.id(), status, generationMode, page, pageSize));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<DataGenTaskDetail> detail(@AuthenticationPrincipal PlayerPrincipal principal,
                                                 @PathVariable Long taskId) {
        return ApiResponse.ok(dataGenService.detail(principal.id(), taskId));
    }

    @GetMapping("/tasks/{taskId}/files/{fileId}/input")
    public ResponseEntity<Resource> downloadInput(@AuthenticationPrincipal PlayerPrincipal principal,
                                                  @PathVariable Long taskId,
                                                  @PathVariable Long fileId) {
        return download(dataGenService.downloadFile(principal.id(), taskId, fileId, true));
    }

    @GetMapping("/tasks/{taskId}/files/{fileId}/output")
    public ResponseEntity<Resource> downloadOutput(@AuthenticationPrincipal PlayerPrincipal principal,
                                                   @PathVariable Long taskId,
                                                   @PathVariable Long fileId) {
        return download(dataGenService.downloadFile(principal.id(), taskId, fileId, false));
    }

    @GetMapping("/tasks/{taskId}/files/archive")
    public ResponseEntity<Resource> downloadArchive(@AuthenticationPrincipal PlayerPrincipal principal,
                                                    @PathVariable Long taskId) {
        return download(dataGenService.downloadArchive(principal.id(), taskId));
    }

    private ResponseEntity<Resource> download(DownloadPayload payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(payload.filename(), StandardCharsets.UTF_8)
                .build());
        if (payload.contentLength() >= 0) {
            headers.setContentLength(payload.contentLength());
        }
        return ResponseEntity.ok().headers(headers).body(payload.resource());
    }
}
