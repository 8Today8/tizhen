package com.tizhen.core.infra.file;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储装配。业务代码只注入 FileStorage 接口。
 */
@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class StorageConfig {

    @Bean
    public FileStorage fileStorage(FileStorageProperties properties) {
        return new LocalFileStorage(properties.getBaseDir());
    }
}
