package com.tizhen.core.battle.service;

import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.battle.entity.BattleRoom;
import com.tizhen.core.battle.mapper.BattleRoomMapper;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.model.JudgeOutcome;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 判题完成后的对战收口。只处理进行中房间的首次 AC；
 * 条件更新保证同时 AC 时只有一个结果生效。
 */
@Service
public class BattleLifecycleService {

    private static final String VERDICT_AC = "AC";

    private final BattleRoomMapper battleRoomMapper;

    public BattleLifecycleService(BattleRoomMapper battleRoomMapper) {
        this.battleRoomMapper = battleRoomMapper;
    }

    @Transactional
    public void onSubmissionFinished(Submission submission, JudgeOutcome outcome) {
        if (submission.getBattleId() == null
                || !outcome.isFinished()
                || !VERDICT_AC.equals(outcome.verdict())) {
            return;
        }

        BattleRoom room = battleRoomMapper.selectById(submission.getBattleId());
        if (room == null
                || !"ongoing".equals(room.getStatus())
                || room.getProblemId() == null
                || !room.getProblemId().equals(submission.getProblemId())) {
            return;
        }

        String result;
        if (submission.getUserId().equals(room.getCreatorId())) {
            result = "creator_win";
        } else if (submission.getUserId().equals(room.getOpponentId())) {
            result = "opponent_win";
        } else {
            return;
        }

        battleRoomMapper.finishOngoing(room.getId(), result, "first_ac",
                TimeUtils.toUtcLocalDateTime(Instant.now()));
    }
}
