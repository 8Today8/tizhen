package com.tizhen.app.api.account;

import com.tizhen.app.api.account.dto.LlmProviderList;
import com.tizhen.app.api.account.dto.SaveLlmCredentialRequest;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.core.account.model.LlmCredentialView;
import com.tizhen.core.account.model.PlayerPrincipal;
import com.tizhen.core.account.service.LlmCredentialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选手 LLM 凭证。接口契约见 docs/design/04.API接口设计.md §6.1.2 CRED-01～CRED-04。
 */
@RestController
public class LlmCredentialController {

    private final LlmCredentialService llmCredentialService;

    public LlmCredentialController(LlmCredentialService llmCredentialService) {
        this.llmCredentialService = llmCredentialService;
    }

    @GetMapping("/api/v1/llm-providers")
    public ApiResponse<LlmProviderList> providers() {
        return ApiResponse.ok(new LlmProviderList(llmCredentialService.providers()));
    }

    @GetMapping("/api/v1/me/llm-credential")
    public ApiResponse<LlmCredentialView> view(@AuthenticationPrincipal PlayerPrincipal principal) {
        return ApiResponse.ok(llmCredentialService.view(principal.id()));
    }

    @PutMapping("/api/v1/me/llm-credential")
    public ApiResponse<LlmCredentialView> save(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @Valid @RequestBody SaveLlmCredentialRequest request) {
        return ApiResponse.ok(llmCredentialService.save(
                principal.id(), request.provider(), request.apiKey(), request.model()));
    }

    @DeleteMapping("/api/v1/me/llm-credential")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal PlayerPrincipal principal) {
        llmCredentialService.delete(principal.id());
        return ResponseEntity.noContent().build();
    }
}
