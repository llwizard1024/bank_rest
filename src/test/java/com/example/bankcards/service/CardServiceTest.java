package com.example.bankcards.service;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    private static final String ENCRYPTED = "encrypted-number";

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardCryptoService cardCryptoService;

    @InjectMocks
    private CardService cardService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCard_encryptsNumberAndReturnsMaskedResponse() {
        CreateCardRequest request = new CreateCardRequest();
        request.setOwnerId(1L);
        request.setCardNumber("4111111111111111");
        request.setExpiryDate(LocalDate.now().plusYears(2));
        request.setBalance(new BigDecimal("1000.00"));

        User owner = user(1L, "bob");

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(cardCryptoService.encrypt("4111111111111111")).thenReturn(ENCRYPTED);
        when(cardCryptoService.decrypt(ENCRYPTED)).thenReturn("4111111111111111");
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            assertEquals("1111", card.getLastFourDigits());
            card.setId(10L);
            return card;
        });

        var response = cardService.createCard(request);

        assertEquals(10L, response.getId());
        assertEquals("**** **** **** 1111", response.getMaskedNumber());
        assertEquals("bob", response.getOwnerUsername());
        assertEquals(CardStatus.ACTIVE, response.getStatus());
        verify(cardCryptoService).encrypt("4111111111111111");
    }

    @Test
    void createCard_rejectsInvalidCardNumber() {
        CreateCardRequest request = new CreateCardRequest();
        request.setOwnerId(1L);
        request.setCardNumber("1234567890123456");
        request.setExpiryDate(LocalDate.now().plusYears(1));
        request.setBalance(BigDecimal.ZERO);

        assertThrows(InvalidCardNumberException.class, () -> cardService.createCard(request));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void createCard_throwsWhenOwnerNotFound() {
        CreateCardRequest request = new CreateCardRequest();
        request.setOwnerId(99L);
        request.setCardNumber("4111111111111111");
        request.setExpiryDate(LocalDate.now().plusYears(1));
        request.setBalance(BigDecimal.ZERO);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> cardService.createCard(request));
    }

    @Test
    void requestBlock_blocksOwnActiveCard() {
        authenticateAs("bob", "ROLE_USER");

        User owner = user(1L, "bob");
        Card card = card(5L, owner, CardStatus.ACTIVE, LocalDate.now().plusYears(1));

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(owner));
        when(cardRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(card));
        when(cardCryptoService.decrypt(ENCRYPTED)).thenReturn("4111111111111111");
        when(cardRepository.save(card)).thenReturn(card);

        var response = cardService.requestBlock(5L);

        assertEquals(CardStatus.BLOCKED, response.getStatus());
        assertEquals(CardStatus.BLOCKED, card.getStatus());
    }

    @Test
    void requestBlock_rejectsNonActiveCard() {
        authenticateAs("bob", "ROLE_USER");

        User owner = user(1L, "bob");
        Card card = card(5L, owner, CardStatus.BLOCKED, LocalDate.now().plusYears(1));

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(owner));
        when(cardRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(card));

        assertThrows(IllegalStateException.class, () -> cardService.requestBlock(5L));
    }

    @Test
    void getCardById_deniesAccessToForeignCardForUser() {
        authenticateAs("alice", "ROLE_USER");

        User alice = user(1L, "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(cardRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> cardService.getCardById(5L));
    }

    @Test
    void getCardById_allowsAdminToAccessAnyCard() {
        authenticateAs("admin", "ROLE_ADMIN");

        User owner = user(2L, "bob");
        Card card = card(5L, owner, CardStatus.ACTIVE, LocalDate.now().plusYears(1));

        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));
        when(cardCryptoService.decrypt(ENCRYPTED)).thenReturn("4111111111111111");

        var response = cardService.getCardById(5L);

        assertEquals(5L, response.getId());
        assertEquals("bob", response.getOwnerUsername());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void activateCard_marksExpiredCardAsExpired() {
        User owner = user(1L, "bob");
        Card card = card(5L, owner, CardStatus.BLOCKED, LocalDate.now().minusDays(1));

        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));
        when(cardCryptoService.decrypt(ENCRYPTED)).thenReturn("4111111111111111");
        when(cardRepository.save(card)).thenReturn(card);

        var response = cardService.activateCard(5L);

        assertEquals(CardStatus.EXPIRED, response.getStatus());
    }

    @Test
    void deleteCard_removesExistingCard() {
        User owner = user(1L, "bob");
        Card card = card(5L, owner, CardStatus.ACTIVE, LocalDate.now().plusYears(1));

        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));

        cardService.deleteCard(5L);

        verify(cardRepository).delete(card);
    }

    @Test
    void getMyCards_searchByLastFourDigits() {
        authenticateAs("bob", "ROLE_USER");

        User owner = user(1L, "bob");
        Pageable pageable = PageRequest.of(0, 20);
        Card card = card(5L, owner, CardStatus.ACTIVE, LocalDate.now().plusYears(1));

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(owner));
        when(cardRepository.findByOwnerIdAndLastFourDigits(1L, "1111", pageable))
                .thenReturn(new PageImpl<>(List.of(card), pageable, 1));
        when(cardCryptoService.decrypt(ENCRYPTED)).thenReturn("4111111111111111");

        var result = cardService.getMyCards(null, "1111", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("**** **** **** 1111", result.getContent().getFirst().getMaskedNumber());
    }

    @Test
    void getMyCards_searchRejectsInvalidLength() {
        authenticateAs("bob", "ROLE_USER");

        assertThrows(
                IllegalArgumentException.class,
                () -> cardService.getMyCards(null, "12", PageRequest.of(0, 20))
        );
    }

    @Test
    void deleteCard_throwsWhenCardMissing() {
        when(cardRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> cardService.deleteCard(404L));
    }

    private void authenticateAs(String username, String role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User user(Long id, String username) {
        User user = User.builder().username(username).password("encoded").build();
        user.setId(id);
        return user;
    }

    private Card card(Long id, User owner, CardStatus status, LocalDate expiryDate) {
        return Card.builder()
                .id(id)
                .encryptedCardNumber(ENCRYPTED)
                .owner(owner)
                .status(status)
                .expiryDate(expiryDate)
                .balance(BigDecimal.TEN)
                .build();
    }
}
