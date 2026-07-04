package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidTransferException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardCryptoService;
import com.example.bankcards.util.CardNumberUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardCryptoService cardCryptoService;

    public TransferService(
            TransferRepository transferRepository,
            CardRepository cardRepository,
            UserRepository userRepository,
            CardCryptoService cardCryptoService
    ) {
        this.transferRepository = transferRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.cardCryptoService = cardCryptoService;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        if (request.getFromCardId().equals(request.getToCardId())) {
            throw new InvalidTransferException("Source and destination cards must be different");
        }

        User currentUser = getCurrentUser();

        Card fromCard;
        Card toCard;
        if (request.getFromCardId() < request.getToCardId()) {
            fromCard = lockCard(request.getFromCardId(), currentUser.getId());
            toCard = lockCard(request.getToCardId(), currentUser.getId());
        } else {
            toCard = lockCard(request.getToCardId(), currentUser.getId());
            fromCard = lockCard(request.getFromCardId(), currentUser.getId());
        }

        validateCardForTransfer(fromCard);
        validateCardForTransfer(toCard);

        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException();
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        Transfer transfer = Transfer.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(request.getAmount())
                .build();

        return toResponse(transferRepository.save(transfer));
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> getMyTransfers(Pageable pageable) {
        User currentUser = getCurrentUser();
        return transferRepository.findByOwnerId(currentUser.getId(), pageable)
                .map(this::toResponse);
    }

    private Card lockCard(Long cardId, Long ownerId) {
        return cardRepository.findForTransferByIdAndOwnerId(cardId, ownerId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
    }

    private void validateCardForTransfer(Card card) {
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new InvalidTransferException("Card is expired: " + card.getId());
        }
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidTransferException("Card is not active: " + card.getId());
        }
    }

    private TransferResponse toResponse(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .fromCardId(transfer.getFromCard().getId())
                .toCardId(transfer.getToCard().getId())
                .fromCardMaskedNumber(maskCardNumber(transfer.getFromCard()))
                .toCardMaskedNumber(maskCardNumber(transfer.getToCard()))
                .amount(transfer.getAmount())
                .createdAt(transfer.getCreatedAt())
                .build();
    }

    private String maskCardNumber(Card card) {
        String plainNumber = cardCryptoService.decrypt(card.getEncryptedCardNumber());
        return CardNumberUtils.mask(plainNumber);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
