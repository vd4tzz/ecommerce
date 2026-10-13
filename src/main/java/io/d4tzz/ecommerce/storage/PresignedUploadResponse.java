package io.d4tzz.ecommerce.storage;

import java.time.Instant;

/**
 * Where and how to upload a file.
 *
 * @param key              the media id to send when adding the image to a product
 * @param uploadUrl        the signed URL to send the file to with an HTTP {@code PUT}
 * @param publicUrl        the URL the image can be read from once uploaded
 * @param expiresInSeconds how long {@code uploadUrl} is valid, in seconds
 * @param expiresAt        the moment {@code uploadUrl} stops being valid
 */
public record PresignedUploadResponse(
    String key,
    String uploadUrl,
    String publicUrl,
    long expiresInSeconds,
    Instant expiresAt
) {}
