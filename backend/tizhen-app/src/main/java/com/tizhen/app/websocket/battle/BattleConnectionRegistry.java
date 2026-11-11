package com.tizhen.app.websocket.battle;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存中的对战连接注册表。同一用户可有多个页面/标签页连接，
 * 只有最后一个连接关闭时才视为离线。
 */
@Component
public class BattleConnectionRegistry {

    private final Map<Long, Map<Long, Set<WebSocketSession>>> rooms = new ConcurrentHashMap<>();
    private final Map<String, Long> lastPongAt = new ConcurrentHashMap<>();

    public void register(Long roomId, Long userId, WebSocketSession session) {
        rooms.computeIfAbsent(roomId, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet())
                .add(session);
        lastPongAt.put(session.getId(), System.currentTimeMillis());
    }

    public UnregisterResult unregister(Long roomId, Long userId, WebSocketSession session) {
        lastPongAt.remove(session.getId());
        Map<Long, Set<WebSocketSession>> room = rooms.get(roomId);
        if (room == null) {
            return new UnregisterResult(false, true);
        }
        Set<WebSocketSession> sessions = room.get(userId);
        if (sessions == null) {
            return new UnregisterResult(false, room.isEmpty());
        }
        boolean removed = sessions.remove(session);
        boolean userOffline = removed && sessions.isEmpty();
        if (userOffline) {
            room.remove(userId, sessions);
        }
        boolean roomEmpty = room.isEmpty();
        if (roomEmpty) {
            rooms.remove(roomId, room);
        }
        return new UnregisterResult(userOffline, roomEmpty);
    }

    public void touch(String sessionId) {
        if (sessionId != null) {
            lastPongAt.put(sessionId, System.currentTimeMillis());
        }
    }

    public Long lastPongAt(String sessionId) {
        return lastPongAt.get(sessionId);
    }

    public boolean isOnline(Long roomId, Long userId) {
        Map<Long, Set<WebSocketSession>> room = rooms.get(roomId);
        if (room == null) {
            return false;
        }
        Set<WebSocketSession> sessions = room.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public boolean hasSessions(Long roomId) {
        Map<Long, Set<WebSocketSession>> room = rooms.get(roomId);
        return room != null && !room.isEmpty();
    }

    public Set<Long> activeRoomIds() {
        return Set.copyOf(rooms.keySet());
    }

    public Set<Long> userIds(Long roomId) {
        Map<Long, Set<WebSocketSession>> room = rooms.get(roomId);
        return room == null ? Set.of() : Set.copyOf(room.keySet());
    }

    public List<WebSocketSession> sessions(Long roomId) {
        Map<Long, Set<WebSocketSession>> room = rooms.get(roomId);
        if (room == null) {
            return List.of();
        }
        List<WebSocketSession> all = new ArrayList<>();
        for (Set<WebSocketSession> sessions : room.values()) {
            all.addAll(sessions);
        }
        return all;
    }

    public List<WebSocketSession> sessions(Long roomId, Long userId) {
        Map<Long, Set<WebSocketSession>> room = rooms.get(roomId);
        if (room == null) {
            return List.of();
        }
        Set<WebSocketSession> sessions = room.get(userId);
        return sessions == null ? List.of() : List.copyOf(sessions);
    }

    public record UnregisterResult(boolean userOffline, boolean roomEmpty) {
    }
}
