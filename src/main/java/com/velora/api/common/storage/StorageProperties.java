package com.velora.api.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from {@code velora.storage.*} in application.yml.
 *
 * <p>{@code localPath} deliberately lives OUTSIDE the project folder so that
 * {@code mvnw clean} cannot delete uploaded images and Git cannot accidentally
 * track them.
 */
@ConfigurationProperties(prefix = "velora.storage")
public class StorageProperties {

    /** Absolute path on disk, e.g. D:/ibrahim_watches/velora-uploads */
    private String localPath = "./velora-uploads";

    /** Prefix prepended to the key to build a public URL. */
    private String publicUrlPrefix = "http://localhost:8080/uploads";

    private long maxFileSizeBytes = 5L * 1024 * 1024;

    private String[] allowedContentTypes = {
            "image/jpeg", "image/png", "image/webp", "image/avif"
    };

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getPublicUrlPrefix() {
        return publicUrlPrefix;
    }

    public void setPublicUrlPrefix(String publicUrlPrefix) {
        this.publicUrlPrefix = publicUrlPrefix;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public String[] getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(String[] allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }
}
