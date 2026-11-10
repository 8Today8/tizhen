package com.tizhen.core.datagen.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.entity.AppUser;
import com.tizhen.core.account.mapper.AppUserMapper;
import com.tizhen.core.account.model.UserBrief;
import com.tizhen.core.datagen.entity.DataGenFile;
import com.tizhen.core.datagen.entity.DataGenTask;
import com.tizhen.core.datagen.mapper.DataGenFileMapper;
import com.tizhen.core.datagen.mapper.DataGenTaskMapper;
import com.tizhen.core.datagen.model.AdminDataGenTaskDetail;
import com.tizhen.core.datagen.model.AdminDataGenTaskListItem;
import com.tizhen.core.datagen.model.DataGenFileView;
import com.tizhen.core.datagen.model.DownloadPayload;
import com.tizhen.core.infra.file.FileStorage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 后台造数据任务查询与下载。不返回 generatorCode 与服务器文件路径。
 */
@Service
public class AdminDataGenService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "pending", "llm_generating", "llm_data_generating", "generating", "success", "failed");
    private static final Set<String> ALLOWED_MODES = Set.of(
            "random", "llm_generator", "llm_direct_data");

    private final DataGenTaskMapper dataGenTaskMapper;
    private final DataGenFileMapper dataGenFileMapper;
    private final AppUserMapper appUserMapper;
    private final FileStorage fileStorage;
    private final ObjectMapper objectMapper;

    public AdminDataGenService(
            DataGenTaskMapper dataGenTaskMapper,
            DataGenFileMapper dataGenFileMapper,
            AppUserMapper appUserMapper,
            FileStorage fileStorage,
            ObjectMapper objectMapper) {
        this.dataGenTaskMapper = dataGenTaskMapper;
        this.dataGenFileMapper = dataGenFileMapper;
        this.appUserMapper = appUserMapper;
        this.fileStorage = fileStorage;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<AdminDataGenTaskListItem> list(Long userId,
                                                    String status,
                                                    String generationMode,
                                                    int page,
                                                    int pageSize) {
        String normalizedStatus = normalizeEnum(status, ALLOWED_STATUSES, "status");
        String normalizedMode = normalizeEnum(generationMode, ALLOWED_MODES, "generationMode");
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<DataGenTask> result = new Page<>(safePage, safePageSize);
        dataGenTaskMapper.selectPage(result, Wrappers.<DataGenTask>lambdaQuery()
                .eq(userId != null, DataGenTask::getUserId, userId)
                .eq(normalizedStatus != null, DataGenTask::getStatus, normalizedStatus)
                .eq(normalizedMode != null, DataGenTask::getGenerationMode, normalizedMode)
                .orderByDesc(DataGenTask::getCreatedAt)
                .orderByDesc(DataGenTask::getId));

        List<Long> userIds = result.getRecords().stream()
                .map(DataGenTask::getUserId)
                .distinct()
                .toList();
        Map<Long, AppUser> users = userIds.isEmpty()
                ? Map.of()
                : appUserMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(AppUser::getId, Function.identity()));

        List<AdminDataGenTaskListItem> items = result.getRecords().stream()
                .map(task -> toListItem(task, users.get(task.getUserId())))
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional(readOnly = true)
    public AdminDataGenTaskDetail detail(Long taskId) {
        DataGenTask task = requireTask(taskId);
        AppUser user = appUserMapper.selectById(task.getUserId());
        List<DataGenFile> files = dataGenFileMapper.selectList(Wrappers.<DataGenFile>lambdaQuery()
                .eq(DataGenFile::getTaskId, taskId)
                .orderByAsc(DataGenFile::getGroupIndex));

        return new AdminDataGenTaskDetail(
                String.valueOf(task.getId()),
                task.getTitle(),
                user == null
                        ? new UserBrief(String.valueOf(task.getUserId()), null, null, null, null)
                        : UserBrief.from(user),
                user == null ? null : user.getUsername(),
                task.getStatement(),
                task.getInputFormat(),
                task.getOutputFormat(),
                task.getStdCode(),
                task.getStdLanguage(),
                parseJson(task.getDataRange()),
                task.getGroupCount() == null ? 0 : task.getGroupCount(),
                task.getGenerationMode(),
                task.getSpecialRequirements(),
                task.getLlmProvider(),
                task.getLlmModel(),
                task.getGeneratorStatus(),
                task.getLlmDataStatus(),
                task.getStatus(),
                task.getFailureStage(),
                task.getFailureReason(),
                parseJson(task.getResultSummary()),
                task.getTimeLimitMs() == null ? 0 : task.getTimeLimitMs(),
                task.getMemoryLimitMb() == null ? 0 : task.getMemoryLimitMb(),
                TimeUtils.toInstant(task.getStartedAt()),
                TimeUtils.toInstant(task.getFinishedAt()),
                TimeUtils.toInstant(task.getFilesExpireAt()),
                TimeUtils.toInstant(task.getFilesCleanedAt()),
                files.stream().map(this::toFileView).toList());
    }

    @Transactional(readOnly = true)
    public DownloadPayload downloadFile(Long taskId, Long fileId, boolean input) {
        requireTask(taskId);
        DataGenFile file = dataGenFileMapper.selectById(fileId);
        if (file == null || !taskId.equals(file.getTaskId())) {
            throw new BusinessException(ErrorCode.DATAGEN_FILE_NOT_FOUND);
        }
        String objectKey = input ? file.getInputPath() : file.getOutputPath();
        if (file.getFileDeletedAt() != null || objectKey == null || objectKey.isBlank()) {
            throw new BusinessException(ErrorCode.DATAGEN_FILE_EXPIRED);
        }
        if (!fileStorage.exists(objectKey)) {
            throw new BusinessException(ErrorCode.DATAGEN_FILE_NOT_FOUND);
        }
        try {
            InputStream stream = fileStorage.open(objectKey);
            long size = input
                    ? (file.getInputSizeBytes() == null ? -1 : file.getInputSizeBytes())
                    : (file.getOutputSizeBytes() == null ? -1 : file.getOutputSizeBytes());
            String filename = "task-" + taskId + "-group-" + file.getGroupIndex() + (input ? ".in" : ".out");
            return new DownloadPayload(new InputStreamResource(stream), size, filename);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.DATAGEN_FILE_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public DownloadPayload downloadArchive(Long taskId) {
        requireTask(taskId);
        List<DataGenFile> files = dataGenFileMapper.selectList(Wrappers.<DataGenFile>lambdaQuery()
                .eq(DataGenFile::getTaskId, taskId)
                .isNull(DataGenFile::getFileDeletedAt)
                .orderByAsc(DataGenFile::getGroupIndex));

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int entryCount = 0;
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            for (DataGenFile file : files) {
                entryCount += addZipEntry(zip, file.getInputPath(),
                        "task-" + taskId + "-group-" + file.getGroupIndex() + ".in");
                entryCount += addZipEntry(zip, file.getOutputPath(),
                        "task-" + taskId + "-group-" + file.getGroupIndex() + ".out");
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "打包下载失败");
        }
        if (entryCount == 0) {
            throw new BusinessException(ErrorCode.DATAGEN_ARCHIVE_EMPTY);
        }
        byte[] bytes = buffer.toByteArray();
        return new DownloadPayload(new ByteArrayResource(bytes), bytes.length, "task-" + taskId + ".zip");
    }

    private int addZipEntry(ZipOutputStream zip, String objectKey, String entryName) throws IOException {
        if (objectKey == null || objectKey.isBlank() || !fileStorage.exists(objectKey)) {
            return 0;
        }
        zip.putNextEntry(new ZipEntry(entryName));
        try (InputStream stream = fileStorage.open(objectKey)) {
            stream.transferTo(zip);
        }
        zip.closeEntry();
        return 1;
    }

    private DataGenTask requireTask(Long taskId) {
        DataGenTask task = dataGenTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.DATAGEN_TASK_NOT_FOUND);
        }
        return task;
    }

    private AdminDataGenTaskListItem toListItem(DataGenTask task, AppUser user) {
        return new AdminDataGenTaskListItem(
                String.valueOf(task.getId()),
                task.getTitle(),
                user == null
                        ? new UserBrief(String.valueOf(task.getUserId()), null, null, null, null)
                        : UserBrief.from(user),
                user == null ? null : user.getUsername(),
                task.getGenerationMode(),
                task.getStdLanguage(),
                task.getGroupCount() == null ? 0 : task.getGroupCount(),
                task.getStatus(),
                task.getLlmProvider(),
                task.getLlmModel(),
                TimeUtils.toInstant(task.getCreatedAt()),
                TimeUtils.toInstant(task.getStartedAt()),
                TimeUtils.toInstant(task.getFinishedAt()),
                TimeUtils.toInstant(task.getFilesExpireAt()),
                TimeUtils.toInstant(task.getFilesCleanedAt()));
    }

    private DataGenFileView toFileView(DataGenFile file) {
        boolean deleted = file.getFileDeletedAt() != null;
        boolean inputAvailable = !deleted
                && file.getInputPath() != null
                && !file.getInputPath().isBlank()
                && fileStorage.exists(file.getInputPath());
        boolean outputAvailable = !deleted
                && file.getOutputPath() != null
                && !file.getOutputPath().isBlank()
                && fileStorage.exists(file.getOutputPath());
        return new DataGenFileView(
                String.valueOf(file.getId()),
                file.getGroupIndex() == null ? 0 : file.getGroupIndex(),
                file.getInputSizeBytes(),
                file.getOutputSizeBytes(),
                file.getStdTimeMs(),
                file.getValidationStatus(),
                file.getValidationMessage(),
                inputAvailable,
                outputAvailable,
                TimeUtils.toInstant(file.getFileDeletedAt()));
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String normalizeEnum(String value, Set<String> allowed, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, fieldName + " 不受支持");
        }
        return normalized;
    }
}
