package com.tizhen.core.judge.service;

import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.mapper.SubmissionMapper;
import com.tizhen.testsupport.IntegrationTestBase;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 按 id 领取的并发语义集成测试：条件更新保证重复消息只执行一次，
 * 重试消息允许接管仍在执行态的任务，达到上限后拒绝再次领取。
 */
@SpringBootTest(classes = SubmissionClaimIntegrationTest.TestApplication.class,
        properties = {
                "spring.main.web-application-type=none",
                "spring.rabbitmq.dynamic=false",
                "tizhen.mq.enabled=false"
        })
@Transactional
class SubmissionClaimIntegrationTest extends IntegrationTestBase {

    @Autowired
    private SubmissionTaskService taskService;

    @Autowired
    private SubmissionMapper submissionMapper;

    @Test
    void pendingOjTaskCanOnlyBeClaimedOnce() {
        Submission submission = insertOj("pending", 0);
        Long id = submission.getId();

        Submission first = taskService.claimById(id, false, 3);
        assertNotNull(first);
        assertEquals("judging", first.getStatus());
        assertEquals(1, first.getJudgeAttempts());
        assertNotNull(first.getJudgeStartedAt());

        assertNull(taskService.claimById(id, false, 3), "重复消息不应再次占用");
    }

    @Test
    void retryClaimTakesOverInProgressAndStopsAtMaxAttempts() {
        Submission submission = insertOj("judging", 1);
        submission.setJudgeStartedAt(LocalDateTime.now(ZoneOffset.UTC));
        submissionMapper.updateById(submission);
        Long id = submission.getId();

        Submission secondAttempt = taskService.claimById(id, true, 3);
        assertNotNull(secondAttempt);
        assertEquals(2, secondAttempt.getJudgeAttempts());

        Submission thirdAttempt = taskService.claimById(id, true, 3);
        assertNotNull(thirdAttempt);
        assertEquals(3, thirdAttempt.getJudgeAttempts());

        assertNull(taskService.claimById(id, true, 3), "达到上限后不应继续领取");
    }

    @Test
    void shortAnswerClaimKeepsGradingStatus() {
        Submission submission = new Submission();
        submission.setUserId(1L);
        submission.setProblemId(1L);
        submission.setProblemType("short_answer");
        submission.setAnswerText("答案");
        submission.setStatus("grading");
        submission.setJudgeAttempts(0);
        submission.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        submissionMapper.insert(submission);

        Submission claimed = taskService.claimById(submission.getId(), false, 3);
        assertNotNull(claimed);
        assertEquals("grading", claimed.getStatus());
        assertEquals(1, claimed.getJudgeAttempts());
    }

    private Submission insertOj(String status, int attempts) {
        Submission submission = new Submission();
        submission.setUserId(1L);
        submission.setProblemId(1L);
        submission.setProblemType("oj");
        submission.setLanguage("cpp17");
        submission.setCode("int main() { return 0; }");
        submission.setStatus(status);
        submission.setJudgeAttempts(attempts);
        submission.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5));
        submissionMapper.insert(submission);
        return submission;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = "com.tizhen.core.judge.mapper", annotationClass = Mapper.class)
    @Import(SubmissionTaskService.class)
    static class TestApplication {
    }
}
