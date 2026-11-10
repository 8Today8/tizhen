package com.tizhen.core.infra.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * 本地文件卷配置。
 */
@ConfigurationProperties(prefix = "tizhen.storage")
public class FileStorageProperties {

    /**
     * 文件根目录。不得位于 Web 静态资源目录内。
     */
    private Path baseDir = Path.of("data", "storage");

    public Path getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(Path baseDir) {
        this.baseDir = baseDir;
    }
}
