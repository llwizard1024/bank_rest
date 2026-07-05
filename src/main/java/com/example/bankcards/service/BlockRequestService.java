package com.example.bankcards.service;

import com.example.bankcards.dto.blockrequest.BlockRequestResponse;
import com.example.bankcards.entity.BlockRequestStatus;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BlockRequestNotFoundException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.CardBlockRequestRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardCryptoService;
import com.example.bankcards.util.CardNumberUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class BlockRequestService {

    private final CardBlockRequestRepository blockRequestRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardCryptoService cardCryptoService;

    public BlockRequestService(
            CardBlockRequestRepository blockRequestRepository,
            CardRepository cardRepository,
            UserRepository userRepository,
            CardCryptoService cardCryptoService
    ) {
        this.blockRequestRepository = blockRequestRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.cardCryptoService = cardCryptoService;
    }

    @Transactional
    public BlockRequestResponse createRequest(Long cardId) {
        User currentUser = getCurrentUser();

        Card card = cardRepository.findByIdAndOwnerId(cardId, currentUser.getId())
                .orElseThrow(() -> new CardNotFoundException(cardId));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Only active cards can be blocked");
        }

        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Expired cards cannot be blocked");
        }

        if (blockRequestRepository.existsByCardIdAndStatus(cardId, BlockRequestStatus.PENDING)) {
            throw new IllegalStateException("A pending block request already exists for this card");
        }

        CardBlockRequest request = CardBlockRequest.builder()
                .card(card)
                .requestedBy(currentUser)
                .status(BlockRequestStatus.PENDING)
                .build();

        return toResponse(blockRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public Page<BlockRequestResponse> getAllRequests(BlockRequestStatus status, Pageable pageable) {
        Page<CardBlockRequest> requests = status == null
                ? blockRequestRepository.findAll(pageable)
                : blockRequestRepository.findByStatus(status, pageable);
        return requests.map(this::toResponse);
    }

    @Transactional
    public BlockRequestResponse approve(Long requestId) {
        CardBlockRequest request = getPendingRequest(requestId);
        User admin = getCurrentUser();

        Card card = request.getCard();
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);

        request.setStatus(BlockRequestStatus.APPROVED);
        request.setProcessedAt(Instant.now());
        request.setProcessedBy(admin);

        return toResponse(blockRequestRepository.save(request));
    }

    @Transactional
    public BlockRequestResponse reject(Long requestId) {
        CardBlockRequest request = getPendingRequest(requestId);
        User admin = getCurrentUser();

        request.setStatus(BlockRequestStatus.REJECTED);
        request.setProcessedAt(Instant.now());
        request.setProcessedBy(admin);

        return toResponse(blockRequestRepository.save(request));
    }

    private CardBlockRequest getPendingRequest(Long requestId) {
        CardBlockRequest request = blockRequestRepository.findById(requestId)
                .orElseThrow(() -> new BlockRequestNotFoundException(requestId));

        if (request.getStatus() != BlockRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending block requests can be processed");
        }

        return request;
    }

    private BlockRequestResponse toResponse(CardBlockRequest request) {
        String plainNumber = cardCryptoService.decrypt(request.getCard().getEncryptedCardNumber());

        return BlockRequestResponse.builder()
                .id(request.getId())
                .cardId(request.getCard().getId())
                .maskedNumber(CardNumberUtils.mask(plainNumber))
                .status(request.getStatus())
                .requestedByUsername(request.getRequestedBy().getUsername())
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
                .processedByUsername(
                        request.getProcessedBy() != null ? request.getProcessedBy().getUsername() : null
                )
                .build();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
