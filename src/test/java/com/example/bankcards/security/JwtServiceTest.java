package com.example.bankcards.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    private final UserDetails userDetails = User.withUsername("alice")
            .password("encoded")
            .roles("USER")
            .build();

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "test-secret-key-at-least-32-characters-long");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    @Test
    void generateToken_andValidate_success() {
        String token = jwtService.generateToken(userDetails);

        assertEquals("alice", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_rejectsTokenForAnotherUser() {
        String token = jwtService.generateToken(userDetails);

        UserDetails otherUser = User.withUsername("bob")
                .password("encoded")
                .roles("USER")
                .build();

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void isTokenValid_rejectsMalformedToken() {
        assertFalse(jwtService.isTokenValid("not-a-jwt", userDetails));
    }
}
