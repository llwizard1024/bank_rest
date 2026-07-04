package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.TransferRequest;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final String ENCRYPTED = "encrypted";

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardCryptoService cardCryptoService;

    @InjectMocks
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        authenticateAs("bob");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transfer_movesBalanceBetweenOwnCards() {
        User bob = user(1L, "bob");
        Card fromCard = card(10L, bob, new BigDecimal("1000.00"));
        Card toCard = card(11L, bob, new BigDecimal("200.00"));

        TransferRequest request = new TransferRequest();
        request.setFromCardId(10L);
        request.setToCardId(11L);
        request.setAmount(new BigDecimal("150.00"));

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(cardRepository.findForTransferByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findForTransferByIdAndOwnerId(11L, 1L)).thenReturn(Optional.of(toCard));
        when(cardCryptoService.decrypt(ENCRYPTED)).thenReturn("4111111111111111");
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            transfer.setId(100L);
            return transfer;
        });

        var response = transferService.transfer(request);

        assertEquals(new BigDecimal("850.00"), fromCard.getBalance());
        assertEquals(new BigDecimal("350.00"), toCard.getBalance());
        assertEquals(new BigDecimal("150.00"), response.getAmount());
        assertEquals("**** **** **** 1111", response.getFromCardMaskedNumber());
        verify(cardRepository).save(fromCard);
        verify(cardRepository).save(toCard);
    }

    @Test
    void transfer_rejectsSameSourceAndDestination() {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(10L);
        request.setToCardId(10L);
        request.setAmount(new BigDecimal("10.00"));

        assertThrows(InvalidTransferException.class, () -> transferService.transfer(request));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transfer_rejectsInsufficientFunds() {
        User bob = user(1L, "bob");
        Card fromCard = card(10L, bob, new BigDecimal("50.00"));
        Card toCard = card(11L, bob, new BigDecimal("0.00"));

        TransferRequest request = new TransferRequest();
        request.setFromCardId(10L);
        request.setToCardId(11L);
        request.setAmount(new BigDecimal("100.00"));

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(cardRepository.findForTransferByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findForTransferByIdAndOwnerId(11L, 1L)).thenReturn(Optional.of(toCard));

        assertThrows(InsufficientFundsException.class, () -> transferService.transfer(request));
    }

    @Test
    void transfer_rejectsForeignCard() {
        User bob = user(1L, "bob");

        TransferRequest request = new TransferRequest();
        request.setFromCardId(10L);
        request.setToCardId(11L);
        request.setAmount(new BigDecimal("10.00"));

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(cardRepository.findForTransferByIdAndOwnerId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> transferService.transfer(request));
    }

    @Test
    void transfer_rejectsBlockedCard() {
        User bob = user(1L, "bob");
        Card fromCard = card(10L, bob, new BigDecimal("1000.00"));
        fromCard.setStatus(CardStatus.BLOCKED);
        Card toCard = card(11L, bob, new BigDecimal("0.00"));

        TransferRequest request = new TransferRequest();
        request.setFromCardId(10L);
        request.setToCardId(11L);
        request.setAmount(new BigDecimal("10.00"));

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(cardRepository.findForTransferByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findForTransferByIdAndOwnerId(11L, 1L)).thenReturn(Optional.of(toCard));

        assertThrows(InvalidTransferException.class, () -> transferService.transfer(request));
    }

    private void authenticateAs(String username) {
        var authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User user(Long id, String username) {
        User user = User.builder().username(username).password("encoded").build();
        user.setId(id);
        return user;
    }

    private Card card(Long id, User owner, BigDecimal balance) {
        return Card.builder()
                .id(id)
                .encryptedCardNumber(ENCRYPTED)
                .owner(owner)
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(1))
                .balance(balance)
                .build();
    }
}
