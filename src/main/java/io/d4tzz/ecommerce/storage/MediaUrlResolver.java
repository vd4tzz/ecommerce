package io.d4tzz.ecommerce.storage;

import org.springframework.stereotype.Component;

/**
 * Turns the media id stored in the database into a URL a browser can open. Media ids are object keys in the bucket,
 * such as {@code products/3f2a.png}; the URL is the key under {@code app.storage.s3.public-base-url}.
 */
@Component
public class MediaUrlResolver {

    private final StorageProperties properties;

    public MediaUrlResolver(StorageProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns the public URL of the media.
     *
     * <p>A value that already is a full URL ({@code http://}, {@code https://}) or a {@code data:} or {@code blob:}
     * URI is returned unchanged.
     *
     * @param mediaId the stored media id, may be {@code null}
     * @return the URL, or {@code null} if {@code mediaId} is {@code null} or blank
     */
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
