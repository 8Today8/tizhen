package com.tizhen.app.api.admin;

import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.admin.model.OperationLogView;
import com.tizhen.core.admin.service.OperationLogQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 操作日志只读接口。运营与超管均可查看。
 */
@RestController
@RequestMapping("/api/v1/admin/operation-logs")
public class AdminOperationLogController {

    private final OperationLogQueryService operationLogQueryService;

    public AdminOperationLogController(OperationLogQueryService operationLogQueryService) {
        this.operationLogQueryService = operationLogQueryService;
    }

    @GetMapping
    public ApiResponse<PageResult<OperationLogView>> list(
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(operationLogQueryService.list(
                adminId, action, targetType, targetId, from, to, page, pageSize));
    }

    @GetMapping("/{logId}")
    public ApiResponse<OperationLogView> detail(@PathVariable Long logId) {
        return ApiResponse.ok(operationLogQueryService.detail(logId));
    }
}
