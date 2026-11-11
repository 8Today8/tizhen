package com.tizhen.app.api.battle;

import com.tizhen.app.api.battle.dto.BattleReadyRequest;
import com.tizhen.app.api.battle.dto.BattleSubmissionRequest;
import com.tizhen.app.api.battle.dto.CreateBattleRoomRequest;
import com.tizhen.app.api.battle.dto.JoinBattleRoomRequest;
import com.tizhen.app.websocket.battle.BattleRealtimeService;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.account.model.PlayerPrincipal;
import com.tizhen.core.battle.model.BattleRecordDetail;
import com.tizhen.core.battle.model.BattleRecordItem;
import com.tizhen.core.battle.model.BattleRoomPreview;
import com.tizhen.core.battle.model.BattleRoomView;
import com.tizhen.core.battle.service.BattleService;
import com.tizhen.core.judge.model.SubmissionDetailView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选手端 1v1 对战。状态变更全部走 REST，WebSocket 只负责推送。
 * 接口契约见 docs/design/04.API接口设计.md §6.7 BAT-01～BAT-11。
 */
@RestController
@RequestMapping("/api/v1/battles")
public class BattleController {

    private final BattleService battleService;
    private final BattleRealtimeService realtimeService;

    public BattleController(BattleService battleService,
                            BattleRealtimeService realtimeService) {
        this.battleService = battleService;
        this.realtimeService = realtimeService;
    }

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<BattleRoomView>> create(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestBody(required = false) CreateBattleRoomRequest request) {
        CreateBattleRoomRequest safeRequest = request == null
                ? new CreateBattleRoomRequest(null, null)
                : request;
        BattleRoomView view = battleService.create(principal.id(),
                safeRequest.filterDifficulty(), safeRequest.filterTag());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(view));
    }

    @GetMapping("/rooms/by-code/{roomCode}")
    public ApiResponse<BattleRoomPreview> preview(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable String roomCode) {
        return ApiResponse.ok(battleService.preview(principal.id(), roomCode));
    }

    @PostMapping("/rooms/join")
    public ApiResponse<BattleRoomView> join(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestBody JoinBattleRoomRequest request) {
        BattleRoomView view = battleService.join(principal.id(),
                request == null ? null : request.roomCode());
        realtimeService.notifyRoomChanged(parseRoomId(view.id()));
        return ApiResponse.ok(view);
    }

    @GetMapping("/rooms/{roomId}")
    public ApiResponse<BattleRoomView> room(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable Long roomId) {
        return ApiResponse.ok(battleService.getRoom(principal.id(), roomId));
    }

    @PostMapping("/rooms/{roomId}/ready")
    public ApiResponse<BattleRoomView> ready(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable Long roomId,
            @RequestBody BattleReadyRequest request) {
        BattleRoomView view = battleService.setReady(principal.id(), roomId,
                request != null && request.ready());
        realtimeService.notifyRoomChanged(roomId);
        return ApiResponse.ok(view);
    }

    @PostMapping("/rooms/{roomId}/start")
    public ApiResponse<BattleRoomView> start(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable Long roomId) {
        BattleRoomView view = battleService.start(principal.id(), roomId);
        realtimeService.notifyRoomChanged(roomId);
        return ApiResponse.ok(view);
    }

    @PostMapping("/rooms/{roomId}/submissions")
    public ResponseEntity<ApiResponse<SubmissionDetailView>> submit(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable Long roomId,
            @RequestBody BattleSubmissionRequest request) {
        SubmissionDetailView detail = battleService.submit(principal.id(), roomId,
                request == null ? null : request.language(),
                request == null ? null : request.code());
        realtimeService.notifyRoomChanged(roomId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(detail));
    }

    @PostMapping("/rooms/{roomId}/surrender")
    public ApiResponse<BattleRoomView> surrender(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable Long roomId) {
        BattleRoomView view = battleService.surrender(principal.id(), roomId);
        realtimeService.notifyRoomChanged(roomId);
        return ApiResponse.ok(view);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable Long roomId) {
        battleService.cancel(principal.id(), roomId);
        realtimeService.notifyRoomChanged(roomId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/records")
    public ApiResponse<PageResult<BattleRecordItem>> records(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestParam(required = false) String result,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(battleService.listRecords(principal.id(), result, page, pageSize));
    }

    @GetMapping("/records/{roomId}")
    public ApiResponse<BattleRecordDetail> record(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @PathVariable Long roomId) {
        return ApiResponse.ok(battleService.getRecord(principal.id(), roomId));
    }

    private Long parseRoomId(String roomId) {
        try {
            return Long.valueOf(roomId);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
