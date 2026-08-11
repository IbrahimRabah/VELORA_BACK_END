package com.velora.api.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A freshly issued, signed guest cart identity")
public record GuestTokenResponse(

        @Schema(description = "Send this back as X-Guest-Token on every subsequent "
                + "cart, shipping-quote and checkout call")
        String guestToken
) {
}
