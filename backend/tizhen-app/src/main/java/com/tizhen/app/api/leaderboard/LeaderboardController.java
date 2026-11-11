package com.tizhen.app.api.leaderboard;

import com.tizhen.common.api.ApiResponse;
import com.tizhen.core.account.model.PlayerPrincipal;
import com.tizhen.core.leaderboard.model.PlayerLeaderboardPage;
import com.tizhen.core.leaderboard.service.PlayerLeaderboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选手端榜单。接口契约见 docs/design/04.API接口设计.md §6.5 LB-01。
 */
@RestController
@RequestMapping("/api/v1/leaderboard")
public class LeaderboardController {

    private final PlayerLeaderboardService leaderboardService;

    public LeaderboardController(PlayerLeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ApiResponse<PlayerLeaderboardPage> getLeaderboard(
            @AuthenticationPrincipal PlayerPrincipal principal,
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(leaderboardService.getLeaderboard(principal.id(), period, page, pageSize));
    }
}
