package com.tizhen.core.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.entity.AdminRole;
import com.tizhen.core.account.entity.AdminUser;
import com.tizhen.core.account.mapper.AdminRoleMapper;
import com.tizhen.core.account.mapper.AdminUserMapper;
import com.tizhen.core.admin.entity.OperationLog;
import com.tizhen.core.admin.mapper.OperationLogMapper;
import com.tizhen.core.admin.model.AdminLogUser;
import com.tizhen.core.admin.model.OperationLogView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 操作日志只读查询。日志不可修改或删除。
 */
@Service
public class OperationLogQueryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final OperationLogMapper operationLogMapper;
    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final ObjectMapper objectMapper;

    public OperationLogQueryService(
            OperationLogMapper operationLogMapper,
            AdminUserMapper adminUserMapper,
            AdminRoleMapper adminRoleMapper,
            ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.adminUserMapper = adminUserMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<OperationLogView> list(Long adminId,
                                             String action,
                                             String targetType,
                                             Long targetId,
                                             Instant from,
                                             Instant to,
                                             int page,
                                             int pageSize) {
        String normalizedAction = normalize(action, 64, "action");
        String normalizedTargetType = normalize(targetType, 32, "targetType");
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<OperationLog> result = new Page<>(safePage, safePageSize);
        operationLogMapper.selectPage(result, Wrappers.<OperationLog>lambdaQuery()
                .eq(adminId != null, OperationLog::getAdminId, adminId)
                .eq(normalizedAction != null, OperationLog::getAction, normalizedAction)
                .eq(normalizedTargetType != null, OperationLog::getTargetType, normalizedTargetType)
                .eq(targetId != null, OperationLog::getTargetId, targetId)
                .ge(from != null, OperationLog::getCreatedAt, TimeUtils.toUtcLocalDateTime(from))
                .lt(to != null, OperationLog::getCreatedAt, TimeUtils.toUtcLocalDateTime(to))
                .orderByDesc(OperationLog::getCreatedAt)
                .orderByDesc(OperationLog::getId));

        List<Long> adminIds = result.getRecords().stream()
                .map(OperationLog::getAdminId)
                .distinct()
                .toList();
        Map<Long, AdminUser> admins = adminIds.isEmpty()
                ? Map.of()
                : adminUserMapper.selectBatchIds(adminIds).stream()
                        .collect(Collectors.toMap(AdminUser::getId, Function.identity()));
        Map<Long, AdminRole> roles = adminRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(AdminRole::getId, Function.identity()));

        List<OperationLogView> items = result.getRecords().stream()
                .map(log -> toView(log, admins.get(log.getAdminId()), roles))
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional(readOnly = true)
    public OperationLogView detail(Long logId) {
        OperationLog log = operationLogMapper.selectById(logId);
        if (log == null) {
            throw new BusinessException(ErrorCode.OPERATION_LOG_NOT_FOUND);
        }
        AdminUser admin = adminUserMapper.selectById(log.getAdminId());
        Map<Long, AdminRole> roles = adminRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(AdminRole::getId, Function.identity()));
        return toView(log, admin, roles);
    }

    private OperationLogView toView(OperationLog log, AdminUser admin, Map<Long, AdminRole> roles) {
        AdminLogUser adminLogUser;
        if (admin == null) {
            adminLogUser = new AdminLogUser(String.valueOf(log.getAdminId()), null, null, null);
        } else {
            AdminRole role = roles.get(admin.getRoleId());
            adminLogUser = new AdminLogUser(
                    String.valueOf(admin.getId()),
                    admin.getUsername(),
                    admin.getDisplayName(),
                    role == null ? null : role.getCode());
        }
        return new OperationLogView(
                String.valueOf(log.getId()),
                adminLogUser,
                log.getAction(),
                log.getTargetType(),
                log.getTargetId() == null ? null : String.valueOf(log.getTargetId()),
                log.getDetail(),
                parseExtra(log.getExtra()),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getRequestId(),
                TimeUtils.toInstant(log.getCreatedAt()));
    }

    private Object parseExtra(String extra) {
        if (extra == null || extra.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(extra, Object.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String normalize(String value, int maxLength, String fieldName) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, fieldName + " 长度不能超过 " + maxLength);
        }
        return trimmed;
    }
}
