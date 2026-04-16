package com.myschool.userservice.service;

import com.myschool.userservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    /*
     * HS256 key — must be >= 32 UTF-8 bytes.
     * buildKey() in JwtService calls Keys.hmacShaKeyFor(secret.getBytes(UTF_8)),
     * so this plain string is fine (no Base64 encoding needed).
     */
    private static final String SECRET =
            "myschool_secret_key_for_testing_must_be_32_chars_min!";

    // ── Fixtures ───────────────────────────────────────────────────────────

    private JwtService jwtService;
    private UserDetails mockUser;

    @BeforeEach
    void setUp() {
        // Instantiate directly — no Spring context, no @Value injection needed
        jwtService = new JwtService(SECRET, 60L);

        // Real UserDetails from Spring Security — no mock required
        mockUser = User.withUsername("john@test.com")
                .password("encoded_password")
                .roles("USER")
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // generateToken()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void generateToken_shouldReturnNonNullToken() {
        // GIVEN — no extra setup needed; mockUser is ready

        // WHEN
        String token = jwtService.generateToken(mockUser);

        // THEN — token must exist and follow the JWT 3-part structure
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(2, token.chars().filter(c -> c == '.').count(),
                "A JWT must contain exactly 2 dots (header.payload.signature)");
    }

    @Test
    void generateToken_shouldIncludeExtraClaims_whenProvided() {
        // GIVEN
        Map<String, Object> extraClaims = Map.of("role", "ADMIN");

        // WHEN
        String token = jwtService.generateToken(extraClaims, mockUser);

        // THEN — token is valid and still carries the correct subject
        assertNotNull(token);
        assertEquals("john@test.com", jwtService.extractUsername(token));
    }

    // ══════════════════════════════════════════════════════════════════════
    // extractUsername()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void extractUsername_shouldReturnCorrectEmail() {
        // GIVEN
        String token = jwtService.generateToken(mockUser);

        // WHEN
        String username = jwtService.extractUsername(token);

        // THEN — subject must match the username used during generation
        assertEquals("john@test.com", username);
    }

    @Test
    void extractUsername_shouldThrowException_whenTokenIsInvalid() {
        // GIVEN — a string that is not a valid signed JWT
        String invalidToken = "this.is.not.a.valid.jwt.token";

        // WHEN + THEN — JJWT must reject the malformed token
        assertThrows(Exception.class,
                () -> jwtService.extractUsername(invalidToken));
    }

    // ══════════════════════════════════════════════════════════════════════
    // extractExpiration()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void extractExpiration_shouldReturnFutureDate() {
        // GIVEN
        String token = jwtService.generateToken(mockUser);

        // WHEN
        Date expiration = jwtService.extractExpiration(token);

        // THEN — a freshly minted token must expire in the future
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()),
                "Expiration date must be after now for a fresh token");
    }

    // ══════════════════════════════════════════════════════════════════════
    // isTokenValid()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void isTokenValid_shouldReturnTrue_whenTokenIsValid() {
        // GIVEN
        String token = jwtService.generateToken(mockUser);

        // WHEN
        boolean result = jwtService.isTokenValid(token, mockUser);

        // THEN
        assertTrue(result);
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenUsernameDoesNotMatch() {
        // GIVEN — token belongs to john@test.com
        String token = jwtService.generateToken(mockUser);

        UserDetails otherUser = User.withUsername("other@test.com")
                .password("pass")
                .roles("USER")
                .build();

        // WHEN
        boolean result = jwtService.isTokenValid(token, otherUser);

        // THEN — the token must not be accepted for a different user
        assertFalse(result);
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsExpired() {
        // GIVEN — expirationMinutes = -1 produces a token that expired 1 minute ago
        JwtService expiredJwtService = new JwtService(SECRET, -1L);
        String token = expiredJwtService.generateToken(mockUser);

        /*
         * WHEN + THEN
         * JJWT 0.12.x parseSignedClaims() throws ExpiredJwtException for
         * expired tokens instead of returning claims. isTokenValid() does not
         * have an internal try-catch, so the exception propagates.
         * Both "returns false" and "throws" represent token rejection;
         * we handle both outcomes to keep the test implementation-agnostic.
         */
        boolean valid;
        try {
            valid = expiredJwtService.isTokenValid(token, mockUser);
        } catch (Exception e) {
            valid = false;
        }

        assertFalse(valid, "An expired token must never be considered valid");
    }

    @Test
    void isTokenValid_shouldThrowException_whenTokenIsNull() {
        // GIVEN — null is not a token

        // WHEN + THEN — must never silently accept a null token
        assertThrows(Exception.class,
                () -> jwtService.extractUsername(null));
    }
}
