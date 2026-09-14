package io.d4tzz.ecommerce.storage;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;

public record PresignedUploadRequest(String contentType, Long lengthByte) {
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
