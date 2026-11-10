package com.tizhen.core.leaderboard.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.common.util.WeekUtils;
import com.tizhen.core.leaderboard.mapper.LeaderboardMapper;
import com.tizhen.core.leaderboard.model.AdminLeaderboardPage;
import com.tizhen.core.leaderboard.model.AdminLeaderboardUser;
import com.tizhen.core.leaderboard.model.LeaderboardItem;
import com.tizhen.core.leaderboard.model.LeaderboardRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * 后台榜单查询。周榜与总榜实时派生，只读，不提供改分或补录。
 */
@Service
public class AdminLeaderboardService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final LeaderboardMapper leaderboardMapper;

    public AdminLeaderboardService(LeaderboardMapper leaderboardMapper) {
        this.leaderboardMapper = leaderboardMapper;
    }

    @Transactional(readOnly = true)
    public AdminLeaderboardPage getLeaderboard(String period, int page, int pageSize) {
        String normalizedPeriod = period == null || period.isBlank()
                ? "week"
                : period.trim().toLowerCase(Locale.ROOT);
        if (!"week".equals(normalizedPeriod) && !"all".equals(normalizedPeriod)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "period 仅支持 week 或 all");
        }
        boolean week = "week".equals(normalizedPeriod);
        LocalDate weekStartDate = week ? WeekUtils.currentWeekStart() : null;
        LocalDateTime weekStartAt = null;
        LocalDateTime weekEndAt = null;
        if (week) {
            Instant start = weekStartDate.atStartOfDay(WeekUtils.BUSINESS_ZONE).toInstant();
            Instant end = weekStartDate.plusWeeks(1).atStartOfDay(WeekUtils.BUSINESS_ZONE).toInstant();
            weekStartAt = TimeUtils.toUtcLocalDateTime(start);
            weekEndAt = TimeUtils.toUtcLocalDateTime(end);
        }

        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        Page<LeaderboardRow> result = new Page<>(safePage, safePageSize);
        leaderboardMapper.selectAdminLeaderboard(result, week, weekStartDate, weekStartAt, weekEndAt);

        List<LeaderboardItem> items = result.getRecords().stream()
                .map(this::toItem)
                .toList();
        long totalPages = safePageSize <= 0 ? 0 : (result.getTotal() + safePageSize - 1) / safePageSize;
        return new AdminLeaderboardPage(
                normalizedPeriod,
                weekStartDate,
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                items,
                safePage,
                safePageSize,
                result.getTotal(),
                totalPages);
    }

    private LeaderboardItem toItem(LeaderboardRow row) {
        AdminLeaderboardUser user = new AdminLeaderboardUser(
                String.valueOf(row.getId()),
                row.getUsername(),
                row.getNickname(),
                row.getAvatarUrl(),
                row.getSchool(),
                row.getEmail(),
                row.getCfHandle());
        String manualStatus = row.getManualDeclarationCount() != null
                && row.getManualDeclarationCount() > 0
                ? "verified"
                : null;
        return new LeaderboardItem(
                row.getRank() == null ? 0 : row.getRank(),
                user,
                valueOrZero(row.getTotalScore()),
                valueOrZero(row.getInternalSolved()),
                valueOrZero(row.getCfSolved()),
                valueOrZero(row.getManualSolved()),
                row.getCfVerificationStatus(),
                row.getCfDataSource(),
                TimeUtils.toInstant(row.getCfLastSyncAt()),
                manualStatus,
                TimeUtils.toInstant(row.getManualUpdatedAt()));
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
