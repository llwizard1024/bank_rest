package com.example.bankcards.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private String username;

    public AuthResponse(String token, String username) {
        this.token = token;
        this.tokenType = "Bearer";
        this.username = username;
    }
}
