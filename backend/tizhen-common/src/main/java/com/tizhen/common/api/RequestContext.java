package com.tizhen.common.api;

import org.slf4j.MDC;

/**
 * 请求上下文。请求 ID 由 {@link RequestIdFilter} 写入 MDC，供响应与日志复用。
 */
public final class RequestContext {

    public static final String REQUEST_ID_ATTRIBUTE = "tizhen.requestId";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private RequestContext() {
    }

    public static String currentRequestId() {
        String requestId = MDC.get(REQUEST_ID_MDC_KEY);
        return requestId == null ? "" : requestId;
    }
}
