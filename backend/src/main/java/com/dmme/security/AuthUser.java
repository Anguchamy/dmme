package com.dmme.security;

/** Authenticated principal derived from the Supabase JWT. */
public record AuthUser(String userId, String email) {
}
