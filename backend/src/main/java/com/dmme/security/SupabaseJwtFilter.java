package com.dmme.security;

import com.dmme.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Verifies the Supabase-issued JWT (HS256, signed with the project's JWT secret)
 * on every request and puts an {@link AuthUser} into the security context.
 */
@Component
public class SupabaseJwtFilter extends OncePerRequestFilter {

    private final SecretKey key;

    public SupabaseJwtFilter(AppProperties props) {
        this.key = Keys.hmacShaKeyFor(
                props.getSupabase().getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                String email = claims.get("email", String.class);

                AuthUser principal = new AuthUser(userId, email);
                var auth = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                // Invalid/expired token -> leave unauthenticated; endpoint security decides.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
