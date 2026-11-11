package com.tizhen.app.api.account.dto;

import com.tizhen.core.account.model.LlmProviderView;

import java.util.List;

/**
 * 预置服务商列表响应。
 */
public record LlmProviderList(List<LlmProviderView> items) {
}
