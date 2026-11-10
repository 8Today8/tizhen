package com.tizhen.core.infra.file;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件存储抽象。首期使用本地文件卷，后续可替换为 MinIO/S3 而不影响业务代码。
 */
public interface FileStorage {

    StoredFile store(InputStream content, String originalFilename) throws IOException;

    InputStream open(String objectKey) throws IOException;

    boolean exists(String objectKey);

    void delete(String objectKey) throws IOException;
}
