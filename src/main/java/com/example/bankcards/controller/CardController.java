package com.example.bankcards.controller;

import com.example.bankcards.dto.blockrequest.BlockRequestResponse;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.BlockRequestService;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;
    private final BlockRequestService blockRequestService;

    public CardController(CardService cardService, BlockRequestService blockRequestService) {
        this.cardService = cardService;
        this.blockRequestService = blockRequestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse createCard(@Valid @RequestBody CreateCardRequest request) {
        return cardService.createCard(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CardResponse> getAllCards(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return cardService.getAllCards(status, search, pageable);
    }

    @GetMapping("/my")
    public Page<CardResponse> getMyCards(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return cardService.getMyCards(status, search, pageable);
    }

    @GetMapping("/{id}")
    public CardResponse getCardById(@PathVariable Long id) {
        return cardService.getCardById(id);
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse blockCard(@PathVariable Long id) {
        return cardService.blockCard(id);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse activateCard(@PathVariable Long id) {
        return cardService.activateCard(id);
    }

    @PostMapping("/{id}/block-request")
    @PreAuthorize("hasRole('USER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BlockRequestResponse requestBlock(@PathVariable Long id) {
        return blockRequestService.createRequest(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
    }
}
