package com.example.bankcards.controller;

import com.example.bankcards.dto.blockrequest.BlockRequestResponse;
import com.example.bankcards.entity.BlockRequestStatus;
import com.example.bankcards.service.BlockRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/block-requests")
public class BlockRequestController {

    private final BlockRequestService blockRequestService;

    public BlockRequestController(BlockRequestService blockRequestService) {
        this.blockRequestService = blockRequestService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<BlockRequestResponse> getAllRequests(
            @RequestParam(required = false) BlockRequestStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return blockRequestService.getAllRequests(status, pageable);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public BlockRequestResponse approve(@PathVariable Long id) {
        return blockRequestService.approve(id);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public BlockRequestResponse reject(@PathVariable Long id) {
        return blockRequestService.reject(id);
    }
}
