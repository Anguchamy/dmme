package com.dmme.service;

import com.dmme.domain.AppUser;
import com.dmme.repository.AppUserRepository;
import com.dmme.security.AuthUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Resolves the authenticated {@link AppUser}, lazily provisioning a row the
 * first time a Supabase-authenticated user hits the API (JIT provisioning).
 */
@Service
public class CurrentUserService {

    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    public AuthUser principal() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof AuthUser au) {
            return au;
        }
        throw new IllegalStateException("No authenticated user");
    }

    public UUID userId() {
        return UUID.fromString(principal().userId());
    }

    @Transactional
    public AppUser require() {
        AuthUser au = principal();
        UUID id = UUID.fromString(au.userId());
        return users.findById(id).orElseGet(() -> provision(au, id));
    }

    private AppUser provision(AuthUser au, UUID id) {
        String email = au.email() != null ? au.email() : id + "@placeholder.local";
        // Concurrent /api/me (and other) calls on first login used to both INSERT and
        // blow up on app_user_pkey. Insert-ignore + refetch is safe under that race.
        users.insertIgnoreConflict(id, email);
        return users.findById(id)
                .orElseThrow(() -> new IllegalStateException("Failed to provision app_user " + id));
    }
}
