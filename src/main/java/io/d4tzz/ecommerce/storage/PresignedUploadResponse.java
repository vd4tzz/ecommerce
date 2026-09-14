package io.d4tzz.ecommerce.storage;

import java.time.Instant;

public record PresignedUploadResponse(
    String key,
    String uploadUrl,
    String publicUrl,
    long expiresInSeconds,
    Instant expiresAt
) {}
