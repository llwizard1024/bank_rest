package com.example.bankcards.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private boolean enabled;
    private Set<String> roles;
    private Instant createdAt;
}
