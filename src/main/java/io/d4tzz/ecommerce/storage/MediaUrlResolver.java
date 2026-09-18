package io.d4tzz.ecommerce.storage;

import org.springframework.stereotype.Component;

@Component
public class MediaUrlResolver {

    private final StorageProperties properties;

    public MediaUrlResolver(StorageProperties properties) {
        this.properties = properties;
    }

    public String resolveUrl(String mediaId) {
        if (mediaId == null || mediaId.isBlank()) {
            return null;
        }
        if (mediaId.startsWith("http://") || mediaId.startsWith("https://") || mediaId.startsWith("data:") || mediaId.startsWith("blob:")) {
            return mediaId;
        }
        String baseUrl = properties.publicBaseUrl().replaceAll("/+$", "");
        String cleanPath = mediaId.replaceAll("^/+", "");
        return baseUrl + "/" + cleanPath;
    }
}
