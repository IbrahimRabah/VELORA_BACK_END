package com.velora.api.cart.web;

import com.velora.api.cart.dto.AddToCartRequest;
import com.velora.api.cart.dto.CartResponse;
import com.velora.api.cart.dto.GuestTokenResponse;
import com.velora.api.cart.dto.MergeCartRequest;
import com.velora.api.cart.dto.UpdateCartItemRequest;
import com.velora.api.cart.security.GuestTokenService;
import com.velora.api.cart.service.CartService;
import com.velora.api.catalog.web.LocaleResolver;
import com.velora.api.identity.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cart endpoints. Public: guest checkout is supported, so a token is optional.
 *
 * <p>Anonymous callers identify their cart with {@code X-Guest-Token} — a UUID the
 * Angular app generates once and keeps in a cookie. Signed-in callers are identified
 * by their token and the header is ignored.
 */
@Tag(name = "Cart", description = "Shopping cart. Works signed in or as a guest.")
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private static final String GUEST_HEADER = "X-Guest-Token";

    private final CartService cartService;
    private final GuestTokenService guestTokenService;

    public CartController(CartService cartService, GuestTokenService guestTokenService) {
        this.cartService = cartService;
        this.guestTokenService = guestTokenService;
    }

    @Operation(summary = "Issue a guest token",
            description = """
                    Call once, before the first cart action, if the browser does not
                    already have an X-Guest-Token. Store the result (a cookie, same as
                    before) and send it back as X-Guest-Token on every subsequent cart,
                    shipping-quote and checkout call.

                    The token is signed server-side, so it cannot be forged for an id
                    the caller does not already hold — knowing or guessing someone
                    else's UUID is no longer enough to read or edit their cart.
                    """,
            security = {})
    @PostMapping("/guest-token")
    public GuestTokenResponse issueGuestToken() {
        return new GuestTokenResponse(guestTokenService.generate());
    }

    @Operation(summary = "Get the cart",
            description = """
                    Recalculated server-side on every call. Prices come from the CURRENT
                    variant price, not from what was stored when the item was added, and
                    `warnings` reports anything that changed.

                    The cart is a proposal, never a promise — nothing here holds stock.
                    """,
            security = {})
    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal UserPrincipal principal,
                                @Parameter(description = "Required when not signed in")
                                @RequestHeader(value = GUEST_HEADER, required = false)
                                String guestToken,
                                HttpServletRequest request) {

        return cartService.getCart(userId(principal), guestToken, LocaleResolver.resolve(request));
    }

    @Operation(summary = "Add a variant to the cart",
            description = "Send the VARIANT id, never the product id.",
            security = {})
    @ApiResponse(responseCode = "409", description = "Not enough stock, or not for sale")
    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal UserPrincipal principal,
                                @RequestHeader(value = GUEST_HEADER, required = false)
                                String guestToken,
                                @Valid @RequestBody AddToCartRequest body,
                                HttpServletRequest request) {

        return cartService.addItem(userId(principal), guestToken, body,
                LocaleResolver.resolve(request));
    }

    @Operation(summary = "Change a line's quantity", security = {})
    @PatchMapping("/items/{itemId}")
    public CartResponse updateItem(@AuthenticationPrincipal UserPrincipal principal,
                                   @RequestHeader(value = GUEST_HEADER, required = false)
                                   String guestToken,
                                   @PathVariable Long itemId,
                                   @Valid @RequestBody UpdateCartItemRequest body,
                                   HttpServletRequest request) {

        return cartService.updateQuantity(userId(principal), guestToken, itemId,
                body.quantity(), LocaleResolver.resolve(request));
    }

    @Operation(summary = "Remove a line", security = {})
    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(@AuthenticationPrincipal UserPrincipal principal,
                                   @RequestHeader(value = GUEST_HEADER, required = false)
                                   String guestToken,
                                   @PathVariable Long itemId,
                                   HttpServletRequest request) {

        return cartService.removeItem(userId(principal), guestToken, itemId,
                LocaleResolver.resolve(request));
    }

    @Operation(summary = "Empty the cart", security = {})
    @DeleteMapping
    public CartResponse clear(@AuthenticationPrincipal UserPrincipal principal,
                              @RequestHeader(value = GUEST_HEADER, required = false)
                              String guestToken,
                              HttpServletRequest request) {

        return cartService.clear(userId(principal), guestToken, LocaleResolver.resolve(request));
    }

    @Operation(summary = "Merge a guest cart after signing in",
            description = """
                    Call this ONCE, right after login, with the guest token the browser was
                    using. Quantities are added together and capped at available stock.
                    Clear the guest cookie afterwards.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/merge")
    public CartResponse merge(@AuthenticationPrincipal UserPrincipal principal,
                              @Valid @RequestBody MergeCartRequest body,
                              HttpServletRequest request) {

        return cartService.mergeGuestCart(principal.id(), body.guestToken(),
                LocaleResolver.resolve(request));
    }

    private Long userId(UserPrincipal principal) {
        return principal == null ? null : principal.id();
    }
}
