package com.tizhen.core.external.model;

import java.util.List;

/**
 * 外部账号列表响应。
 */
public record ExternalAccountList(List<ExternalAccountView> items) {

    public ExternalAccountList {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
