package com.example.bankcards.exception;

public class InvalidRoleException extends RuntimeException {

    public InvalidRoleException(String role) {
        super("Invalid role: " + role);
    }
}
