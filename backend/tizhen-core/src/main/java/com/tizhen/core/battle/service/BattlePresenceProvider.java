package com.tizhen.core.battle.service;

/**
 * 由 Web 层提供的在线状态查询。worker 等无 WebSocket 的进程可以不注册实现。
 */
public interface BattlePresenceProvider {

    boolean isOnline(Long roomId, Long userId);
}
