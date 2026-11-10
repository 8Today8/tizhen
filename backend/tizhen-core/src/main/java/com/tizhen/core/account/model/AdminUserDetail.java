package com.tizhen.core.account.model;

import java.util.List;

/**
 * 后台用户详情：基本资料、CF 绑定历史、凭证状态与业务计数。
 */
public record AdminUserDetail(
        AdminUserSummary user,
        List<AdminExternalAccountView> cfAccounts,
        AdminLlmCredentialView llmCredential,
        long dataGenTaskCount,
        long submissionCount,
        long firstAcCount,
        long wrongBookCount,
        long battleCount
) {
}
