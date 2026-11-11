package com.tizhen.core.record.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.judge.entity.WrongBook;
import com.tizhen.core.judge.mapper.WrongBookMapper;
import com.tizhen.core.record.mapper.WrongBookRecordMapper;
import com.tizhen.core.record.model.WrongBookItem;
import com.tizhen.core.record.model.WrongBookProblemBrief;
import com.tizhen.core.record.model.WrongBookRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 错题本查询与手动移除。移除只写 removedAt，再次出错由判题链路自动恢复。
 */
@Service
public class WrongBookService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final Set<String> PROBLEM_TYPES =
            Set.of("oj", "objective", "short_answer");

    private final WrongBookRecordMapper wrongBookRecordMapper;
    private final WrongBookMapper wrongBookMapper;

    public WrongBookService(WrongBookRecordMapper wrongBookRecordMapper,
                            WrongBookMapper wrongBookMapper) {
        this.wrongBookRecordMapper = wrongBookRecordMapper;
        this.wrongBookMapper = wrongBookMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<WrongBookItem> list(Long userId,
                                          String problemType,
                                          String keyword,
                                          int page,
                                          int pageSize) {
        String normalizedType = normalizeProblemType(problemType);
        String normalizedKeyword = normalizeKeyword(keyword);
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<WrongBookRow> result = new Page<>(safePage, safePageSize);
        wrongBookRecordMapper.selectUserWrongBookPage(
                result, userId, normalizedType, normalizedKeyword);

        List<WrongBookItem> items = result.getRecords().stream()
                .map(this::toItem)
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    /**
     * 移除自己的错题；已移除时保持幂等，不暴露其他用户的记录。
     */
    @Transactional
    public void remove(Long userId, Long wrongBookId) {
        WrongBook record = wrongBookMapper.selectById(wrongBookId);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "错题记录不存在");
        }
        if (record.getRemovedAt() != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        wrongBookMapper.update(null, Wrappers.<WrongBook>lambdaUpdate()
                .eq(WrongBook::getId, wrongBookId)
                .eq(WrongBook::getUserId, userId)
                .isNull(WrongBook::getRemovedAt)
                .set(WrongBook::getRemovedAt, now)
                .set(WrongBook::getUpdatedAt, now));
    }

    private WrongBookItem toItem(WrongBookRow row) {
        return new WrongBookItem(
                String.valueOf(row.getId()),
                new WrongBookProblemBrief(
                        String.valueOf(row.getProblemId()),
                        row.getProblemType(),
                        row.getTitle(),
                        row.getDifficulty()),
                row.getLatestError(),
                row.getLatestScore(),
                row.getErrorCount() == null ? 0 : row.getErrorCount(),
                TimeUtils.toInstant(row.getLastErrorAt()));
    }

    private String normalizeProblemType(String problemType) {
        if (problemType == null || problemType.isBlank()) {
            return null;
        }
        String normalized = problemType.trim().toLowerCase(Locale.ROOT);
        if (!PROBLEM_TYPES.contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM,
                    "problemType 仅支持 oj、objective 或 short_answer");
        }
        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED,
                    "keyword 不能超过 " + MAX_KEYWORD_LENGTH + " 个字符");
        }
        return normalized;
    }
}
