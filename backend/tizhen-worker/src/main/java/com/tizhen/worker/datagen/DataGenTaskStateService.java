package com.tizhen.worker.datagen;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tizhen.core.datagen.entity.DataGenFile;
import com.tizhen.core.datagen.entity.DataGenTask;
import com.tizhen.core.datagen.mapper.DataGenFileMapper;
import com.tizhen.core.datagen.mapper.DataGenTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 造数据 worker 的任务状态迁移。领取、完成与失败都在事务内完成，
 * 避免失败任务留下半成品文件元数据。
 */
@Service
public class DataGenTaskStateService {

    private static final String MODE_RANDOM = "random";
    private static final String MODE_LLM_GENERATOR = "llm_generator";
    private static final String MODE_LLM_DIRECT = "llm_direct_data";

    private final DataGenTaskMapper dataGenTaskMapper;
    private final DataGenFileMapper dataGenFileMapper;

    public DataGenTaskStateService(DataGenTaskMapper dataGenTaskMapper,
                                   DataGenFileMapper dataGenFileMapper) {
        this.dataGenTaskMapper = dataGenTaskMapper;
        this.dataGenFileMapper = dataGenFileMapper;
    }

    @Transactional
    public List<DataGenTask> claim(int limit) {
        List<DataGenTask> tasks = dataGenTaskMapper.selectPendingForUpdate(limit);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (DataGenTask task : tasks) {
            task.setStatus(initialStatus(task.getGenerationMode()));
            task.setStartedAt(now);
            task.setHeartbeatAt(now);
            task.setUpdatedAt(now);
            task.setAttempts(task.getAttempts() == null ? 1 : task.getAttempts() + 1);
            if (MODE_LLM_GENERATOR.equals(task.getGenerationMode())) {
                task.setGeneratorStatus("generating");
            }
            if (MODE_LLM_DIRECT.equals(task.getGenerationMode())) {
                task.setLlmDataStatus("generating");
            }
            dataGenTaskMapper.updateById(task);
        }
        return tasks;
    }

    @Transactional
    public void heartbeat(Long taskId) {
        DataGenTask task = new DataGenTask();
        task.setId(taskId);
        task.setHeartbeatAt(LocalDateTime.now(ZoneOffset.UTC));
        task.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        dataGenTaskMapper.updateById(task);
    }

    @Transactional
    public void generatorReady(Long taskId, String generatorCode) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DataGenTask task = new DataGenTask();
        task.setId(taskId);
        task.setGeneratorCode(generatorCode);
        task.setGeneratorStatus("ready");
        task.setStatus("generating");
        task.setHeartbeatAt(now);
        task.setUpdatedAt(now);
        dataGenTaskMapper.updateById(task);
    }

    @Transactional
    public void llmDataReady(Long taskId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DataGenTask task = new DataGenTask();
        task.setId(taskId);
        task.setLlmDataStatus("ready");
        task.setStatus("generating");
        task.setHeartbeatAt(now);
        task.setUpdatedAt(now);
        dataGenTaskMapper.updateById(task);
    }

    @Transactional
    public void complete(Long taskId, String resultSummary, List<DataGenFile> files) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DataGenTask task = dataGenTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus("success");
        task.setFinishedAt(now);
        task.setHeartbeatAt(now);
        task.setUpdatedAt(now);
        task.setResultSummary(resultSummary);
        task.setFailureStage(null);
        task.setFailureReason(null);
        if (MODE_LLM_GENERATOR.equals(task.getGenerationMode())) {
            task.setGeneratorStatus("ready");
        }
        if (MODE_LLM_DIRECT.equals(task.getGenerationMode())) {
            task.setLlmDataStatus("ready");
        }
        dataGenTaskMapper.updateById(task);

        for (DataGenFile file : files) {
            file.setTaskId(taskId);
            file.setCreatedAt(now);
            file.setUpdatedAt(now);
            dataGenFileMapper.insert(file);
        }
    }

    @Transactional
    public void fail(Long taskId, String stage, String reason) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DataGenTask task = dataGenTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus("failed");
        task.setFailureStage(stage);
        task.setFailureReason(truncate(reason, 2_000));
        task.setFinishedAt(now);
        task.setHeartbeatAt(now);
        task.setUpdatedAt(now);
        if (MODE_LLM_GENERATOR.equals(task.getGenerationMode())) {
            task.setGeneratorStatus("failed");
        }
        if (MODE_LLM_DIRECT.equals(task.getGenerationMode())) {
            task.setLlmDataStatus("failed");
        }
        dataGenTaskMapper.updateById(task);
    }

    @Transactional
    public void recoverStale(LocalDateTime threshold, int maxAttempts) {
        List<DataGenTask> tasks = dataGenTaskMapper.selectStaleInProgress(threshold);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (DataGenTask task : tasks) {
            int attempts = task.getAttempts() == null ? 0 : task.getAttempts();
            if (attempts >= maxAttempts) {
                task.setStatus("failed");
                task.setFailureStage("system");
                task.setFailureReason("任务执行超时，已终止，请重新提交");
                task.setFinishedAt(now);
                task.setHeartbeatAt(now);
                task.setUpdatedAt(now);
                if (MODE_LLM_GENERATOR.equals(task.getGenerationMode())) {
                    task.setGeneratorStatus("failed");
                }
                if (MODE_LLM_DIRECT.equals(task.getGenerationMode())) {
                    task.setLlmDataStatus("failed");
                }
            } else {
                task.setStatus("pending");
                task.setStartedAt(null);
                task.setHeartbeatAt(null);
                task.setUpdatedAt(now);
                if (MODE_LLM_GENERATOR.equals(task.getGenerationMode())) {
                    task.setGeneratorStatus("pending");
                }
                if (MODE_LLM_DIRECT.equals(task.getGenerationMode())) {
                    task.setLlmDataStatus("pending");
                }
            }
            dataGenTaskMapper.updateById(task);
        }
    }

    @Transactional
    public void cleanExpiredFiles(List<DataGenTask> tasks,
                                  java.util.function.Consumer<DataGenFile> deleter) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (DataGenTask task : tasks) {
            List<DataGenFile> files = dataGenFileMapper.selectList(
                    Wrappers.<DataGenFile>lambdaQuery()
                            .eq(DataGenFile::getTaskId, task.getId())
                            .isNull(DataGenFile::getFileDeletedAt));
            for (DataGenFile file : files) {
                deleter.accept(file);
                file.setFileDeletedAt(now);
                file.setUpdatedAt(now);
                dataGenFileMapper.updateById(file);
            }
            DataGenTask update = new DataGenTask();
            update.setId(task.getId());
            update.setFilesCleanedAt(now);
            update.setUpdatedAt(now);
            dataGenTaskMapper.updateById(update);
        }
    }

    private String initialStatus(String mode) {
        if (MODE_LLM_GENERATOR.equals(mode)) {
            return "llm_generating";
        }
        if (MODE_LLM_DIRECT.equals(mode)) {
            return "llm_data_generating";
        }
        if (MODE_RANDOM.equals(mode)) {
            return "generating";
        }
        return "generating";
    }

    private String truncate(String value, int maxLength) {
        String normalized = value == null ? "造数据任务执行失败" : value;
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
