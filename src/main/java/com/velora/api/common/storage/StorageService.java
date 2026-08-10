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

    /**
     * Stores bytes the application generated rather than a customer uploaded.
     *
     * <p>Separate from {@link #store} because generated files skip the upload
     * validation entirely: a PDF this service just rendered does not need its type
     * or size checked against what a browser is allowed to send.
     *
     * @param filename the exact name to use, e.g. {@code VLR-INV-2026-000001.pdf}
     * @return the storage key
     */
    String storeBytes(byte[] content, String folder, String filename, String contentType);

    /** Silently does nothing if the key does not exist — delete is idempotent. */
    void delete(String key);

    String urlFor(String key);

    boolean exists(String key);
}
