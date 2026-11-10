package com.tizhen.core.external.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.WeekUtils;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.model.LoginMetadata;
import com.tizhen.core.account.service.AdminAuthService;
import com.tizhen.core.admin.service.OperationLogService;
import com.tizhen.core.external.entity.ExternalAccount;
import com.tizhen.core.external.mapper.ExternalAccountMapper;
import com.tizhen.core.external.model.CfSyncAccepted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CF 模拟数据刷新。首期不接入真实 Codeforces API，刷新只更新模拟快照。
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

    public CfSyncService(
            StringRedisTemplate redisTemplate,
            ExternalAccountMapper externalAccountMapper,
            OperationLogService operationLogService) {
        this.redisTemplate = redisTemplate;
        this.externalAccountMapper = externalAccountMapper;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public CfSyncAccepted sync(AdminPrincipal actor, LoginMetadata metadata) {
        if (!AdminAuthService.isSuperAdmin(actor)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "仅超级管理员可刷新 CF 模拟数据");
        }
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ErrorCode.CF_SYNC_IN_PROGRESS);
        }

        long accountCount = externalAccountMapper.selectCount(Wrappers.<ExternalAccount>lambdaQuery()
                .eq(ExternalAccount::getPlatform, PLATFORM)
                .eq(ExternalAccount::getStatus, "active"));
        operationLogService.record(
                actor.id(),
                "refresh_cf",
                "external_account",
                null,
                "刷新 CF 模拟数据，账户数 " + accountCount,
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
                            .eq(ExternalAccount::getStatus, "active"));
            LocalDate currentWeekStart = WeekUtils.currentWeekStart();
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            for (ExternalAccount account : accounts) {
                int increment = ThreadLocalRandom.current().nextInt(0, 3);
                int weeklyCount = increment;
                if (currentWeekStart.equals(account.getWeeklyWeekStart())) {
                    weeklyCount = (account.getWeeklyCount() == null ? 0 : account.getWeeklyCount()) + increment;
                }
                int allTimeCount = (account.getAllTimeCount() == null ? 0 : account.getAllTimeCount()) + increment;

                ExternalAccount update = new ExternalAccount();
                update.setId(account.getId());
                update.setWeeklyCount(weeklyCount);
                update.setWeeklyWeekStart(currentWeekStart);
                update.setAllTimeCount(allTimeCount);
                update.setLastSyncAt(now);
                update.setVerificationStatus("simulated");
                update.setDataSource("simulated");
                externalAccountMapper.updateById(update);
            }
            log.info("CF 模拟数据刷新完成，账户数={}", accounts.size());
        } catch (RuntimeException exception) {
            log.error("CF 模拟数据刷新失败", exception);
        } finally {
            redisTemplate.delete(LOCK_KEY);
        }
    }
}
