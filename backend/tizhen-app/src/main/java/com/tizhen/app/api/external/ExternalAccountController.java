package com.tizhen.app.api.external;

import com.tizhen.app.api.external.dto.BindExternalAccountRequest;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.core.account.model.PlayerPrincipal;
import com.tizhen.core.external.model.ExternalAccountList;
import com.tizhen.core.external.model.ExternalAccountView;
import com.tizhen.core.external.service.ExternalAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选手外部平台账号。接口契约见 docs/design/04.API接口设计.md §6.2.1 EXT-01～EXT-03。
 */
@RestController
@RequestMapping("/api/v1/me/external-accounts")
public class ExternalAccountController {

    private final ExternalAccountService externalAccountService;

    public ExternalAccountController(ExternalAccountService externalAccountService) {
        this.externalAccountService = externalAccountService;
    }

    @GetMapping
    public ApiResponse<ExternalAccountList> list(@AuthenticationPrincipal PlayerPrincipal principal) {
        return ApiResponse.ok(externalAccountService.list(principal.id()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExternalAccountView>> bind(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @Valid @RequestBody BindExternalAccountRequest request) {
        ExternalAccountView account = externalAccountService.bind(
                principal.id(),
                request.getPlatform(),
                request.getHandle(),
                request.getProfileUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(account));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> unbind(@AuthenticationPrincipal PlayerPrincipal principal,
                                       @PathVariable Long accountId) {
        externalAccountService.unbind(principal.id(), accountId);
        return ResponseEntity.noContent().build();
    }
}
