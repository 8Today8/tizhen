package com.tizhen.core.datagen.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.common.api.PageResult;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.entity.LlmCredential;
import com.tizhen.core.account.model.LlmCredentialView;
import com.tizhen.core.account.service.LlmCredentialService;
import com.tizhen.core.datagen.entity.DataGenFile;
import com.tizhen.core.datagen.entity.DataGenTask;
import com.tizhen.core.datagen.mapper.DataGenFileMapper;
import com.tizhen.core.datagen.mapper.DataGenTaskMapper;
import com.tizhen.core.datagen.model.DataGenCreateCommand;
import com.tizhen.core.datagen.model.DataGenFileView;
import com.tizhen.core.datagen.model.DataGenOptions;
import com.tizhen.core.datagen.model.DataGenTaskDetail;
import com.tizhen.core.datagen.model.DataGenTaskListItem;
import com.tizhen.core.datagen.model.DataRangeItem;
import com.tizhen.core.datagen.model.DownloadPayload;
import com.tizhen.core.infra.file.FileStorage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 选手造数据工作台。任务异步执行，创建接口只负责校验、占用并发名额与持久化。
 */
@Service
public class DataGenService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int FILE_RETENTION_DAYS = 7;
    private static final int MAX_STD_BYTES = 64 * 1024;
    private static final String MODE_RANDOM = "random";
    private static final String MODE_LLM_GENERATOR = "llm_generator";
    private static final String MODE_LLM_DIRECT = "llm_direct_data";
    private static final Set<String> MODES = Set.of(MODE_RANDOM, MODE_LLM_GENERATOR, MODE_LLM_DIRECT);
    private static final Set<String> STATUSES = Set.of(
            "pending", "llm_generating", "llm_data_generating", "generating", "success", "failed");
    private static final Set<String> LANGUAGES = Set.of("cpp17", "python310");
    private static final Set<String> RANGE_TYPES = Set.of("int", "long long", "double");
    private static final BigDecimal INT_MIN = BigDecimal.valueOf(Integer.MIN_VALUE);
    private static final BigDecimal INT_MAX = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final BigDecimal LONG_MIN = BigDecimal.valueOf(Long.MIN_VALUE);
    private static final BigDecimal LONG_MAX = BigDecimal.valueOf(Long.MAX_VALUE);

    private final DataGenTaskMapper dataGenTaskMapper;
    private final DataGenFileMapper dataGenFileMapper;
    private final LlmCredentialService llmCredentialService;
    private final DataGenRateLimiter rateLimiter;
    private final FileStorage fileStorage;
    private final ObjectMapper objectMapper;

    public DataGenService(DataGenTaskMapper dataGenTaskMapper,
                          DataGenFileMapper dataGenFileMapper,
                          LlmCredentialService llmCredentialService,
                          DataGenRateLimiter rateLimiter,
                          FileStorage fileStorage,
                          ObjectMapper objectMapper) {
        this.dataGenTaskMapper = dataGenTaskMapper;
        this.dataGenFileMapper = dataGenFileMapper;
        this.llmCredentialService = llmCredentialService;
        this.rateLimiter = rateLimiter;
        this.fileStorage = fileStorage;
        this.objectMapper = objectMapper;
    }

    public DataGenOptions options(Long userId) {
        LlmCredentialView credential = llmCredentialService.view(userId);
        return new DataGenOptions(
                List.of(
                        new DataGenOptions.ModeOption(MODE_RANDOM, false),
                        new DataGenOptions.ModeOption(MODE_LLM_GENERATOR, true),
                        new DataGenOptions.ModeOption(MODE_LLM_DIRECT, true)),
                List.of(
                        new DataGenOptions.EnumOption("cpp17", "C++17"),
                        new DataGenOptions.EnumOption("python310", "Python 3.10")),
                List.of("int", "long long", "double"),
                new DataGenOptions.Limits(
                        new DataGenOptions.Range(1, 50),
                        new DataGenOptions.Range(100, 60_000),
                        new DataGenOptions.Range(16, 2_048)),
                credential,
                credential.hasActiveTask());
    }

    @Transactional
    public DataGenTaskDetail create(Long userId, DataGenCreateCommand command) {
        rateLimiter.assertAllowed(userId);
        String title = requireText(command.title(), "任务名称", 200);
        String statement = requireText(command.statement(), "题面", 100_000);
        String inputFormat = requireText(command.inputFormat(), "输入格式", 20_000);
        String outputFormat = requireText(command.outputFormat(), "输出格式", 20_000);
        String stdCode = requireText(command.stdCode(), "标准程序", 100_000);
        if (stdCode.getBytes(StandardCharsets.UTF_8).length > MAX_STD_BYTES) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "标准程序不能超过 64 KB");
        }
        String stdLanguage = command.stdLanguage() == null
                ? "" : command.stdLanguage().trim().toLowerCase(Locale.ROOT);
        if (!LANGUAGES.contains(stdLanguage)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "标准程序语言不受支持");
        }
        String mode = command.generationMode() == null
                ? "" : command.generationMode().trim().toLowerCase(Locale.ROOT);
        if (!MODES.contains(mode)) {
            throw new BusinessException(ErrorCode.DATAGEN_MODE_INVALID);
        }
        if (command.groupCount() < 1 || command.groupCount() > 50) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "数据组数必须在 1–50 之间");
        }
        if (command.timeLimitMs() < 100 || command.timeLimitMs() > 60_000) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "时间限制必须在 100–60000 毫秒之间");
        }
        if (command.memoryLimitMb() < 16 || command.memoryLimitMb() > 2_048) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "内存限制必须在 16–2048 MB 之间");
        }
        List<DataRangeItem> ranges = validateRanges(command.dataRange());
        String specialRequirements = normalizeSpecialRequirements(command.specialRequirements(), mode);

        LlmCredential credential = null;
        if (!MODE_RANDOM.equals(mode)) {
            credential = llmCredentialService.requireUsableCredential(userId);
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DataGenTask task = new DataGenTask();
        task.setUserId(userId);
        task.setTitle(title);
        task.setStatement(statement);
        task.setInputFormat(inputFormat);
        task.setOutputFormat(outputFormat);
        task.setStdCode(stdCode);
        task.setStdLanguage(stdLanguage);
        task.setDataRange(writeJson(ranges));
        task.setGroupCount(command.groupCount());
        task.setGenerationMode(mode);
        task.setSpecialRequirements(specialRequirements);
        task.setTimeLimitMs(command.timeLimitMs());
        task.setMemoryLimitMb(command.memoryLimitMb());
        task.setStatus("pending");
        task.setAttempts(0);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setFilesExpireAt(now.plusDays(FILE_RETENTION_DAYS));
        if (credential != null) {
            task.setLlmProvider(credential.getProvider());
            task.setLlmModel(credential.getModel());
            task.setLlmCredentialId(credential.getId());
        }
        task.setGeneratorStatus(MODE_LLM_GENERATOR.equals(mode) ? "pending" : "not_applicable");
        task.setLlmDataStatus(MODE_LLM_DIRECT.equals(mode) ? "pending" : "not_applicable");

        try {
            dataGenTaskMapper.insert(task);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DATAGEN_ACTIVE_TASK_EXISTS);
        }
        return detail(userId, task.getId());
    }

    @Transactional(readOnly = true)
    public PageResult<DataGenTaskListItem> list(Long userId,
                                                String status,
                                                String generationMode,
                                                int page,
                                                int pageSize) {
        String normalizedStatus = normalizeEnum(status, STATUSES, "status");
        String normalizedMode = normalizeEnum(generationMode, MODES, "generationMode");
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<DataGenTask> result = new Page<>(safePage, safePageSize);
        dataGenTaskMapper.selectPage(result, Wrappers.<DataGenTask>lambdaQuery()
                .eq(DataGenTask::getUserId, userId)
                .eq(normalizedStatus != null, DataGenTask::getStatus, normalizedStatus)
                .eq(normalizedMode != null, DataGenTask::getGenerationMode, normalizedMode)
                .orderByDesc(DataGenTask::getCreatedAt)
                .orderByDesc(DataGenTask::getId));

        List<DataGenTaskListItem> items = result.getRecords().stream()
                .map(this::toListItem)
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional(readOnly = true)
    public DataGenTaskDetail detail(Long userId, Long taskId) {
        DataGenTask task = requireOwnedTask(userId, taskId);
        List<DataGenFile> files = dataGenFileMapper.selectList(Wrappers.<DataGenFile>lambdaQuery()
                .eq(DataGenFile::getTaskId, taskId)
                .orderByAsc(DataGenFile::getGroupIndex));
        return new DataGenTaskDetail(
                String.valueOf(task.getId()),
                task.getTitle(),
                task.getStatement(),
                task.getInputFormat(),
                task.getOutputFormat(),
                task.getStdCode(),
                task.getStdLanguage(),
                parseRanges(task.getDataRange()),
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
    public DownloadPayload downloadFile(Long userId, Long taskId, Long fileId, boolean input) {
        requireOwnedTask(userId, taskId);
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
            String filename = "task-" + taskId + "-group-" + file.getGroupIndex()
                    + (input ? ".in" : ".out");
            return new DownloadPayload(new InputStreamResource(stream), size, filename);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.DATAGEN_FILE_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public DownloadPayload downloadArchive(Long userId, Long taskId) {
        requireOwnedTask(userId, taskId);
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

    private DataGenTask requireOwnedTask(Long userId, Long taskId) {
        DataGenTask task = taskId == null ? null : dataGenTaskMapper.selectById(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            throw new BusinessException(ErrorCode.DATAGEN_TASK_NOT_FOUND);
        }
        return task;
    }

    private List<DataRangeItem> validateRanges(List<DataRangeItem> ranges) {
        if (ranges == null || ranges.isEmpty() || ranges.size() > 20) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "数据范围必须包含 1–20 个变量");
        }
        Set<String> names = new HashSet<>();
        for (DataRangeItem item : ranges) {
            String name = item.name() == null ? "" : item.name().trim();
            if (!name.matches("[A-Za-z_][A-Za-z0-9_]{0,31}")) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "变量名格式非法：" + name);
            }
            if (!names.add(name)) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "变量名重复：" + name);
            }
            String type = item.type() == null ? "" : item.type().trim().toLowerCase(Locale.ROOT);
            if (!RANGE_TYPES.contains(type)) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "变量类型不受支持：" + name);
            }
            if (item.min() == null || item.max() == null) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "变量范围不能为空：" + name);
            }
            if (item.max().compareTo(item.min()) < 0) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "变量最大值不能小于最小值：" + name);
            }
            validateTypeBounds(name, type, item.min(), item.max());
        }
        return ranges.stream()
                .map(item -> new DataRangeItem(
                        item.name().trim(),
                        item.type().trim().toLowerCase(Locale.ROOT),
                        item.min(),
                        item.max()))
                .toList();
    }

    private void validateTypeBounds(String name, String type, BigDecimal min, BigDecimal max) {
        if ("int".equals(type)) {
            if (min.compareTo(INT_MIN) < 0 || max.compareTo(INT_MAX) > 0) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "int 变量超出 32 位范围：" + name);
            }
            if (min.stripTrailingZeros().scale() > 0 || max.stripTrailingZeros().scale() > 0) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "int 变量范围必须是整数：" + name);
            }
        } else if ("long long".equals(type)) {
            if (min.compareTo(LONG_MIN) < 0 || max.compareTo(LONG_MAX) > 0) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "long long 变量超出 64 位范围：" + name);
            }
            if (min.stripTrailingZeros().scale() > 0 || max.stripTrailingZeros().scale() > 0) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "long long 变量范围必须是整数：" + name);
            }
        }
    }

    private String normalizeSpecialRequirements(String value, String mode) {
        String normalized = value == null ? "" : value.trim();
        if (MODE_RANDOM.equals(mode)) {
            if (!normalized.isEmpty()) {
                throw new BusinessException(ErrorCode.DATAGEN_MODE_INVALID,
                        "随机模式不能填写特殊构造要求");
            }
            return null;
        }
        if (normalized.length() > 2_000) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "特殊构造要求不能超过 2000 字");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private String requireText(String value, String field, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, field + "不能为空");
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM,
                    field + "不能超过 " + maxLength + " 个字符");
        }
        return normalized;
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

    private DataGenTaskListItem toListItem(DataGenTask task) {
        return new DataGenTaskListItem(
                String.valueOf(task.getId()),
                task.getTitle(),
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

    private List<DataRangeItem> parseRanges(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<DataRangeItem>>() {
            });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "任务参数序列化失败");
        }
    }
}
