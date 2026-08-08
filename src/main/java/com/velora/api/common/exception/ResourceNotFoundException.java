package com.velora.api.common.exception;

import java.io.Serial;

/**
 * A requested entity does not exist, or the caller is not allowed to see it.
 *
 * <p>Deliberately does not distinguish those two cases. Returning 403 for
 * "exists but not yours" and 404 for "does not exist" lets an attacker enumerate
 * which order ids are real.
 */
public class ResourceNotFoundException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String resource, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resource + " not found: " + id);
    }

    public ResourceNotFoundException(ErrorCode code) {
        super(code);
    }

    public ResourceNotFoundException(ErrorCode code, String detail) {
        super(code, detail);
    }
}
