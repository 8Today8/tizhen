package com.tizhen.core.infra.file;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 本地文件卷实现。
 * 文件名随机化并按 UTC 日期分目录；写入采用临时文件 + 原子移动，避免读到半成品。
 */
public class LocalFileStorage implements FileStorage {

    private static final DateTimeFormatter DATE_DIRECTORY_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final String TEMP_DIRECTORY = ".tmp";

    private final Path baseDir;

    public LocalFileStorage(Path baseDir) {
        this.baseDir = baseDir.toAbsolutePath().normalize();
    }

    @PostConstruct
    public void initialize() throws IOException {
        Files.createDirectories(baseDir);
        Files.createDirectories(baseDir.resolve(TEMP_DIRECTORY));
    }

    @Override
    public StoredFile store(InputStream content, String originalFilename) throws IOException {
        String dateDirectory = DATE_DIRECTORY_FORMAT.format(LocalDate.now(ZoneOffset.UTC));
        String objectKey = dateDirectory + "/" + UUID.randomUUID() + extractExtension(originalFilename);
        Path target = resolve(objectKey);
        Files.createDirectories(target.getParent());

        Path temporaryFile = Files.createTempFile(baseDir.resolve(TEMP_DIRECTORY), "upload-", ".part");
        try {
            Files.copy(content, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            moveAtomically(temporaryFile, target);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
        return new StoredFile(objectKey, Files.size(target));
    }

    @Override
    public InputStream open(String objectKey) throws IOException {
        return Files.newInputStream(resolve(objectKey));
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.isRegularFile(resolve(objectKey));
    }

    @Override
    public void delete(String objectKey) throws IOException {
        Files.deleteIfExists(resolve(objectKey));
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path resolve(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("文件标识不能为空");
        }
        Path resolved = baseDir.resolve(objectKey).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException("文件标识非法");
        }
        return resolved;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        String filename = originalFilename.replace('\\', '/');
        int slashIndex = filename.lastIndexOf('/');
        if (slashIndex >= 0) {
            filename = filename.substring(slashIndex + 1);
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return extension.matches("[a-z0-9]{1,10}") ? "." + extension : "";
    }
}
