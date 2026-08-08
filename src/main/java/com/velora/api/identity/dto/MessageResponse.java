package com.velora.api.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simple acknowledgement")
public record MessageResponse(String message) {

    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
