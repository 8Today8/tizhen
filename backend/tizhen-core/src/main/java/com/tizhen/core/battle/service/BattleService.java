package com.tizhen.core.battle.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.entity.AppUser;
import com.tizhen.core.account.mapper.AppUserMapper;
import com.tizhen.core.account.model.UserBrief;
import com.tizhen.core.battle.entity.BattleRoom;
import com.tizhen.core.battle.mapper.BattleRoomMapper;
import com.tizhen.core.battle.model.BattlePlayerView;
import com.tizhen.core.battle.model.BattleProblemView;
import com.tizhen.core.battle.model.BattleRecordDetail;
import com.tizhen.core.battle.model.BattleRecordItem;
import com.tizhen.core.battle.model.BattleRecordProblem;
import com.tizhen.core.battle.model.BattleRoomPreview;
import com.tizhen.core.battle.model.BattleRoomView;
import com.tizhen.core.battle.model.BattleSubmissionSnapshot;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.mapper.SubmissionMapper;
import com.tizhen.core.judge.model.SubmissionCreateCommand;
import com.tizhen.core.judge.model.SubmissionDetailView;
import com.tizhen.core.judge.model.SubmissionListItem;
import com.tizhen.core.judge.model.SubmissionProblemBrief;
import com.tizhen.core.judge.service.SubmissionService;
import com.tizhen.core.problem.entity.Problem;
import com.tizhen.core.problem.entity.ProblemTag;
import com.tizhen.core.problem.mapper.ProblemMapper;
import com.tizhen.core.problem.mapper.ProblemTagMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * M07 对战核心流程。所有状态迁移都使用条件更新，
 * 以便先 AC、认输、断线、超时和取消在并发情况下只落一个最终状态。
 */
@Service
public class BattleService {

    public static final Duration BATTLE_DURATION = Duration.ofMinutes(30);

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int ROOM_CODE_LENGTH = 8;
    private static final int MAX_TAG_LENGTH = 64;
    private static final int MAX_EXPIRED_BATCH = 100;

    private static final String ROOM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String STATUS_WAITING = "waiting";
    private static final String STATUS_READY = "ready";
    private static final String STATUS_ONGOING = "ongoing";
    private static final String STATUS_FINISHED = "finished";
    private static final String STATUS_CANCELLED = "cancelled";

    private static final String RESULT_CREATOR_WIN = "creator_win";
    private static final String RESULT_OPPONENT_WIN = "opponent_win";
    private static final String RESULT_DRAW = "draw";

    private static final Set<String> DIFFICULTIES = Set.of("entry", "basic", "advanced");
    private static final Set<String> RECORD_RESULTS = Set.of("win", "loss", "draw");

    private final BattleRoomMapper battleRoomMapper;
    private final AppUserMapper appUserMapper;
    private final ProblemMapper problemMapper;
    private final ProblemTagMapper problemTagMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionService submissionService;
    private final BattlePresenceProvider presenceProvider;

    public BattleService(BattleRoomMapper battleRoomMapper,
                         AppUserMapper appUserMapper,
                         ProblemMapper problemMapper,
                         ProblemTagMapper problemTagMapper,
                         SubmissionMapper submissionMapper,
                         SubmissionService submissionService,
                         ObjectProvider<BattlePresenceProvider> presenceProvider) {
        this.battleRoomMapper = battleRoomMapper;
        this.appUserMapper = appUserMapper;
        this.problemMapper = problemMapper;
        this.problemTagMapper = problemTagMapper;
        this.submissionMapper = submissionMapper;
        this.submissionService = submissionService;
        BattlePresenceProvider provider = presenceProvider.getIfAvailable();
        this.presenceProvider = provider == null ? (roomId, userId) -> false : provider;
    }

    @Transactional
    public BattleRoomView create(Long userId, String filterDifficulty, String filterTag) {
        String difficulty = normalizeDifficulty(filterDifficulty);
        String tag = normalizeTag(filterTag);
        LocalDateTime now = now();

        for (int attempt = 0; attempt < 8; attempt++) {
            BattleRoom room = new BattleRoom();
            room.setRoomCode(generateRoomCode());
            room.setCreatorId(userId);
            room.setFilterDifficulty(difficulty);
            room.setFilterTag(tag);
            room.setCreatorReady(0);
            room.setOpponentReady(0);
            room.setStatus(STATUS_WAITING);
            room.setCreatedAt(now);
            room.setUpdatedAt(now);
            try {
                battleRoomMapper.insert(room);
                return toView(room, userId);
            } catch (DuplicateKeyException exception) {
                // 房间码碰撞时重新生成。
            }
        }
        throw new BusinessException(ErrorCode.COMMON_CONFLICT, "房间号生成失败，请重试");
    }

    @Transactional(readOnly = true)
    public BattleRoomPreview preview(Long userId, String roomCode) {
        BattleRoom room = battleRoomMapper.selectByRoomCode(normalizeRoomCode(roomCode));
        if (room == null) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_NOT_FOUND);
        }
        Map<Long, AppUser> users = loadUsers(room);
        boolean canJoin = STATUS_WAITING.equals(room.getStatus())
                && room.getOpponentId() == null
                && !userId.equals(room.getCreatorId());
        return new BattleRoomPreview(
                room.getRoomCode(),
                room.getStatus(),
                toBrief(users.get(room.getCreatorId()), room.getCreatorId()),
                room.getOpponentId() == null
                        ? null
                        : toBrief(users.get(room.getOpponentId()), room.getOpponentId()),
                canJoin,
                TimeUtils.toInstant(room.getCreatedAt()));
    }

    @Transactional
    public BattleRoomView join(Long userId, String roomCode) {
        BattleRoom room = battleRoomMapper.selectByRoomCode(normalizeRoomCode(roomCode));
        if (room == null) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_NOT_FOUND);
        }
        if (userId.equals(room.getCreatorId())) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID,
                    "不能加入自己创建的房间");
        }
        if (!STATUS_WAITING.equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID);
        }
        if (userId.equals(room.getOpponentId())) {
            return toView(room, userId);
        }
        if (room.getOpponentId() != null) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_FULL);
        }

        int updated = battleRoomMapper.joinIfAvailable(room.getId(), userId, now());
        if (updated == 0) {
            BattleRoom latest = battleRoomMapper.selectById(room.getId());
            if (latest == null) {
                throw new BusinessException(ErrorCode.BATTLE_ROOM_NOT_FOUND);
            }
            if (userId.equals(latest.getOpponentId())) {
                return toView(latest, userId);
            }
            if (latest.getOpponentId() != null) {
                throw new BusinessException(ErrorCode.BATTLE_ROOM_FULL);
            }
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID);
        }
        BattleRoom latest = battleRoomMapper.selectById(room.getId());
        return toView(latest, userId);
    }

    @Transactional(readOnly = true)
    public BattleRoomView getRoom(Long userId, Long roomId) {
        return toView(requireParticipant(userId, roomId), userId);
    }

    /**
     * WebSocket 实时推送内部使用；调用方已确认房间与用户身份。
     */
    @Transactional(readOnly = true)
    public BattleRoom findRoom(Long roomId) {
        return roomId == null ? null : battleRoomMapper.selectById(roomId);
    }

    @Transactional(readOnly = true)
    public BattleRoomView snapshot(Long roomId, Long userId) {
        BattleRoom room = findRoom(roomId);
        return room == null ? null : toView(room, userId);
    }

    @Transactional
    public BattleRoomView setReady(Long userId, Long roomId, boolean ready) {
        BattleRoom room = requireParticipant(userId, roomId);
        if (!STATUS_WAITING.equals(room.getStatus()) && !STATUS_READY.equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID);
        }

        int updated;
        if (userId.equals(room.getCreatorId())) {
            updated = battleRoomMapper.setCreatorReady(roomId, userId, ready, now());
        } else {
            updated = battleRoomMapper.setOpponentReady(roomId, userId, ready, now());
        }
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID);
        }
        return toView(battleRoomMapper.selectById(roomId), userId);
    }

    @Transactional
    public BattleRoomView start(Long userId, Long roomId) {
        BattleRoom room = requireParticipant(userId, roomId);
        if (!userId.equals(room.getCreatorId())) {
            throw new BusinessException(ErrorCode.BATTLE_NOT_CREATOR);
        }
        if (!STATUS_READY.equals(room.getStatus())
                || room.getOpponentId() == null
                || !isTrue(room.getCreatorReady())
                || !isTrue(room.getOpponentReady())) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID);
        }

        Problem problem = battleRoomMapper.selectRandomOjProblem(
                room.getFilterDifficulty(), room.getFilterTag());
        if (problem == null) {
            throw new BusinessException(ErrorCode.BATTLE_PROBLEM_UNAVAILABLE);
        }

        int updated = battleRoomMapper.startReadyRoom(roomId, userId, problem.getId(), now());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID);
        }
        return toView(battleRoomMapper.selectById(roomId), userId);
    }

    @Transactional
    public SubmissionDetailView submit(Long userId,
                                       Long roomId,
                                       String language,
                                       String code) {
        BattleRoom room = requireParticipant(userId, roomId);
        if (!STATUS_ONGOING.equals(room.getStatus()) || room.getProblemId() == null) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID);
        }

        LocalDateTime now = now();
        LocalDateTime endsAt = room.getStartedAt().plus(BATTLE_DURATION);
        if (!now.isBefore(endsAt)) {
            battleRoomMapper.finishOngoing(roomId, RESULT_DRAW, "timeout", endsAt);
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID);
        }

        return submissionService.createForBattle(userId, room.getProblemId(), roomId,
                new SubmissionCreateCommand(language, code, null, null));
    }

    @Transactional
    public BattleRoomView surrender(Long userId, Long roomId) {
        BattleRoom room = requireParticipant(userId, roomId);
        if (!STATUS_ONGOING.equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID);
        }
        String result = userId.equals(room.getCreatorId())
                ? RESULT_OPPONENT_WIN
                : RESULT_CREATOR_WIN;
        int updated = battleRoomMapper.finishOngoing(roomId, result, "surrender", now());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID);
        }
        return toView(battleRoomMapper.selectById(roomId), userId);
    }

    @Transactional
    public void cancel(Long userId, Long roomId) {
        BattleRoom room = requireParticipant(userId, roomId);
        if (!userId.equals(room.getCreatorId())) {
            throw new BusinessException(ErrorCode.BATTLE_NOT_CREATOR);
        }
        if (!STATUS_WAITING.equals(room.getStatus()) && !STATUS_READY.equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID);
        }

        int updated = battleRoomMapper.cancelWaiting(roomId, userId, now());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_STATE_INVALID);
        }
    }

    /**
     * 断线判负。仅在没有其他活动连接时由 WebSocket 层调用。
     */
    @Transactional
    public BattleRoomView handleDisconnect(Long roomId, Long userId) {
        BattleRoom room = battleRoomMapper.selectById(roomId);
        if (room == null
                || !STATUS_ONGOING.equals(room.getStatus())
                || (!userId.equals(room.getCreatorId()) && !userId.equals(room.getOpponentId()))) {
            return null;
        }
        String result = userId.equals(room.getCreatorId())
                ? RESULT_OPPONENT_WIN
                : RESULT_CREATOR_WIN;
        int updated = battleRoomMapper.finishOngoing(roomId, result, "disconnect", now());
        return updated == 0 ? null : toView(battleRoomMapper.selectById(roomId), userId);
    }

    /**
     * 服务端计时兜底：到达 30 分钟仍未 AC 的房间判平局。
     *
     * @return 本次真正结束的房间 ID
     */
    @Transactional
    public List<Long> closeExpiredRooms() {
        LocalDateTime now = now();
        List<BattleRoom> rooms = battleRoomMapper.selectExpiredOngoing(
                now.minus(BATTLE_DURATION), MAX_EXPIRED_BATCH);
        List<Long> finishedIds = new ArrayList<>();
        for (BattleRoom room : rooms) {
            LocalDateTime endedAt = room.getStartedAt().plus(BATTLE_DURATION);
            int updated = battleRoomMapper.finishOngoing(
                    room.getId(), RESULT_DRAW, "timeout", endedAt);
            if (updated > 0) {
                finishedIds.add(room.getId());
            }
        }
        return finishedIds;
    }

    @Transactional(readOnly = true)
    public PageResult<BattleRecordItem> listRecords(Long userId,
                                                    String result,
                                                    int page,
                                                    int pageSize) {
        String normalizedResult = normalizeResult(result);
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<BattleRoom> pageRequest = new Page<>(safePage, safePageSize);
        battleRoomMapper.selectMyFinishedRooms(pageRequest, userId, normalizedResult);

        Set<Long> opponentIds = new HashSet<>();
        Set<Long> problemIds = new HashSet<>();
        for (BattleRoom room : pageRequest.getRecords()) {
            Long opponentId = opponentId(room, userId);
            if (opponentId != null) {
                opponentIds.add(opponentId);
            }
            if (room.getProblemId() != null) {
                problemIds.add(room.getProblemId());
            }
        }
        Map<Long, AppUser> users = loadUsers(opponentIds);
        Map<Long, Problem> problems = loadProblems(problemIds);

        List<BattleRecordItem> items = pageRequest.getRecords().stream()
                .map(room -> toRecordItem(room, userId, users, problems))
                .toList();
        return PageResult.of(items, safePage, safePageSize, pageRequest.getTotal());
    }

    @Transactional(readOnly = true)
    public BattleRecordDetail getRecord(Long userId, Long roomId) {
        BattleRoom room = requireParticipant(userId, roomId);
        if (!STATUS_FINISHED.equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_NOT_FOUND);
        }

        Long opponentId = opponentId(room, userId);
        Set<Long> userIds = new HashSet<>();
        userIds.add(userId);
        if (opponentId != null) {
            userIds.add(opponentId);
        }
        Map<Long, AppUser> users = loadUsers(userIds);
        Map<Long, Problem> problems = room.getProblemId() == null
                ? Map.of()
                : loadProblems(Set.of(room.getProblemId()));
        BattleRecordItem item = toRecordItem(room, userId, users, problems);

        List<Submission> submissions = submissionMapper.selectList(
                Wrappers.<Submission>lambdaQuery()
                        .eq(Submission::getBattleId, roomId)
                        .in(Submission::getUserId, userIds)
                        .orderByAsc(Submission::getCreatedAt)
                        .orderByAsc(Submission::getId));
        Problem problem = room.getProblemId() == null ? null : problems.get(room.getProblemId());
        List<SubmissionListItem> mine = new ArrayList<>();
        List<SubmissionListItem> opponent = new ArrayList<>();
        for (Submission submission : submissions) {
            SubmissionListItem view = toSubmissionListItem(submission, problem);
            if (submission.getUserId().equals(userId)) {
                mine.add(view);
            } else if (opponentId != null && submission.getUserId().equals(opponentId)) {
                opponent.add(view);
            }
        }

        return new BattleRecordDetail(
                item.roomId(),
                item.roomCode(),
                item.opponent(),
                item.problem(),
                item.resultForMe(),
                item.endReason(),
                item.startedAt(),
                item.endedAt(),
                item.durationSeconds(),
                mine,
                opponent);
    }

    /**
     * 实时推送轮询使用，不要求调用方是参与者。
     */
    @Transactional(readOnly = true)
    public List<BattleSubmissionSnapshot> listSubmissionSnapshots(Long roomId) {
        return submissionMapper.selectList(Wrappers.<Submission>lambdaQuery()
                        .eq(Submission::getBattleId, roomId)
                        .orderByAsc(Submission::getId))
                .stream()
                .map(submission -> new BattleSubmissionSnapshot(
                        submission.getId(),
                        submission.getUserId(),
                        submission.getStatus(),
                        submission.getVerdict(),
                        submission.getScore(),
                        submission.getJudgedAt(),
                        submission.getUpdatedAt()))
                .toList();
    }

    public boolean isParticipant(BattleRoom room, Long userId) {
        return room != null
                && (userId.equals(room.getCreatorId()) || userId.equals(room.getOpponentId()));
    }

    private BattleRoomView toView(BattleRoom room, Long viewerId) {
        Map<Long, AppUser> users = loadUsers(room);
        Map<Long, PlayerStat> stats = loadPlayerStats(room.getId());

        BattlePlayerView creator = toPlayerView(
                room.getCreatorId(), room.getCreatorReady(), room, users, stats);
        BattlePlayerView opponent = room.getOpponentId() == null
                ? null
                : toPlayerView(room.getOpponentId(), room.getOpponentReady(), room, users, stats);

        BattleProblemView problem = null;
        if (room.getProblemId() != null
                && (STATUS_ONGOING.equals(room.getStatus())
                || STATUS_FINISHED.equals(room.getStatus()))) {
            Problem entity = problemMapper.selectById(room.getProblemId());
            if (entity != null) {
                problem = new BattleProblemView(
                        String.valueOf(entity.getId()),
                        entity.getType(),
                        entity.getTitle(),
                        entity.getDifficulty(),
                        loadTags(entity.getId()));
            }
        }

        Instant startedAt = TimeUtils.toInstant(room.getStartedAt());
        Instant endsAt = room.getStartedAt() == null
                ? null
                : TimeUtils.toInstant(room.getStartedAt().plus(BATTLE_DURATION));
        return new BattleRoomView(
                String.valueOf(room.getId()),
                room.getRoomCode(),
                room.getStatus(),
                creator,
                opponent,
                room.getFilterDifficulty(),
                room.getFilterTag(),
                isTrue(room.getCreatorReady()),
                isTrue(room.getOpponentReady()),
                problem,
                startedAt,
                endsAt,
                TimeUtils.toInstant(room.getEndedAt()),
                resultForMe(room, viewerId),
                room.getEndReason(),
                TimeUtils.toInstant(room.getCreatedAt()));
    }

    private BattlePlayerView toPlayerView(Long userId,
                                          Integer ready,
                                          BattleRoom room,
                                          Map<Long, AppUser> users,
                                          Map<Long, PlayerStat> stats) {
        PlayerStat stat = stats.getOrDefault(userId, new PlayerStat());
        return new BattlePlayerView(
                toBrief(users.get(userId), userId),
                isTrue(ready),
                presenceProvider.isOnline(room.getId(), userId),
                stat.count,
                stat.lastVerdict);
    }

    private BattleRecordItem toRecordItem(BattleRoom room,
                                          Long userId,
                                          Map<Long, AppUser> users,
                                          Map<Long, Problem> problems) {
        Long opponentId = opponentId(room, userId);
        Problem problem = room.getProblemId() == null ? null : problems.get(room.getProblemId());
        Long durationSeconds = null;
        if (room.getStartedAt() != null && room.getEndedAt() != null) {
            durationSeconds = Math.max(0,
                    Duration.between(room.getStartedAt(), room.getEndedAt()).getSeconds());
        }
        return new BattleRecordItem(
                String.valueOf(room.getId()),
                room.getRoomCode(),
                opponentId == null ? null : toBrief(users.get(opponentId), opponentId),
                problem == null
                        ? null
                        : new BattleRecordProblem(String.valueOf(problem.getId()), problem.getTitle()),
                resultForMe(room, userId),
                room.getEndReason(),
                TimeUtils.toInstant(room.getStartedAt()),
                TimeUtils.toInstant(room.getEndedAt()),
                durationSeconds);
    }

    private SubmissionListItem toSubmissionListItem(Submission submission, Problem problem) {
        String title = problem == null ? "题目" : problem.getTitle();
        return new SubmissionListItem(
                String.valueOf(submission.getId()),
                new SubmissionProblemBrief(
                        String.valueOf(submission.getProblemId()),
                        submission.getProblemType(),
                        title),
                String.valueOf(submission.getBattleId()),
                submission.getLanguage(),
                submission.getStatus(),
                submission.getVerdict(),
                submission.getScore(),
                TimeUtils.toInstant(submission.getCreatedAt()),
                TimeUtils.toInstant(submission.getJudgedAt()));
    }

    private Map<Long, PlayerStat> loadPlayerStats(Long roomId) {
        Map<Long, PlayerStat> stats = new HashMap<>();
        List<Submission> submissions = submissionMapper.selectList(
                Wrappers.<Submission>lambdaQuery()
                        .eq(Submission::getBattleId, roomId)
                        .orderByAsc(Submission::getId));
        for (Submission submission : submissions) {
            PlayerStat stat = stats.computeIfAbsent(submission.getUserId(), key -> new PlayerStat());
            stat.count++;
            stat.lastVerdict = submission.getVerdict();
        }
        return stats;
    }

    private Map<Long, AppUser> loadUsers(BattleRoom room) {
        Set<Long> ids = new HashSet<>();
        if (room.getCreatorId() != null) {
            ids.add(room.getCreatorId());
        }
        if (room.getOpponentId() != null) {
            ids.add(room.getOpponentId());
        }
        return loadUsers(ids);
    }

    private Map<Long, AppUser> loadUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, AppUser> users = new HashMap<>();
        for (AppUser user : appUserMapper.selectBatchIds(userIds)) {
            users.put(user.getId(), user);
        }
        return users;
    }

    private Map<Long, Problem> loadProblems(Set<Long> problemIds) {
        if (problemIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Problem> problems = new HashMap<>();
        for (Problem problem : problemMapper.selectBatchIds(problemIds)) {
            problems.put(problem.getId(), problem);
        }
        return problems;
    }

    private List<String> loadTags(Long problemId) {
        return problemTagMapper.selectList(Wrappers.<ProblemTag>lambdaQuery()
                        .eq(ProblemTag::getProblemId, problemId)
                        .eq(ProblemTag::getTagType, "topic")
                        .orderByAsc(ProblemTag::getTagValue))
                .stream()
                .map(ProblemTag::getTagValue)
                .toList();
    }

    private BattleRoom requireParticipant(Long userId, Long roomId) {
        if (roomId == null || roomId <= 0) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_NOT_FOUND);
        }
        BattleRoom room = battleRoomMapper.selectById(roomId);
        if (room == null
                || (!userId.equals(room.getCreatorId()) && !userId.equals(room.getOpponentId()))) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_NOT_FOUND);
        }
        return room;
    }

    private Long opponentId(BattleRoom room, Long userId) {
        if (userId.equals(room.getCreatorId())) {
            return room.getOpponentId();
        }
        return userId.equals(room.getOpponentId()) ? room.getCreatorId() : null;
    }

    private String resultForMe(BattleRoom room, Long userId) {
        if (!STATUS_FINISHED.equals(room.getStatus()) || room.getResult() == null) {
            return null;
        }
        if (RESULT_DRAW.equals(room.getResult())) {
            return "draw";
        }
        boolean creator = userId.equals(room.getCreatorId());
        if (RESULT_CREATOR_WIN.equals(room.getResult())) {
            return creator ? "win" : "loss";
        }
        if (RESULT_OPPONENT_WIN.equals(room.getResult())) {
            return creator ? "loss" : "win";
        }
        return null;
    }

    private UserBrief toBrief(AppUser user, Long userId) {
        if (user == null) {
            return new UserBrief(String.valueOf(userId), null, "未知选手", null, null);
        }
        return UserBrief.from(user);
    }

    private String normalizeRoomCode(String roomCode) {
        String normalized = roomCode == null ? "" : roomCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 16) {
            throw new BusinessException(ErrorCode.BATTLE_ROOM_NOT_FOUND);
        }
        return normalized;
    }

    private String normalizeDifficulty(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!DIFFICULTIES.contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "filterDifficulty 取值非法");
        }
        return normalized;
    }

    private String normalizeTag(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_TAG_LENGTH) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED,
                    "专题标签不能超过 " + MAX_TAG_LENGTH + " 个字符");
        }
        return normalized;
    }

    private String normalizeResult(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!RECORD_RESULTS.contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "result 取值非法");
        }
        return normalized;
    }

    private String generateRoomCode() {
        StringBuilder builder = new StringBuilder(ROOM_CODE_LENGTH);
        for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
            builder.append(ROOM_CODE_ALPHABET.charAt(RANDOM.nextInt(ROOM_CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private boolean isTrue(Integer value) {
        return value != null && value == 1;
    }

    private LocalDateTime now() {
        return TimeUtils.toUtcLocalDateTime(Instant.now());
    }

    private static final class PlayerStat {
        private int count;
        private String lastVerdict;
    }
}
