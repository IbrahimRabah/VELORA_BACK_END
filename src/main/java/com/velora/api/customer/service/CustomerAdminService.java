package com.velora.api.customer.service;

import com.velora.api.common.dto.PageResponse;
import com.velora.api.common.exception.BusinessException;
import com.velora.api.common.exception.ErrorCode;
import com.velora.api.common.util.PhoneNormalizer;
import com.velora.api.customer.dto.CustomerDetailResponse;
import com.velora.api.customer.dto.CustomerSummaryResponse;
import com.velora.api.customer.repository.CustomerAddressRepository;
import com.velora.api.customer.repository.CustomerQueries;
import com.velora.api.identity.domain.AppUser;
import com.velora.api.identity.repository.AppUserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The customer list and one customer's history.
 *
 * <p>The purchase summary is the point. A name and a phone number is a contact list;
 * knowing that this person has ordered four times, refused delivery twice and spent
 * nine thousand pounds is what lets staff decide how to handle the call.
 */
@Service
public class CustomerAdminService {

    private final AppUserRepository userRepository;
    private final CustomerAddressRepository addressRepository;
    private final CustomerQueries queries;

    public CustomerAdminService(AppUserRepository userRepository,
                                CustomerAddressRepository addressRepository,
                                CustomerQueries queries) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.queries = queries;
    }

    /**
     * Customers with their purchase totals.
     *
     * <p>{@code search} matches a name, a phone or an email. Phone input is normalized
     * first, so {@code 01012345678} finds a customer stored as {@code +201012345678} —
     * without that, searching by the number a customer reads out over the phone would
     * find nothing.
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerSummaryResponse> list(String search, String sort,
                                                      Pageable pageable) {
        String normalizedSearch = normalizeSearch(search);
        return queries.findCustomers(normalizedSearch, sort, pageable);
    }

    @Transactional(readOnly = true)
    public CustomerDetailResponse get(Long customerId) {
        AppUser user = userRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Customer not found"));

        CustomerQueries.PurchaseTotals totals = queries.purchaseTotals(customerId);

        BigDecimal average = totals.totalOrders() == 0
                ? BigDecimal.ZERO
                : totals.totalSpent().divide(
                        BigDecimal.valueOf(totals.totalOrders()), 2, RoundingMode.HALF_UP);

        List<CustomerDetailResponse.AddressLine> addresses = addressRepository
                .findByUserIdAndDeletedAtIsNullOrderByDefaultAddressDescIdDesc(customerId)
                .stream()
                .map(address -> new CustomerDetailResponse.AddressLine(
                        address.getId(),
                        address.getLabel(),
                        address.getRecipientName(),
                        PhoneNormalizer.toLocalFormat(address.getPhoneE164()),
                        address.getGovernorate().getNameAr(),
                        address.formatted(),
                        address.isDefaultAddress()))
                .toList();

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getCode())
                .toList();

        return new CustomerDetailResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                PhoneNormalizer.toLocalFormat(user.getPhoneE164()),
                user.getEmail(),
                user.isPhoneVerified(),
                user.isEmailVerified(),
                user.getStatus().name(),
                user.getPreferredLocale(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                roles,
                new CustomerDetailResponse.Purchases(
                        totals.totalOrders(),
                        totals.deliveredOrders(),
                        totals.failedOrders(),
                        totals.cancelledOrders(),
                        totals.totalSpent(),
                        average,
                        totals.firstOrderAt(),
                        totals.lastOrderAt()),
                addresses,
                queries.recentOrders(customerId, 10));
    }

    // ------------------------------------------------------------------ internal

    /**
     * A phone number typed by staff is normalized before searching; anything else is
     * passed through as a text match.
     */
    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String trimmed = search.trim();

        if (trimmed.matches("[0-9+\\s-]{7,}")) {
            String e164 = PhoneNormalizer.toE164(trimmed);
            return e164 != null ? e164 : trimmed;
        }
        return trimmed;
    }

    /** Used by the identity module when suspending an account. */
    @Transactional(readOnly = true)
    public Optional<AppUser> findByPhone(String phone) {
        String e164 = PhoneNormalizer.toE164(phone);
        return e164 == null ? Optional.empty() : userRepository.findByPhoneE164(e164);
    }
}
