package com.velora.api.customer.repository;

import com.velora.api.customer.domain.CustomerAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    List<CustomerAddress> findByUserIdAndDeletedAtIsNullOrderByDefaultAddressDescIdDesc(
            Long userId);

    /**
     * Scoped by user on purpose. Looking up by id alone would let one customer read
     * another's address by changing the number in the URL — the single most common
     * real-world API vulnerability.
     */
    Optional<CustomerAddress> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    Optional<CustomerAddress> findByUserIdAndDefaultAddressTrueAndDeletedAtIsNull(Long userId);

    long countByUserIdAndDeletedAtIsNull(Long userId);

    /** Clears the flag before setting a new default — only one may be true. */
    @Modifying
    @Query("update CustomerAddress a set a.defaultAddress = false where a.user.id = :userId")
    int clearDefaultFor(@Param("userId") Long userId);
}
