package com.dmme.security;

import com.dmme.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verifies the Supabase-issued JWT on every request and puts an {@link AuthUser}
 * into the security context.
 *
 * Supports both signing schemes Supabase uses:
 *  - HS256 (legacy shared JWT secret), and
 *  - ES256 / RS256 (the newer asymmetric "JWT signing keys"), verified against
 *    the project's JWKS endpoint discovered from the token issuer.
 */
@Component
public class SupabaseJwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SupabaseJwtFilter.class);

    private final SecretKey hmacKey; // nullable — only used for HS* tokens
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    // issuer -> (kid -> public key)
    private final Map<String, Map<String, Key>> jwksCache = new ConcurrentHashMap<>();

    public SupabaseJwtFilter(AppProperties props) {
        SecretKey k = null;
        String secret = props.getSupabase().getJwtSecret();
        if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= 32) {
            try {
                k = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.warn("Supabase JWT secret present but unusable for HS256: {}", e.getMessage());
            }
        }
        this.hmacKey = k;
        log.info("SupabaseJwtFilter initialized: asymmetric JWKS verification enabled, HS256 fallback={}",
                k != null);
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
                String issuer = extractIssuer(token);
                Claims claims = Jwts.parser()
                        .keyLocator(new SupabaseKeyLocator(issuer))
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
                log.warn("JWT verification failed: {}", ex.toString());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    /** Resolves the verification key from the JWS header (alg + kid). */
    private final class SupabaseKeyLocator implements Locator<Key> {
        private final String issuer;

        private SupabaseKeyLocator(String issuer) {
            this.issuer = issuer;
        }

        @Override
        public Key locate(Header h) {
            if (!(h instanceof JwsHeader jws)) return null;
            String alg = jws.getAlgorithm();
            if (alg != null && alg.startsWith("HS")) {
                return hmacKey;
            }
            return jwkFor(issuer, jws.getKeyId());
        }
    }

    private Key jwkFor(String issuer, String kid) {
        if (issuer == null || kid == null) return null;
        Map<String, Key> keys = jwksCache.get(issuer);
        if (keys == null || !keys.containsKey(kid)) {
            keys = fetchJwks(issuer);
            if (!keys.isEmpty()) jwksCache.put(issuer, keys);
        }
        return keys.get(kid);
    }

    private Map<String, Key> fetchJwks(String issuer) {
        String base = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
        String url = base + "/.well-known/jwks.json";
        try {
            HttpResponse<String> res = http.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("JWKS fetch {} returned {}", url, res.statusCode());
                return Map.of();
            }
            JwkSet set = Jwks.setParser().build().parse(res.body());
            Map<String, Key> map = new ConcurrentHashMap<>();
            for (Jwk<?> jwk : set.getKeys()) {
                if (jwk.getId() != null) {
                    map.put(jwk.getId(), jwk.toKey());
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("Failed to fetch JWKS from {}: {}", url, e.getMessage());
            return Map.of();
        }
    }

    /** Reads the (unverified) "iss" claim so we know which JWKS to fetch. */
    private String extractIssuer(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return mapper.readTree(payload).path("iss").asText(null);
        } catch (Exception e) {
            return null;
        }
    }
}
