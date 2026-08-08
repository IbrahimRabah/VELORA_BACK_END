package com.velora.api.common.storage;

/**
 * The result of storing a file.
 *
 * @param key         storage key, e.g. {@code products/2026/08/a1b2c3.jpg} — this is
 *                    what goes in the database, NOT the URL
 * @param url         publicly reachable URL, built from the key at read time
 * @param sizeBytes   size of the stored file
 * @param contentType detected content type
 */
public record StoredFile(String key, String url, long sizeBytes, String contentType) {
}
