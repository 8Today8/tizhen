package com.tizhen.core.external.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.model.LoginMetadata;
import com.tizhen.core.account.service.AdminAuthService;
import com.tizhen.core.admin.service.OperationLogService;
import com.tizhen.core.external.entity.ExternalAccount;
import com.tizhen.core.external.mapper.ExternalAccountMapper;
import com.tizhen.core.external.model.CfSyncAccepted;
import com.tizhen.core.infra.cf.CodeforcesClient;
import com.tizhen.core.infra.cf.CodeforcesSolvedStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * CF 数据刷新：串行调用 Codeforces 公共 API，更新全部有效绑定的周快照与历史累计。
 */
@Service
public class CfSyncService {

    private static final Logger log = LoggerFactory.getLogger(CfSyncService.class);
    private static final String LOCK_KEY = "tizhen:admin:cf-sync:lock";
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final String PLATFORM = "codeforces";

    private final StringRedisTemplate redisTemplate;
    private final ExternalAccountMapper externalAccountMapper;
    private final OperationLogService operationLogService;
    private final CodeforcesClient codeforcesClient;

    public CfSyncService(
            StringRedisTemplate redisTemplate,
            ExternalAccountMapper externalAccountMapper,
            OperationLogService operationLogService,
            CodeforcesClient codeforcesClient) {
        this.redisTemplate = redisTemplate;
        this.externalAccountMapper = externalAccountMapper;
        this.operationLogService = operationLogService;
        this.codeforcesClient = codeforcesClient;
    }

    @Transactional
    public CfSyncAccepted sync(AdminPrincipal actor, LoginMetadata metadata) {
        if (!AdminAuthService.isSuperAdmin(actor)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "仅超级管理员可刷新 CF 数据");
        }
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ErrorCode.CF_SYNC_IN_PROGRESS);
        }

        long accountCount = externalAccountMapper.selectCount(Wrappers.<ExternalAccount>lambdaQuery()
                .eq(ExternalAccount::getPlatform, PLATFORM)
                .eq(ExternalAccount::getStatus, "active")
                .eq(ExternalAccount::getActiveFlag, 1));
        operationLogService.record(
                actor.id(),
                "refresh_cf",
                "external_account",
                null,
                "刷新 CF 数据，账户数 " + accountCount,
                Map.of("accountCount", accountCount),
                metadata);

        CompletableFuture.runAsync(this::refreshAllAccounts);
        return new CfSyncAccepted(true, (int) accountCount, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    private void refreshAllAccounts() {
        try {
            List<ExternalAccount> accounts = externalAccountMapper.selectList(
                    Wrappers.<ExternalAccount>lambdaQuery()
                            .eq(ExternalAccount::getPlatform, PLATFORM)
                            .eq(ExternalAccount::getStatus, "active")
                            .eq(ExternalAccount::getActiveFlag, 1));
            int synced = 0;
            for (ExternalAccount account : accounts) {
                if (account.getHandle() == null || account.getHandle().isBlank()) {
                    continue;
                }
                try {
                    CodeforcesSolvedStats stats = codeforcesClient.fetchSolvedStats(account.getHandle());
                    LocalDateTime now = TimeUtils.toUtcLocalDateTime(Instant.now());
                    ExternalAccount update = new ExternalAccount();
                    update.setId(account.getId());
                    update.setWeeklyCount(stats.weeklySolved());
                    update.setWeeklyWeekStart(stats.weekStartDate());
                    update.setAllTimeCount(stats.allTimeSolved());
                    update.setLastSyncAt(now);
                    update.setVerificationStatus("verified");
                    update.setDataSource("api");
                    externalAccountMapper.updateById(update);
                    synced++;
                } catch (CodeforcesClient.HandleNotFoundException exception) {
                    log.warn("CF 同步跳过：Handle 不存在，accountId={}，handle={}",
                            account.getId(), account.getHandle());
                } catch (CodeforcesClient.UnavailableException exception) {
                    log.warn("CF 同步失败，accountId={}，handle={}",
                            account.getId(), account.getHandle(), exception);
                }
            }
            log.info("CF 数据刷新完成，成功={}，总数={}", synced, accounts.size());
        } catch (RuntimeException exception) {
            log.error("CF 数据刷新失败", exception);
        } finally {
            redisTemplate.delete(LOCK_KEY);
        }
    }
}
