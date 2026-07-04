package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class CardResponse {

    private Long id;
    private String maskedNumber;
    private String ownerUsername;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal balance;
    private Instant createdAt;
}
