package com.example.bankcards.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CardCryptoServiceTest {

    private CardCryptoService cardCryptoService;

    @BeforeEach
    void setUp() {
        cardCryptoService = new CardCryptoService("test-secret-key-at-least-32-characters-long");
    }

    @Test
    void encrypt_decrypt_roundTrip() {
        String plain = "4111111111111111";

        String encrypted = cardCryptoService.encrypt(plain);
        String decrypted = cardCryptoService.decrypt(encrypted);

        assertEquals(plain, decrypted);
        assertNotEquals(plain, encrypted);
    }
}
