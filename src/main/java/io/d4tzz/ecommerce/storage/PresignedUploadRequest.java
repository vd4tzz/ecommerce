package io.d4tzz.ecommerce.storage;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;

/**
 * What the client wants to upload, sent to ask for an upload URL.
 *
 * @param contentType the MIME type of the file, for example {@code image/png}; trimmed and lowercased
 * @param lengthByte  the size of the file in bytes, greater than zero
 */
public record PresignedUploadRequest(String contentType, Long lengthByte) {
    /**
     * @throws InvalidArgumentException if the content type is blank or the size is missing or not greater than zero
     */
    public PresignedUploadRequest {
        if (contentType == null || contentType.isBlank()) {
            throw new InvalidArgumentException("");
        }
        if (lengthByte == null || lengthByte <= 0) {
            throw new InvalidArgumentException("");
        }

        contentType = contentType.toLowerCase().trim();
    }
}
