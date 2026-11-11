package com.tizhen.core.infra.file;

import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * 头像图片的校验、存储与读取。文件仍由 {@link FileStorage} 落盘，
 * 对外只暴露受控的媒体访问路径，避免泄漏存储层目录结构。
 */
@Service
public class AvatarStorageService {

    public static final String PUBLIC_PATH_PREFIX = "/api/v1/media/avatar/";

    private static final Logger log = LoggerFactory.getLogger(AvatarStorageService.class);
    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private static final Pattern STORED_KEY_PATTERN = Pattern.compile(
            "^\\d{4}/\\d{2}/\\d{2}/[0-9a-fA-F-]{36}\\.(jpg|png)$");
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final FileStorage fileStorage;

    public AvatarStorageService(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    /**
     * 校验并保存头像，返回可直接用于 img src 的相对地址。
     */
    public String store(InputStream content, long declaredSize, String originalFilename) {
        if (declaredSize <= 0) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "请选择要上传的头像图片");
        }
        if (declaredSize > MAX_AVATAR_BYTES) {
            throw new BusinessException(ErrorCode.COMMON_PAYLOAD_TOO_LARGE, "头像图片不能超过 2MB");
        }

        byte[] bytes;
        try {
            bytes = content.readAllBytes();
        } catch (IOException exception) {
            log.warn("读取头像上传内容失败", exception);
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "头像上传失败，请稍后重试");
        }
        if (bytes.length == 0) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "请选择要上传的头像图片");
        }
        if (bytes.length > MAX_AVATAR_BYTES) {
            throw new BusinessException(ErrorCode.COMMON_PAYLOAD_TOO_LARGE, "头像图片不能超过 2MB");
        }

        ImageFormat format = detectFormat(bytes);
        validateDimensions(bytes);

        try {
            StoredFile stored = fileStorage.store(
                    new ByteArrayInputStream(bytes), "avatar." + format.extension());
            return PUBLIC_PATH_PREFIX + stored.objectKey();
        } catch (IOException exception) {
            log.error("头像文件写入失败 originalFilename={}", originalFilename, exception);
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "头像保存失败，请稍后重试");
        }
    }

    /**
     * 仅接受本服务签发的头像地址，null/空白表示清除头像。
     */
    public String normalizeAvatarUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String normalized = rawUrl.trim();
        if (normalized.length() > 512
                || !normalized.startsWith(PUBLIC_PATH_PREFIX)
                || !isStoredKey(normalized.substring(PUBLIC_PATH_PREFIX.length()))) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_FAILED, "头像地址不合法");
        }
        return normalized;
    }

    public AvatarContent read(String objectKey) {
        if (!isStoredKey(objectKey)) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "头像不存在");
        }
        if (!fileStorage.exists(objectKey)) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "头像不存在");
        }
        try (InputStream input = fileStorage.open(objectKey)) {
            MediaType mediaType = objectKey.endsWith(".png")
                    ? MediaType.PNG
                    : MediaType.JPEG;
            return new AvatarContent(mediaType.contentType(), input.readAllBytes());
        } catch (IOException exception) {
            log.warn("读取头像文件失败 objectKey={}", objectKey, exception);
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "头像不存在");
        }
    }

    /**
     * 删除被替换的旧头像；清理失败不影响业务结果。
     */
    public void deleteByUrl(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(PUBLIC_PATH_PREFIX)) {
            return;
        }
        String objectKey = avatarUrl.substring(PUBLIC_PATH_PREFIX.length());
        if (!isStoredKey(objectKey)) {
            return;
        }
        try {
            fileStorage.delete(objectKey);
        } catch (IOException exception) {
            log.warn("清理旧头像失败 objectKey={}", objectKey, exception);
        }
    }

    private boolean isStoredKey(String objectKey) {
        return objectKey != null && STORED_KEY_PATTERN.matcher(objectKey).matches();
    }

    private ImageFormat detectFormat(byte[] bytes) {
        if (bytes.length >= PNG_SIGNATURE.length) {
            boolean png = true;
            for (int i = 0; i < PNG_SIGNATURE.length; i++) {
                if (bytes[i] != PNG_SIGNATURE[i]) {
                    png = false;
                    break;
                }
            }
            if (png) {
                return ImageFormat.PNG;
            }
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return ImageFormat.JPEG;
        }
        throw new BusinessException(ErrorCode.COMMON_UNSUPPORTED_MEDIA_TYPE, "仅支持 JPG/PNG 格式的头像");
    }

    private void validateDimensions(byte[] bytes) {
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (imageInput == null) {
                throw unsupportedImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw unsupportedImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new BusinessException(
                            ErrorCode.COMMON_VALIDATION_FAILED,
                            "头像尺寸不能超过 " + MAX_DIMENSION + "×" + MAX_DIMENSION);
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            log.warn("解析头像图片失败", exception);
            throw unsupportedImage();
        }
    }

    private BusinessException unsupportedImage() {
        return new BusinessException(ErrorCode.COMMON_UNSUPPORTED_MEDIA_TYPE, "仅支持 JPG/PNG 格式的头像");
    }

    private enum ImageFormat {
        JPEG("jpg"),
        PNG("png");

        private final String extension;

        ImageFormat(String extension) {
            this.extension = extension;
        }

        String extension() {
            return extension;
        }
    }

    private enum MediaType {
        JPEG("image/jpeg"),
        PNG("image/png");

        private final String contentType;

        MediaType(String contentType) {
            this.contentType = contentType;
        }

        String contentType() {
            return contentType;
        }
    }

    public record AvatarContent(String contentType, byte[] bytes) {
    }
}
