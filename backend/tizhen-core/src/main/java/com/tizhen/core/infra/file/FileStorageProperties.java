package com.tizhen.core.infra.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地文件卷配置。baseDir 保留字符串形式，避免 Spring Boot 对含 ".." 的
 * 相对路径做资源路径归一化时拒绝绑定；目录解析与规范化由 LocalFileStorage 完成。
 */
@ConfigurationProperties(prefix = "tizhen.storage")
public class FileStorageProperties {

    /**
     * 文件根目录。不得位于 Web 静态资源目录内。
     */
    private String baseDir = "data/storage";

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }
}
