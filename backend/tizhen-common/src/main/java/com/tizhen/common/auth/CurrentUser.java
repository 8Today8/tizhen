package com.tizhen.common.auth;

/**
 * 会话中的当前选手身份。业务模块只依赖该快照，不自行读取会话。
 */
public record CurrentUser(Long id, String username, String nickname) {
}
