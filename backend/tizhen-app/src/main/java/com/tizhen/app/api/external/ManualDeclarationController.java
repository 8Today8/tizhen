package com.tizhen.app.api.external;

import com.tizhen.app.api.external.dto.UpsertManualDeclarationRequest;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.core.account.model.PlayerPrincipal;
import com.tizhen.core.external.model.ManualDeclarationView;
import com.tizhen.core.external.model.PlayerManualDeclarationPage;
import com.tizhen.core.external.service.ManualDeclarationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选手手动申报。接口契约见 docs/design/04.API接口设计.md §6.2.2 DEC-01～DEC-03。
 */
@RestController
@RequestMapping("/api/v1/me/manual-declarations")
public class ManualDeclarationController {

    private final ManualDeclarationService manualDeclarationService;

    public ManualDeclarationController(ManualDeclarationService manualDeclarationService) {
        this.manualDeclarationService = manualDeclarationService;
    }

    @GetMapping
    public ApiResponse<PlayerManualDeclarationPage> list(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(manualDeclarationService.list(principal.id(), page, pageSize));
    }

    @PutMapping
    public ApiResponse<ManualDeclarationView> upsert(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @Valid @RequestBody UpsertManualDeclarationRequest request) {
        return ApiResponse.ok(manualDeclarationService.upsert(
                principal.id(),
                request.getPlatform(),
                request.getAccount(),
                request.getProfileUrl(),
                request.getCount()));
    }

    @DeleteMapping("/{declarationId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal PlayerPrincipal principal,
                                       @PathVariable Long declarationId) {
        manualDeclarationService.delete(principal.id(), declarationId);
        return ResponseEntity.noContent().build();
    }
}
