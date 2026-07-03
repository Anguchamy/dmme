package com.dmme.repository;

import com.dmme.domain.InstagramAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstagramAccountRepository extends JpaRepository<InstagramAccount, Long> {
    List<InstagramAccount> findByUserId(UUID userId);
    Optional<InstagramAccount> findByIgUserId(String igUserId);
}
