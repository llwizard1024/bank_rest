package com.example.bankcards.repository;

import com.example.bankcards.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @EntityGraph(attributePaths = {"fromCard", "toCard"})
    @Query("""
            SELECT t FROM Transfer t
            WHERE t.fromCard.owner.id = :userId OR t.toCard.owner.id = :userId
            ORDER BY t.createdAt DESC
            """)
    Page<Transfer> findByOwnerId(@Param("userId") Long userId, Pageable pageable);
}
