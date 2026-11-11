package com.tizhen.app.api.record;

import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.account.model.PlayerPrincipal;
import com.tizhen.core.record.model.WrongBookItem;
import com.tizhen.core.record.service.WrongBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选手错题本。接口契约见 docs/design/04.API接口设计.md §6.6 REC-01～REC-02。
 */
@RestController
@RequestMapping("/api/v1/records/wrong-book")
public class WrongBookController {

    private final WrongBookService wrongBookService;

    public WrongBookController(WrongBookService wrongBookService) {
        this.wrongBookService = wrongBookService;
    }

    @GetMapping
    public ApiResponse<PageResult<WrongBookItem>> list(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestParam(required = false) String problemType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(wrongBookService.list(
                principal.id(), problemType, keyword, page, pageSize));
    }

    @DeleteMapping("/{wrongBookId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal PlayerPrincipal principal,
                                       @PathVariable Long wrongBookId) {
        wrongBookService.remove(principal.id(), wrongBookId);
        return ResponseEntity.noContent().build();
    }
}
