package io.d4tzz.ecommerce.storage;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
