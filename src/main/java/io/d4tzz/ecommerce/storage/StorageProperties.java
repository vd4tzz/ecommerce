package io.d4tzz.ecommerce.storage;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the object store, read from {@code app.storage.s3}. All of them are required.
 *
 * @param bucket                   the bucket that holds the images
 * @param region                   the region name given to the S3 client; S3-compatible servers usually ignore it
 * @param endpoint                 the address of the S3 API the application talks to
 * @param accessKey                the access key of the store
 * @param secretKey                the secret key of the store
 * @param publicBaseUrl            the address browsers read images from, which {@link MediaUrlResolver} puts in front of a key
 * @param presignedDurationMinutes how long a signed upload URL stays valid, in minutes
 */
@ConfigurationProperties(prefix = "app.storage.s3")
public record StorageProperties(
    String bucket,
    String region,
    String endpoint,
    String accessKey,
    String secretKey,
    String publicBaseUrl,
    Integer presignedDurationMinutes
) {
    public StorageProperties {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(region, "region must not be null");
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        Objects.requireNonNull(accessKey, "accessKey must not be null");
        Objects.requireNonNull(secretKey, "secretKey must not be null");
        Objects.requireNonNull(publicBaseUrl, "publicBaseUrl must not be null");
        Objects.requireNonNull(presignedDurationMinutes, "presignedDurationMinutes must not be null");
    }
}
