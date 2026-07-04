package com.example.bankcards.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;
}
