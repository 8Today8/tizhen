package com.tizhen.app.websocket.battle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.battle.entity.BattleRoom;
import com.tizhen.core.battle.model.BattleRoomView;
import com.tizhen.core.battle.model.BattleSubmissionSnapshot;
import com.tizhen.core.battle.service.BattleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对战实时推送：房间状态由 REST/判题进程变更，本服务通过短轮询发现变化后推送；
 * WebSocket 本身不承载业务写操作。
 */
@Service
public class BattleRealtimeService {

    private static final Logger log = LoggerFactory.getLogger(BattleRealtimeService.class);

    private static final String STATUS_ONGOING = "ongoing";
    private static final String STATUS_FINISHED = "finished";
    private static final String STATUS_CANCELLED = "cancelled";

    private static final CloseStatus CLOSE_ROOM_NOT_FOUND = new CloseStatus(4404, "room not found");
    private static final CloseStatus CLOSE_ROOM_ENDED = new CloseStatus(4409, "room ended");
    private static final CloseStatus CLOSE_HEARTBEAT_TIMEOUT =
            new CloseStatus(1001, "heartbeat timeout");

    private static final long HEARTBEAT_TIMEOUT_MILLIS = 11_000L;

    private final BattleService battleService;
    private final BattleConnectionRegistry registry;
    private final ObjectMapper objectMapper;
    private final Map<Long, RoomTracker> trackers = new ConcurrentHashMap<>();
    private final Object refreshLock = new Object();

    public BattleRealtimeService(BattleService battleService,
                                 BattleConnectionRegistry registry,
                                 ObjectMapper objectMapper) {
        this.battleService = battleService;
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    public void pollActiveRooms() {
        for (Long roomId : registry.activeRoomIds()) {
            try {
                refreshRoom(roomId);
            } catch (RuntimeException exception) {
                log.warn("刷新对战房间 {} 失败", roomId, exception);
            }
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void heartbeatAndTimeout() {
        sendHeartbeats();
        try {
            List<Long> finishedRoomIds = battleService.closeExpiredRooms();
            for (Long roomId : finishedRoomIds) {
                notifyRoomChanged(roomId);
            }
        } catch (RuntimeException exception) {
            log.warn("关闭超时对战房间失败", exception);
        }
    }

    /**
     * REST 写操作完成后立即刷新，避免前端等待下一次轮询。
     */
    public void notifyRoomChanged(Long roomId) {
        if (roomId == null) {
            return;
        }
        try {
            refreshRoom(roomId);
        } catch (RuntimeException exception) {
            log.warn("推送对战房间 {} 变更失败", roomId, exception);
        }
    }

    public void sendSnapshotToSession(Long roomId, Long userId, WebSocketSession session) {
        BattleRoomView view = battleService.snapshot(roomId, userId);
        if (view != null) {
            send(session, "battle.snapshot", Map.of("room", view));
        }
    }

    public void broadcastConnection(Long roomId, Long userId, boolean online) {
        BattleRoom room = battleService.findRoom(roomId);
        if (room == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", String.valueOf(userId));
        payload.put("online", online);
        payload.put("roomStatus", room.getStatus());
        sendToRoom(roomId, "battle.player.connection", payload);
    }

    public void sendError(WebSocketSession session, String code, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("code", code);
        payload.put("message", message);
        send(session, "battle.error", payload);
    }

    private void refreshRoom(Long roomId) {
        synchronized (refreshLock) {
            if (!registry.hasSessions(roomId)) {
                trackers.remove(roomId);
                return;
            }
            BattleRoom room = battleService.findRoom(roomId);
            if (room == null) {
                closeRoom(roomId, CLOSE_ROOM_NOT_FOUND);
                trackers.remove(roomId);
                return;
            }

            Map<Long, BattleSubmissionSnapshot> currentSubmissions =
                    loadSubmissionSnapshots(roomId);
            RoomTracker tracker = trackers.get(roomId);
            if (tracker == null) {
                trackers.put(roomId, RoomTracker.from(room, currentSubmissions));
                sendSnapshotToRoom(roomId);
                return;
            }

            boolean changed = false;
            String newStatus = room.getStatus();
            if (!Objects.equals(tracker.status, newStatus)) {
                if (STATUS_ONGOING.equals(newStatus)) {
                    sendStarted(roomId);
                }
                if (STATUS_FINISHED.equals(newStatus) || STATUS_CANCELLED.equals(newStatus)) {
                    sendEnded(roomId);
                    sendSnapshotToRoom(roomId);
                    closeRoom(roomId, CLOSE_ROOM_ENDED);
                    trackers.remove(roomId);
                    return;
                }
                changed = true;
            }

            if (tracker.opponentId == null && room.getOpponentId() != null) {
                sendPlayerJoined(roomId);
                changed = true;
            }

            if (tracker.creatorReady != isTrue(room.getCreatorReady())) {
                sendPlayerReady(roomId, room.getCreatorId(),
                        isTrue(room.getCreatorReady()), room.getStatus());
                changed = true;
            }
            if (room.getOpponentId() != null
                    && tracker.opponentReady != isTrue(room.getOpponentReady())) {
                sendPlayerReady(roomId, room.getOpponentId(),
                        isTrue(room.getOpponentReady()), room.getStatus());
                changed = true;
            }

            for (BattleSubmissionSnapshot submission : currentSubmissions.values()) {
                BattleSubmissionSnapshot previous = tracker.submissions.get(submission.id());
                if (previous == null || changedSubmission(previous, submission)) {
                    sendSubmissionUpdated(roomId, submission);
                    changed = true;
                }
            }
            if (tracker.submissions.size() != currentSubmissions.size()) {
                changed = true;
            }

            tracker.update(room, currentSubmissions);
            if (changed) {
                sendSnapshotToRoom(roomId);
            }
        }
    }

    private void sendStarted(Long roomId) {
        for (Long userId : registry.userIds(roomId)) {
            BattleRoomView view = battleService.snapshot(roomId, userId);
            if (view == null) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("problem", view.problem());
            payload.put("startedAt", view.startedAt());
            payload.put("endsAt", view.endsAt());
            sendToUser(roomId, userId, "battle.started", payload);
        }
    }

    private void sendEnded(Long roomId) {
        for (Long userId : registry.userIds(roomId)) {
            BattleRoomView view = battleService.snapshot(roomId, userId);
            if (view == null) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("resultForMe", view.resultForMe());
            payload.put("endReason", view.endReason());
            payload.put("endedAt", view.endedAt());
            sendToUser(roomId, userId, "battle.ended", payload);
        }
    }

    private void sendPlayerJoined(Long roomId) {
        for (Long userId : registry.userIds(roomId)) {
            BattleRoomView view = battleService.snapshot(roomId, userId);
            if (view == null) {
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("roomId", String.valueOf(roomId));
            payload.put("opponent", view.opponent() == null ? null : view.opponent().user());
            sendToUser(roomId, userId, "battle.player.joined", payload);
        }
    }

    private void sendPlayerReady(Long roomId, Long userId, boolean ready, String roomStatus) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", String.valueOf(userId));
        payload.put("ready", ready);
        payload.put("roomStatus", roomStatus);
        sendToRoom(roomId, "battle.player.ready", payload);
    }

    private void sendSubmissionUpdated(Long roomId, BattleSubmissionSnapshot submission) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", String.valueOf(submission.userId()));
        payload.put("submissionId", String.valueOf(submission.id()));
        payload.put("status", submission.status());
        payload.put("verdict", submission.verdict());
        payload.put("score", submission.score());
        payload.put("judgedAt", TimeUtils.toInstant(submission.judgedAt()));
        sendToRoom(roomId, "battle.submission.updated", payload);
    }

    private void sendSnapshotToRoom(Long roomId) {
        for (Long userId : registry.userIds(roomId)) {
            BattleRoomView view = battleService.snapshot(roomId, userId);
            if (view == null) {
                continue;
            }
            sendToUser(roomId, userId, "battle.snapshot", Map.of("room", view));
        }
    }

    private void sendHeartbeats() {
        long now = System.currentTimeMillis();
        for (Long roomId : registry.activeRoomIds()) {
            for (WebSocketSession session : registry.sessions(roomId)) {
                Long lastPong = registry.lastPongAt(session.getId());
                if (lastPong == null || now - lastPong > HEARTBEAT_TIMEOUT_MILLIS) {
                    closeSession(session, CLOSE_HEARTBEAT_TIMEOUT);
                    continue;
                }
                send(session, "battle.ping", Map.of("serverTime", Instant.now()));
            }
        }
    }

    private Map<Long, BattleSubmissionSnapshot> loadSubmissionSnapshots(Long roomId) {
        Map<Long, BattleSubmissionSnapshot> snapshots = new HashMap<>();
        for (BattleSubmissionSnapshot snapshot : battleService.listSubmissionSnapshots(roomId)) {
            snapshots.put(snapshot.id(), snapshot);
        }
        return snapshots;
    }

    private boolean changedSubmission(BattleSubmissionSnapshot previous,
                                      BattleSubmissionSnapshot current) {
        return !Objects.equals(previous.status(), current.status())
                || !Objects.equals(previous.verdict(), current.verdict())
                || !Objects.equals(previous.score(), current.score())
                || !Objects.equals(previous.judgedAt(), current.judgedAt())
                || !Objects.equals(previous.updatedAt(), current.updatedAt());
    }

    private void sendToRoom(Long roomId, String type, Object payload) {
        for (WebSocketSession session : registry.sessions(roomId)) {
            send(session, type, payload);
        }
    }

    private void sendToUser(Long roomId, Long userId, String type, Object payload) {
        for (WebSocketSession session : registry.sessions(roomId, userId)) {
            send(session, type, payload);
        }
    }

    private void send(WebSocketSession session, String type, Object payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(
                    new BattleWsMessage(type, payload, Instant.now()));
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(body));
                }
            }
        } catch (JsonProcessingException exception) {
            log.warn("序列化对战事件 {} 失败", type, exception);
        } catch (IOException exception) {
            log.debug("推送对战事件 {} 失败，连接可能已关闭", type);
        }
    }

    private void closeRoom(Long roomId, CloseStatus status) {
        for (WebSocketSession session : registry.sessions(roomId)) {
            closeSession(session, status);
        }
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException exception) {
            log.debug("关闭对战 WebSocket 失败", exception);
        }
    }

    private boolean isTrue(Integer value) {
        return value != null && value == 1;
    }

    public record BattleWsMessage(String type, Object payload, Instant serverTime) {
    }

    private static final class RoomTracker {

        private String status;
        private Long opponentId;
        private boolean creatorReady;
        private boolean opponentReady;
        private Map<Long, BattleSubmissionSnapshot> submissions = Map.of();

        static RoomTracker from(BattleRoom room,
                                Map<Long, BattleSubmissionSnapshot> submissions) {
            RoomTracker tracker = new RoomTracker();
            tracker.update(room, submissions);
            return tracker;
        }

        void update(BattleRoom room, Map<Long, BattleSubmissionSnapshot> submissions) {
            this.status = room.getStatus();
            this.opponentId = room.getOpponentId();
            this.creatorReady = room.getCreatorReady() != null && room.getCreatorReady() == 1;
            this.opponentReady = room.getOpponentReady() != null && room.getOpponentReady() == 1;
            this.submissions = submissions;
        }
    }
}
