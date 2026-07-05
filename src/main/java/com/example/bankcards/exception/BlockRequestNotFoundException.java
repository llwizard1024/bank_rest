package com.example.bankcards.exception;

public class BlockRequestNotFoundException extends RuntimeException {

    public BlockRequestNotFoundException(Long id) {
        super("Block request not found: " + id);
    }
}
