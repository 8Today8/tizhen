package com.tizhen.core.infra.file;

/**
 * 文件写入结果。objectKey 为存储层相对路径，不直接暴露给前端。
 */
public record StoredFile(String objectKey, long sizeBytes) {
}
