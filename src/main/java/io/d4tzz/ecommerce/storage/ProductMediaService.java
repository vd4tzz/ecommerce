package io.d4tzz.ecommerce.storage;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Handles product images in the object store. The application never receives the image: it signs a URL, and the client
 * uploads the file straight to the store.
 */
@Service
public class ProductMediaService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
        "image/jpeg", ".jpg",
        "image/jpg", ".jpg",
        "image/png", ".png",
        "image/webp", ".webp",
        "image/gif", ".gif",
        "image/svg+xml", ".svg",
        "image/avif", ".avif"
    );

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final StorageProperties properties;
    private final MediaUrlResolver urlResolver;

    public ProductMediaService(
        S3Presigner s3Presigner,
        S3Client s3Client,
        StorageProperties properties,
        MediaUrlResolver urlResolver
    ) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
        this.properties = properties;
        this.urlResolver = urlResolver;
    }

    /**
     * Validates an upload request and signs a URL to upload one image.
     *
     * <p>The image gets a new key, {@code products/{random UUID}{extension}}, where the extension comes from the content
     * type. The URL is valid for {@code app.storage.s3.presigned-duration-minutes}. Nothing is written to the store yet.
     *
     * @param request the content type and size of the file
     * @return the key, the signed upload URL, the public URL and the expiry
     * @throws InvalidArgumentException if the file is larger than 10 MB, or the content type is not an allowed image
     *                                  type (jpeg, png, webp, gif, svg or avif)
     */
    public PresignedUploadResponse generatePresignedUploadUrl(PresignedUploadRequest request) {
        if (request.lengthByte() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidArgumentException(
                "File size exceeds maximum allowed limit of " + MAX_FILE_SIZE_BYTES / (1024 * 1024) + "MB"
            );
        }

        String extension = MIME_TO_EXTENSION.get(request.contentType());
        if (extension == null) {
            throw new InvalidArgumentException(
                "Unsupported content type: " + request.contentType() + ". Allowed types: " + MIME_TO_EXTENSION.keySet()
            );
        }

        String mediaId = "products/" + UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(properties.bucket())
            .key(mediaId)
            .contentType(request.contentType())
            .contentLength(request.lengthByte())
            .build();

        Duration duration = Duration.ofMinutes(properties.presignedDurationMinutes());

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(duration)
            .putObjectRequest(putObjectRequest)
            .build();

        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);

        String uploadUrl = presignedPutObjectRequest.url().toString();
        Instant expiresAt = presignedPutObjectRequest.expiration();
        String publicUrl = resolvePublicUrl(mediaId);

        return new PresignedUploadResponse(mediaId, uploadUrl, publicUrl, duration.toSeconds(), expiresAt);
    }

    /**
     * Tells whether an object with this key exists in the bucket.
     *
     * @param key the media id, may be {@code null}
     * @return {@code false} if the key is blank or the object is not found
     * @throws software.amazon.awssdk.services.s3.model.S3Exception if the store fails for any other reason
     */
    public boolean exists(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    private String resolvePublicUrl(String mediaId) {
        return urlResolver.resolveUrl(mediaId);
    }
}
