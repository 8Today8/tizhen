package com.tizhen.core.leaderboard.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.common.util.WeekUtils;
import com.tizhen.core.leaderboard.mapper.LeaderboardMapper;
import com.tizhen.core.leaderboard.model.LeaderboardRow;
import com.tizhen.core.leaderboard.model.PlayerLeaderboardItem;
import com.tizhen.core.leaderboard.model.PlayerLeaderboardPage;
import com.tizhen.core.leaderboard.model.PlayerLeaderboardUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * 选手端榜单查询。周榜与总榜均为实时派生，不保存排名快照；
 * 返回结构只包含公开资料，CF Handle 等账号信息不对外暴露。
 */
@Service
public class PlayerLeaderboardService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final LeaderboardMapper leaderboardMapper;

    public PlayerLeaderboardService(LeaderboardMapper leaderboardMapper) {
        this.leaderboardMapper = leaderboardMapper;
    }

    @Transactional(readOnly = true)
    public PlayerLeaderboardPage getLeaderboard(Long userId, String period, int page, int pageSize) {
        PeriodContext context = resolvePeriod(period);
        String normalizedPeriod = context.period();
        boolean week = context.week();
        LocalDate weekStartDate = context.weekStartDate();
        LocalDateTime weekStartAt = context.weekStartAt();
        LocalDateTime weekEndAt = context.weekEndAt();

        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<LeaderboardRow> result = new Page<>(safePage, safePageSize);
        leaderboardMapper.selectLeaderboard(
                result, week, weekStartDate, weekStartAt, weekEndAt, null);

        List<PlayerLeaderboardItem> items = result.getRecords().stream()
                .map(row -> toItem(row, userId))
                .toList();

        LeaderboardRow meRow = leaderboardMapper.selectLeaderboardUser(
                week, weekStartDate, weekStartAt, weekEndAt, userId);
        PlayerLeaderboardItem me = meRow == null ? null : toItem(meRow, userId);

        long totalPages = safePageSize <= 0 ? 0 : (result.getTotal() + safePageSize - 1) / safePageSize;
        return new PlayerLeaderboardPage(
                normalizedPeriod,
                weekStartDate,
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                items,
                me,
                safePage,
                safePageSize,
                result.getTotal(),
                totalPages);
    }

    /**
     * 查询当前用户在指定榜单中的排名；无成绩或未上榜时返回 null。
     */
    @Transactional(readOnly = true)
    public PlayerLeaderboardItem getMyRank(Long userId, String period) {
        PeriodContext context = resolvePeriod(period);
        LeaderboardRow row = leaderboardMapper.selectLeaderboardUser(
                context.week(),
                context.weekStartDate(),
                context.weekStartAt(),
                context.weekEndAt(),
                userId);
        return row == null ? null : toItem(row, userId);
    }

    private PeriodContext resolvePeriod(String period) {
        String normalizedPeriod = normalizePeriod(period);
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
        return new PeriodContext(
                normalizedPeriod, week, weekStartDate, weekStartAt, weekEndAt);
    }

    private record PeriodContext(String period,
                                 boolean week,
                                 LocalDate weekStartDate,
                                 LocalDateTime weekStartAt,
                                 LocalDateTime weekEndAt) {
    }

    private String normalizePeriod(String period) {
        String normalized = period == null || period.isBlank()
                ? "week"
                : period.trim().toLowerCase(Locale.ROOT);
        if (!"week".equals(normalized) && !"all".equals(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "period 仅支持 week 或 all");
        }
        return normalized;
    }

    private PlayerLeaderboardItem toItem(LeaderboardRow row, Long currentUserId) {
        PlayerLeaderboardUser user = new PlayerLeaderboardUser(
                String.valueOf(row.getId()),
                row.getNickname(),
                row.getAvatarUrl(),
                row.getSchool());
        String manualStatus = row.getManualDeclarationCount() != null
                && row.getManualDeclarationCount() > 0
                ? "verified"
                : null;
        boolean isMe = currentUserId != null && currentUserId.equals(row.getId());
        return new PlayerLeaderboardItem(
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
                TimeUtils.toInstant(row.getManualUpdatedAt()),
                isMe);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
