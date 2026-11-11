package com.tizhen.app.websocket.battle;

import com.tizhen.core.account.model.PlayerPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 握手阶段复用 Spring Session 中的选手身份，未登录直接拒绝升级。
 * 房间可见性在连接建立后由 Handler 校验。
 */
@Component
public class BattleHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ROOM_ID_ATTRIBUTE = "tizhen.battle.roomId";
    public static final String USER_ID_ATTRIBUTE = "tizhen.battle.userId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof PlayerPrincipal principal)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Long roomId = parseRoomId(request.getURI().getPath());
        if (roomId == null) {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return false;
        }

        attributes.put(ROOM_ID_ATTRIBUTE, roomId);
        attributes.put(USER_ID_ATTRIBUTE, principal.id());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private Long parseRoomId(String path) {
        if (path == null) {
            return null;
        }
        int slash = path.lastIndexOf('/');
        if (slash < 0 || slash == path.length() - 1) {
            return null;
        }
        try {
            long roomId = Long.parseLong(path.substring(slash + 1));
            return roomId > 0 ? roomId : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
