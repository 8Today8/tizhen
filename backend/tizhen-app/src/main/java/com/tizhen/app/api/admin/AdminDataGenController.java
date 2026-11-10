package com.tizhen.app.api.admin;

import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.datagen.model.AdminDataGenTaskDetail;
import com.tizhen.core.datagen.model.AdminDataGenTaskListItem;
import com.tizhen.core.datagen.model.DownloadPayload;
import com.tizhen.core.datagen.service.AdminDataGenService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * M08 后台造数据任务接口。管理员可查看全平台任务并下载仍保留的文件。
 */
@RestController
@RequestMapping("/api/v1/admin/datagen/tasks")
public class AdminDataGenController {

    private final AdminDataGenService adminDataGenService;

    public AdminDataGenController(AdminDataGenService adminDataGenService) {
        this.adminDataGenService = adminDataGenService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminDataGenTaskListItem>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String generationMode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(adminDataGenService.list(userId, status, generationMode, page, pageSize));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<AdminDataGenTaskDetail> detail(@PathVariable Long taskId) {
        return ApiResponse.ok(adminDataGenService.detail(taskId));
    }

    @GetMapping("/{taskId}/files/{fileId}/input")
    public ResponseEntity<Resource> downloadInput(@PathVariable Long taskId,
                                                  @PathVariable Long fileId) {
        return download(adminDataGenService.downloadFile(taskId, fileId, true));
    }

    @GetMapping("/{taskId}/files/{fileId}/output")
    public ResponseEntity<Resource> downloadOutput(@PathVariable Long taskId,
                                                   @PathVariable Long fileId) {
        return download(adminDataGenService.downloadFile(taskId, fileId, false));
    }

    @GetMapping("/{taskId}/files/archive")
    public ResponseEntity<Resource> downloadArchive(@PathVariable Long taskId) {
        return download(adminDataGenService.downloadArchive(taskId));
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
