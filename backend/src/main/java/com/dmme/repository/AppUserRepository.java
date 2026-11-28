package com.dmme.repository;

import com.dmme.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);

    /**
     * JIT-provision without racing: concurrent first-login requests both call this;
     * the loser hits ON CONFLICT and becomes a no-op instead of violating app_user_pkey.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            insert into app_user (id, email, plan_code, created_at, updated_at)
            values (:id, :email, 'FREE', now(), now())
            on conflict (id) do nothing
            """, nativeQuery = true)
    int insertIgnoreConflict(@Param("id") UUID id, @Param("email") String email);
}
