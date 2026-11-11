package com.tizhen.app.websocket.battle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.core.battle.entity.BattleRoom;
import com.tizhen.core.battle.model.BattleRoomView;
import com.tizhen.core.battle.service.BattleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * 对战 WebSocket：握手鉴权后校验房间可见性；客户端只发送 battle.pong；
 * 最后一个连接断开且比赛进行中时判负。
 */
@Component
public class BattleWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(BattleWebSocketHandler.class);

    private static final CloseStatus CLOSE_UNAUTHENTICATED = new CloseStatus(4401, "unauthenticated");
    private static final CloseStatus CLOSE_ROOM_NOT_FOUND = new CloseStatus(4404, "room not found");
    private static final CloseStatus CLOSE_ROOM_ENDED = new CloseStatus(4409, "room ended");
    private static final CloseStatus CLOSE_INTERNAL_ERROR = new CloseStatus(1011, "internal error");

    private static final String STATUS_ONGOING = "ongoing";
    private static final String STATUS_FINISHED = "finished";
    private static final String STATUS_CANCELLED = "cancelled";

    private final BattleService battleService;
    private final BattleConnectionRegistry registry;
    private final BattleRealtimeService realtimeService;
    private final ObjectMapper objectMapper;

    public BattleWebSocketHandler(BattleService battleService,
                                  BattleConnectionRegistry registry,
                                  BattleRealtimeService realtimeService,
                                  ObjectMapper objectMapper) {
        this.battleService = battleService;
        this.registry = registry;
        this.realtimeService = realtimeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long roomId = roomId(session);
        Long userId = userId(session);
        if (roomId == null || userId == null) {
            close(session, CLOSE_UNAUTHENTICATED);
            return;
        }

        BattleRoomView view;
        try {
            view = battleService.getRoom(userId, roomId);
        } catch (BusinessException exception) {
            close(session, CLOSE_ROOM_NOT_FOUND);
            return;
        } catch (RuntimeException exception) {
            log.warn("建立对战连接时读取房间失败 roomId={} userId={}", roomId, userId, exception);
            close(session, CLOSE_INTERNAL_ERROR);
            return;
        }

        if (STATUS_FINISHED.equals(view.status()) || STATUS_CANCELLED.equals(view.status())) {
            realtimeService.sendSnapshotToSession(roomId, userId, session);
            close(session, CLOSE_ROOM_ENDED);
            return;
        }

        registry.register(roomId, userId, session);
        realtimeService.sendSnapshotToSession(roomId, userId, session);
        realtimeService.broadcastConnection(roomId, userId, true);
        realtimeService.notifyRoomChanged(roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.path("type").asText("");
            if ("battle.pong".equals(type)) {
                registry.touch(session.getId());
                return;
            }
            realtimeService.sendError(session, "COMMON_INVALID_PARAM", "客户端只允许发送 battle.pong");
        } catch (IOException exception) {
            realtimeService.sendError(session, "COMMON_INVALID_PARAM", "消息格式错误");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long roomId = roomId(session);
        Long userId = userId(session);
        if (roomId == null || userId == null) {
            return;
        }

        BattleConnectionRegistry.UnregisterResult result =
                registry.unregister(roomId, userId, session);
        if (!result.userOffline()) {
            return;
        }

        realtimeService.broadcastConnection(roomId, userId, false);
        BattleRoom room = battleService.findRoom(roomId);
        if (room != null && STATUS_ONGOING.equals(room.getStatus())) {
            battleService.handleDisconnect(roomId, userId);
            realtimeService.notifyRoomChanged(roomId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("对战 WebSocket 传输异常 sessionId={}", session.getId(), exception);
    }

    private Long roomId(WebSocketSession session) {
        Object value = session.getAttributes().get(BattleHandshakeInterceptor.ROOM_ID_ATTRIBUTE);
        return value instanceof Long roomId ? roomId : null;
    }

    private Long userId(WebSocketSession session) {
        Object value = session.getAttributes().get(BattleHandshakeInterceptor.USER_ID_ATTRIBUTE);
        return value instanceof Long userId ? userId : null;
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException exception) {
            log.debug("关闭对战 WebSocket 失败", exception);
        }
    }
}
