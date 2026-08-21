package com.velora.api.customer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.velora.api.customer.domain.CustomerAddress;
import com.velora.api.customer.dto.AddressRequest;
import com.velora.api.customer.dto.AddressResponse;
import com.velora.api.customer.repository.CustomerAddressRepository;
import com.velora.api.identity.domain.AppUser;
import com.velora.api.identity.repository.AppUserRepository;
import com.velora.api.shipping.domain.Governorate;
import com.velora.api.shipping.repository.GovernorateRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@code customer_address.city_id} was left over {@code BIGINT NOT NULL} from an
 * earlier design, but the business decision — captured in {@code AddressRequest} —
 * is that city is free text folded into {@code area}, and the governorate alone
 * drives shipping. {@code AddressRequest} has no {@code cityId} field at all, so
 * every {@code POST /api/v1/me/addresses} failed with a NOT NULL violation.
 *
 * <p>V7 migration relaxes the column to nullable; this runs against the real
 * database to prove the save actually succeeds now, not just that the entity graph
 * compiles.
 */
@SpringBootTest
class AddressServiceIntegrationTest {

    @Autowired private AddressService addressService;
    @Autowired private AppUserRepository userRepository;
    @Autowired private GovernorateRepository governorateRepository;
    @Autowired private CustomerAddressRepository addressRepository;
    @Autowired private JdbcTemplate jdbc;

    private Long userId;
    private Long governorateId;

    @BeforeEach
    void createUserWithServedGovernorate() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        AppUser user = new AppUser();
        user.setEmail("addr-test-" + unique + "@example.com");
        user.setPasswordHash("not-a-real-hash");
        user.setFirstName("Test");
        userId = userRepository.save(user).getId();

        // Any governorate that actually has a shipping rate — AddressService refuses
        // to save an address for one that does not.
        governorateId = governorateRepository.findByCode("CAI")
                .map(Governorate::getId)
                .orElseGet(() -> governorateRepository
                        .findByActiveTrueOrderByDisplayOrderAsc().get(0).getId());
    }

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM customer_address WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM app_user WHERE id = ?", userId);
    }

    @Test
    @DisplayName("Creating an address with no cityId succeeds — the request has no such field")
    void create_withoutCityId_succeeds() {
        AddressRequest request = new AddressRequest(
                "HOME",
                "محمد أحمد",
                "01012345678",
                null,
                governorateId,
                "المنيا الجديدة",
                "شارع الجمهورية",
                "12",
                "3",
                "5",
                "بجوار مسجد النور",
                true);

        AddressResponse response = addressService.create(userId, request, "ar");

        assertThat(response.id()).isNotNull();
        assertThat(response.governorateId()).isEqualTo(governorateId);
        assertThat(response.isDefault()).isTrue();

        CustomerAddress persisted = addressRepository.findById(response.id()).orElseThrow();
        assertThat(persisted.getCityId())
                .as("cityId is never supplied by the request and must stay null")
                .isNull();
    }
}
