package com.thesystem.modules.xp.repository;

import com.thesystem.modules.xp.entity.XpTransaction;
import com.thesystem.modules.xp.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface XpTransactionRepository extends JpaRepository<XpTransaction, UUID> {

    List<XpTransaction> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<XpTransaction> findByUserIdAndTransactionTypeAndDeletedAtIsNull(UUID userId, TransactionType transactionType);

    List<XpTransaction> findByUserIdAndCreatedAtBetweenAndDeletedAtIsNull(UUID userId, Instant start, Instant end);

    Optional<XpTransaction> findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(String sourceEngine, UUID sourceId, String sourceType);

    List<XpTransaction> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Page<XpTransaction> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM XpTransaction t WHERE t.userId = :userId AND t.deletedAt IS NULL AND t.amount > 0")
    Integer sumPositiveAmountByUserId(@Param("userId") UUID userId);

    @Query("SELECT SUM(t.amount) FROM XpTransaction t WHERE t.userId = :userId AND t.deletedAt IS NULL AND t.amount < 0")
    Integer sumNegativeAmountByUserId(@Param("userId") UUID userId);

    @Query("SELECT SUM(t.amount) FROM XpTransaction t WHERE t.userId = :userId AND t.deletedAt IS NULL AND t.amount > 0 AND t.createdAt >= :start")
    Integer sumPositiveAmountByUserIdAndCreatedAtAfter(@Param("userId") UUID userId, @Param("start") java.time.Instant start);
}
