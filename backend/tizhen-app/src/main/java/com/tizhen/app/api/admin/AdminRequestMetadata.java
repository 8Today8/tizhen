package com.tizhen.app.api.admin;

import com.tizhen.common.api.RequestContext;
import com.tizhen.core.account.model.LoginMetadata;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * 从当前请求提取审计所需的 IP、User-Agent 与请求 ID。
 */
public final class AdminRequestMetadata {

    private AdminRequestMetadata() {
    }

    public static LoginMetadata of(HttpServletRequest request) {
        return new LoginMetadata(
                request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT),
                RequestContext.currentRequestId());
    }
}
