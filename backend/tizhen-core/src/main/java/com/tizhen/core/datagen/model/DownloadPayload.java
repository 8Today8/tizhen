package com.tizhen.core.datagen.model;

import org.springframework.core.io.Resource;

/**
 * 文件下载载荷。文件名只包含任务、组号与扩展名，不暴露服务器路径。
 */
public record DownloadPayload(
        Resource resource,
        long contentLength,
        String filename
) {
}
