package com.tizhen.core.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.core.account.model.LoginMetadata;
import com.tizhen.core.admin.entity.OperationLog;
import com.tizhen.core.admin.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 后台操作日志写入。敏感信息不得进入 detail 或 extra。
 */
@Service
public class OperationLogService {

    private static final int IP_MAX_LENGTH = 45;
    private static final int USER_AGENT_MAX_LENGTH = 512;
    private static final int REQUEST_ID_MAX_LENGTH = 64;

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public OperationLogService(OperationLogMapper operationLogMapper, ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    public void recordAdminLogin(Long adminId, LoginMetadata metadata) {
        OperationLog log = new OperationLog();
        log.setAdminId(adminId);
        log.setAction("login");
        log.setTargetType("admin_user");
        log.setTargetId(null);
        log.setDetail("管理员登录后台");
        log.setIpAddress(truncate(metadata.ipAddress(), IP_MAX_LENGTH));
        log.setUserAgent(truncate(metadata.userAgent(), USER_AGENT_MAX_LENGTH));
        log.setRequestId(truncate(metadata.requestId(), REQUEST_ID_MAX_LENGTH));
        operationLogMapper.insert(log);
    }

    /**
     * 记录一次后台写操作。detail 与 extra 由调用方保证已完成脱敏。
     */
    public void record(Long adminId,
                       String action,
                       String targetType,
                       Long targetId,
                       String detail,
                       Map<String, Object> extra,
                       LoginMetadata metadata) {
        OperationLog log = new OperationLog();
        log.setAdminId(adminId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setExtra(writeExtra(extra));
        if (metadata != null) {
            log.setIpAddress(truncate(metadata.ipAddress(), IP_MAX_LENGTH));
            log.setUserAgent(truncate(metadata.userAgent(), USER_AGENT_MAX_LENGTH));
            log.setRequestId(truncate(metadata.requestId(), REQUEST_ID_MAX_LENGTH));
        }
        operationLogMapper.insert(log);
    }

    private String writeExtra(Map<String, Object> extra) {
        if (extra == null || extra.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(extra);
        } catch (JsonProcessingException exception) {
            // 附加信息失败不应阻断业务主流程，但保留可读详情供审计。
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
