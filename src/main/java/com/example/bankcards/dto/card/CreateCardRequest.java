package com.example.bankcards.dto.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateCardRequest {

    @NotNull(message = "Owner id is required")
    private Long ownerId;

    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @PositiveOrZero(message = "Balance must be zero or positive")
    private BigDecimal balance = BigDecimal.ZERO;
}
