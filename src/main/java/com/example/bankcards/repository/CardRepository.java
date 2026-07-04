package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    @EntityGraph(attributePaths = "owner")
    @Override
    Page<Card> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<Card> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<Card> findByOwnerIdAndStatus(Long ownerId, CardStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<Card> findByStatus(CardStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Optional<Card> findByIdAndOwnerId(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "owner")
    @Query("SELECT c FROM Card c WHERE c.id = :id AND c.owner.id = :ownerId")
    Optional<Card> findForTransferByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);
}
