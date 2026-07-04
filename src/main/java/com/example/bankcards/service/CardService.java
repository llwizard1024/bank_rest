package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InvalidCardNumberException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardCryptoService;
import com.example.bankcards.util.CardNumberUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardCryptoService cardCryptoService;

    public CardService(
            CardRepository cardRepository,
            UserRepository userRepository,
            CardCryptoService cardCryptoService
    ) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.cardCryptoService = cardCryptoService;
    }

    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        if (!CardNumberUtils.isValid(request.getCardNumber())) {
            throw new InvalidCardNumberException();
        }

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new UserNotFoundException(request.getOwnerId()));

        String normalizedNumber = CardNumberUtils.normalize(request.getCardNumber());
        CardStatus status = resolveInitialStatus(request.getExpiryDate());

        Card card = Card.builder()
                .encryptedCardNumber(cardCryptoService.encrypt(normalizedNumber))
                .owner(owner)
                .expiryDate(request.getExpiryDate())
                .status(status)
                .balance(request.getBalance())
                .build();

        return toResponse(cardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(CardStatus status, Pageable pageable) {
        Page<Card> cards = status == null
                ? cardRepository.findAll(pageable)
                : cardRepository.findByStatus(status, pageable);
        return cards.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getMyCards(CardStatus status, Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<Card> cards = status == null
                ? cardRepository.findByOwnerId(currentUser.getId(), pageable)
                : cardRepository.findByOwnerIdAndStatus(currentUser.getId(), status, pageable);
        return cards.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CardResponse getCardById(Long id) {
        Card card = getAccessibleCard(id);
        return toResponse(card);
    }

    @Transactional
    public CardResponse blockCard(Long id) {
        Card card = getCardOrThrow(id);
        card.setStatus(CardStatus.BLOCKED);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse activateCard(Long id) {
        Card card = getCardOrThrow(id);
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            card.setStatus(CardStatus.EXPIRED);
        } else {
            card.setStatus(CardStatus.ACTIVE);
        }
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse requestBlock(Long id) {
        User currentUser = getCurrentUser();
        Card card = cardRepository.findByIdAndOwnerId(id, currentUser.getId())
                .orElseThrow(() -> new CardNotFoundException(id));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Only active cards can be blocked");
        }

        card.setStatus(CardStatus.BLOCKED);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(Long id) {
        Card card = getCardOrThrow(id);
        cardRepository.delete(card);
    }

    private Card getAccessibleCard(Long id) {
        if (isAdmin()) {
            return getCardOrThrow(id);
        }

        User currentUser = getCurrentUser();
        return cardRepository.findByIdAndOwnerId(id, currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
    }

    private Card getCardOrThrow(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
    }

    private CardResponse toResponse(Card card) {
        String plainNumber = cardCryptoService.decrypt(card.getEncryptedCardNumber());

        return CardResponse.builder()
                .id(card.getId())
                .maskedNumber(CardNumberUtils.mask(plainNumber))
                .ownerUsername(card.getOwner().getUsername())
                .expiryDate(card.getExpiryDate())
                .status(resolveDisplayStatus(card))
                .balance(card.getBalance())
                .createdAt(card.getCreatedAt())
                .build();
    }

    private CardStatus resolveDisplayStatus(Card card) {
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            return CardStatus.EXPIRED;
        }
        return card.getStatus();
    }

    private CardStatus resolveInitialStatus(LocalDate expiryDate) {
        return expiryDate.isBefore(LocalDate.now()) ? CardStatus.EXPIRED : CardStatus.ACTIVE;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
