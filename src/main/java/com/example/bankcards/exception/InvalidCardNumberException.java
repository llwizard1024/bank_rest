package com.example.bankcards.exception;

public class InvalidCardNumberException extends RuntimeException {

    public InvalidCardNumberException() {
        super("Invalid card number");
    }
}
