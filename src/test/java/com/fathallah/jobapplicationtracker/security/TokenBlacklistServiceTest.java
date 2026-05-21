package com.fathallah.jobapplicationtracker.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBlacklistServiceTest {

    final TokenBlacklistService svc = new TokenBlacklistService();

    @Test
    void freshJti_isNotBlacklisted() {
        assertFalse(svc.isBlacklisted("some-jti"));
    }

    @Test
    void afterInvalidate_jtiIsBlacklisted() {
        svc.invalidate("abc-123");
        assertTrue(svc.isBlacklisted("abc-123"));
    }

    @Test
    void invalidatingOneJti_doesNotAffectAnother() {
        svc.invalidate("abc-123");
        assertFalse(svc.isBlacklisted("xyz-456"));
    }
}
