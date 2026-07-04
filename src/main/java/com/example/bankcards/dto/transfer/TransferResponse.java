package com.example.bankcards.dto.transfer;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransferResponse {

    private Long id;
    private Long fromCardId;
    private Long toCardId;
    private String fromCardMaskedNumber;
    private String toCardMaskedNumber;
    private BigDecimal amount;
    private Instant createdAt;
}
