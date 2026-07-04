package com.example.bankcards.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardNumberUtilsTest {

    @Test
    void isValid_acceptsKnownValidCardNumber() {
        assertTrue(CardNumberUtils.isValid("4111111111111111"));
        assertTrue(CardNumberUtils.isValid("5555 5555 5555 4444"));
    }

    @Test
    void isValid_rejectsInvalidLuhnNumber() {
        assertFalse(CardNumberUtils.isValid("4111111111111112"));
    }

    @Test
    void isValid_rejectsTooShortNumber() {
        assertFalse(CardNumberUtils.isValid("1234"));
    }

    @Test
    void normalize_removesNonDigits() {
        assertEquals("4111111111111111", CardNumberUtils.normalize("4111 1111 1111 1111"));
    }

    @Test
    void mask_returnsLastFourDigitsOnly() {
        assertEquals("**** **** **** 1111", CardNumberUtils.mask("4111111111111111"));
    }

    @Test
    void mask_throwsForTooShortInput() {
        assertThrows(IllegalArgumentException.class, () -> CardNumberUtils.mask("123"));
    }

    @Test
    void lastFour_returnsLastFourDigits() {
        assertEquals("1111", CardNumberUtils.lastFour("4111111111111111"));
        assertEquals("4444", CardNumberUtils.lastFour("5555 5555 5555 4444"));
    }
}
