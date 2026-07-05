package com.example.bankcards.repository;

import com.example.bankcards.entity.BlockRequestStatus;
import com.example.bankcards.entity.CardBlockRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardBlockRequestRepository extends JpaRepository<CardBlockRequest, Long> {

    boolean existsByCardIdAndStatus(Long cardId, BlockRequestStatus status);

    @EntityGraph(attributePaths = {"card", "card.owner", "requestedBy", "processedBy"})
    Page<CardBlockRequest> findByStatus(BlockRequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"card", "card.owner", "requestedBy", "processedBy"})
    @Override
    Page<CardBlockRequest> findAll(@NonNull Pageable pageable);
}
