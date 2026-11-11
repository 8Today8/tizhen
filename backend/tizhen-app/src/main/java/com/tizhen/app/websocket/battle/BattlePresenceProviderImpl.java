package com.tizhen.app.websocket.battle;

import com.tizhen.core.battle.service.BattlePresenceProvider;
import org.springframework.stereotype.Component;

/**
 * 用当前 Web 进程内的 WebSocket 连接判断在线状态。
 */
@Component
public class BattlePresenceProviderImpl implements BattlePresenceProvider {

    private final BattleConnectionRegistry registry;

    public BattlePresenceProviderImpl(BattleConnectionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean isOnline(Long roomId, Long userId) {
        return registry.isOnline(roomId, userId);
    }
}
