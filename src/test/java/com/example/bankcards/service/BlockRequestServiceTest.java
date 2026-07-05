package com.example.bankcards.service;

import com.example.bankcards.TestDates;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockRequestServiceTest {

    private static final String ENCRYPTED = "encrypted-number";

    @Mock
    private CardBlockRequestRepository blockRequestRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardCryptoService cardCryptoService;

    @InjectMocks
    private BlockRequestService blockRequestService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRequest_createsPendingRequestWithoutBlockingCard() {
        authenticateAs("bob", "ROLE_USER");

        User owner = user(1L, "bob");
        Card card = card(5L, owner, CardStatus.ACTIVE, TestDates.FUTURE_EXPIRY);

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(owner));
        when(cardRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(card));
        when(blockRequestRepository.existsByCardIdAndStatus(5L, BlockRequestStatus.PENDING)).thenReturn(false);
        when(cardCryptoService.decrypt(ENCRYPTED)).thenReturn("4111111111111111");
        when(blockRequestRepository.save(any(CardBlockRequest.class))).thenAnswer(invocation -> {
            CardBlockRequest request = invocation.getArgument(0);
            request.setId(10L);
            return request;
        });

        var response = blockRequestService.createRequest(5L);

        assertEquals(10L, response.getId());
        assertEquals(5L, response.getCardId());
        assertEquals(BlockRequestStatus.PENDING, response.getStatus());
        assertEquals("bob", response.getRequestedByUsername());
        assertEquals("**** **** **** 1111", response.getMaskedNumber());
        assertEquals(CardStatus.ACTIVE, card.getStatus());
        verify(cardRepository, never()).save(card);
    }

    @Test
    void createRequest_rejectsNonActiveCard() {
        authenticateAs("bob", "ROLE_USER");

        User owner = user(1L, "bob");
        Card card = card(5L, owner, CardStatus.BLOCKED, TestDates.FUTURE_EXPIRY);

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(owner));
        when(cardRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(card));

        assertThrows(IllegalStateException.class, () -> blockRequestService.createRequest(5L));
        verify(blockRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_rejectsExpiredCard() {
        authenticateAs("bob", "ROLE_USER");

        User owner = user(1L, "bob");
        Card card = card(5L, owner, CardStatus.ACTIVE, TestDates.PAST_EXPIRY);

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(owner));
        when(cardRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(card));

        assertThrows(IllegalStateException.class, () -> blockRequestService.createRequest(5L));
        verify(blockRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_rejectsDuplicatePendingRequest() {
        authenticateAs("bob", "ROLE_USER");

        User owner = user(1L, "bob");
        Card card = card(5L, owner, CardStatus.ACTIVE, TestDates.FUTURE_EXPIRY);

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(owner));
        when(cardRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(card));
        when(blockRequestRepository.existsByCardIdAndStatus(5L, BlockRequestStatus.PENDING)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> blockRequestService.createRequest(5L));
        verify(blockRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_throwsWhenCardNotFound() {
        authenticateAs("bob", "ROLE_USER");

        User owner = user(1L, "bob");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(owner));
        when(cardRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> blockRequestService.createRequest(5L));
    }

    @Test
    void approve_blocksCardAndMarksRequestApproved() {
        authenticateAs("admin", "ROLE_ADMIN");

        User owner = user(1L, "bob");
        User admin = user(2L, "admin");
        Card card = card(5L, owner, CardStatus.ACTIVE, TestDates.FUTURE_EXPIRY);
        CardBlockRequest request = blockRequest(10L, card, owner, BlockRequestStatus.PENDING);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(blockRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(cardCryptoService.decrypt(ENCRYPTED)).thenReturn("4111111111111111");
        when(blockRequestRepository.save(request)).thenReturn(request);
        when(cardRepository.save(card)).thenReturn(card);

        var response = blockRequestService.approve(10L);

        assertEquals(BlockRequestStatus.APPROVED, response.getStatus());
        assertEquals(CardStatus.BLOCKED, card.getStatus());
        assertEquals("admin", response.getProcessedByUsername());
        verify(cardRepository).save(card);
    }

    @Test
    void reject_leavesCardUnchangedAndMarksRequestRejected() {
        authenticateAs("admin", "ROLE_ADMIN");

        User owner = user(1L, "bob");
        User admin = user(2L, "admin");
        Card card = card(5L, owner, CardStatus.ACTIVE, TestDates.FUTURE_EXPIRY);
        CardBlockRequest request = blockRequest(10L, card, owner, BlockRequestStatus.PENDING);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(blockRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(cardCryptoService.decrypt(ENCRYPTED)).thenReturn("4111111111111111");
        when(blockRequestRepository.save(request)).thenReturn(request);

        var response = blockRequestService.reject(10L);

        assertEquals(BlockRequestStatus.REJECTED, response.getStatus());
        assertEquals(CardStatus.ACTIVE, card.getStatus());
        assertEquals("admin", response.getProcessedByUsername());
        verify(cardRepository, never()).save(card);
    }

    @Test
    void approve_rejectsAlreadyProcessedRequest() {
        authenticateAs("admin", "ROLE_ADMIN");

        User owner = user(1L, "bob");
        Card card = card(5L, owner, CardStatus.BLOCKED, TestDates.FUTURE_EXPIRY);
        CardBlockRequest request = blockRequest(10L, card, owner, BlockRequestStatus.APPROVED);

        when(blockRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(IllegalStateException.class, () -> blockRequestService.approve(10L));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void approve_throwsWhenRequestNotFound() {
        authenticateAs("admin", "ROLE_ADMIN");

        when(blockRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BlockRequestNotFoundException.class, () -> blockRequestService.approve(99L));
    }

    @Test
    void getAllRequests_filtersByStatus() {
        User owner = user(1L, "bob");
        Card card = card(5L, owner, CardStatus.ACTIVE, TestDates.FUTURE_EXPIRY);
        CardBlockRequest request = blockRequest(10L, card, owner, BlockRequestStatus.PENDING);
        Pageable pageable = PageRequest.of(0, 20);

        when(blockRequestRepository.findByStatus(BlockRequestStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(request), pageable, 1));
        when(cardCryptoService.decrypt(ENCRYPTED)).thenReturn("4111111111111111");

        var result = blockRequestService.getAllRequests(BlockRequestStatus.PENDING, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(BlockRequestStatus.PENDING, result.getContent().getFirst().getStatus());
        assertNull(result.getContent().getFirst().getProcessedByUsername());
    }

    @Test
    void createRequest_savesPendingStatus() {
        authenticateAs("bob", "ROLE_USER");

        User owner = user(1L, "bob");
        Card card = card(5L, owner, CardStatus.ACTIVE, TestDates.FUTURE_EXPIRY);

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(owner));
        when(cardRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(card));
        when(blockRequestRepository.existsByCardIdAndStatus(5L, BlockRequestStatus.PENDING)).thenReturn(false);
        when(cardCryptoService.decrypt(ENCRYPTED)).thenReturn("4111111111111111");
        when(blockRequestRepository.save(any(CardBlockRequest.class))).thenAnswer(invocation -> {
            CardBlockRequest request = invocation.getArgument(0);
            request.setId(10L);
            return request;
        });

        blockRequestService.createRequest(5L);

        ArgumentCaptor<CardBlockRequest> captor = ArgumentCaptor.forClass(CardBlockRequest.class);
        verify(blockRequestRepository).save(captor.capture());
        assertEquals(BlockRequestStatus.PENDING, captor.getValue().getStatus());
        assertEquals(owner, captor.getValue().getRequestedBy());
        assertEquals(card, captor.getValue().getCard());
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

    private CardBlockRequest blockRequest(Long id, Card card, User requestedBy, BlockRequestStatus status) {
        return CardBlockRequest.builder()
                .id(id)
                .card(card)
                .requestedBy(requestedBy)
                .status(status)
                .build();
    }
}
