package com.velora.api.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * File storage behind one interface.
 *
 * <p>V1 stores on local disk. Moving to Cloudflare R2 or S3 later means adding one
 * implementation class — no caller changes, because nothing above this interface
 * knows where bytes actually live.
 *
 * <p>Store the {@link StoredFile#key()} in the database, never the full URL. The URL
 * contains the host and CDN prefix, both of which change; the key does not. Build
 * the URL at read time with {@link #urlFor(String)}.
 */
public interface StorageService {

    /**
     * @param folder logical folder, e.g. {@code products} or {@code categories}
     * @throws com.velora.api.common.exception.BusinessException on invalid type or size
     */
    StoredFile store(MultipartFile file, String folder);

    /** Silently does nothing if the key does not exist — delete is idempotent. */
    void delete(String key);

    String urlFor(String key);

    boolean exists(String key);
}
