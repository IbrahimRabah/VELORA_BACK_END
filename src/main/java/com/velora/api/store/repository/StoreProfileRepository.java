package com.velora.api.store.repository;

import com.velora.api.store.domain.StoreProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreProfileRepository extends JpaRepository<StoreProfile, Integer> {
}
