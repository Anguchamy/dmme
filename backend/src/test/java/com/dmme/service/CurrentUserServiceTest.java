package com.dmme.service;

import com.dmme.domain.AppUser;
import com.dmme.repository.AppUserRepository;
import com.dmme.security.AuthUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    private static final UUID USER_ID = UUID.fromString("f460a726-9d61-4a16-a896-ebf8a72a51f6");
    private static final String EMAIL = "creator@example.com";

    @Mock private AppUserRepository users;

    private CurrentUserService service;

    @BeforeEach
    void setUp() {
        service = new CurrentUserService(users);
        AuthUser principal = new AuthUser(USER_ID.toString(), EMAIL);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireReturnsExistingUserWithoutInsert() {
        AppUser existing = new AppUser();
        existing.setId(USER_ID);
        existing.setEmail(EMAIL);
        when(users.findById(USER_ID)).thenReturn(Optional.of(existing));

        assertSame(existing, service.require());
        verify(users, never()).insertIgnoreConflict(USER_ID, EMAIL);
    }

    @Test
    void requireProvisionsViaInsertIgnoreThenRefetches() {
        AppUser provisioned = new AppUser();
        provisioned.setId(USER_ID);
        provisioned.setEmail(EMAIL);

        when(users.findById(USER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(provisioned));
        when(users.insertIgnoreConflict(USER_ID, EMAIL)).thenReturn(1);

        assertEquals(USER_ID, service.require().getId());
        verify(users).insertIgnoreConflict(USER_ID, EMAIL);
    }

    @Test
    void requireSurvivesConcurrentInsertRace() {
        // First find misses; insert is a no-op (other request won); refetch hits.
        AppUser winner = new AppUser();
        winner.setId(USER_ID);
        winner.setEmail(EMAIL);

        when(users.findById(USER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(users.insertIgnoreConflict(USER_ID, EMAIL)).thenReturn(0);

        assertSame(winner, service.require());
    }
}
