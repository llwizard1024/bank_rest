package com.example.bankcards.dto.blockrequest;

import com.example.bankcards.entity.BlockRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class BlockRequestResponse {

    private Long id;
    private Long cardId;
    private String maskedNumber;
    private BlockRequestStatus status;
    private String requestedByUsername;
    private Instant createdAt;
    private Instant processedAt;
    private String processedByUsername;
}
