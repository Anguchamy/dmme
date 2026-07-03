package com.dmme.service;

import com.dmme.domain.AppUser;
import com.dmme.repository.AppUserRepository;
import com.dmme.security.AuthUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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

    public AppUser require() {
        AuthUser au = principal();
        UUID id = UUID.fromString(au.userId());
        return users.findById(id).orElseGet(() -> {
            AppUser u = new AppUser();
            u.setId(id);
            u.setEmail(au.email() != null ? au.email() : id + "@placeholder.local");
            return users.save(u);
        });
    }
}
