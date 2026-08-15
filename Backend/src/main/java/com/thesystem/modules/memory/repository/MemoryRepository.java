package com.thesystem.modules.memory.repository;

import com.thesystem.modules.memory.entity.Memory;
import com.thesystem.modules.memory.enums.MemoryImportance;
import com.thesystem.modules.memory.enums.MemorySource;
import com.thesystem.modules.memory.enums.MemoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {

    List<Memory> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    List<Memory> findByUserIdAndTypeAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, MemoryType type);

    List<Memory> findByUserIdAndImportanceAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, MemoryImportance importance);

    List<Memory> findByUserIdAndSourceAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, MemorySource source);

    Optional<Memory> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    long countByUserIdAndDeletedAtIsNull(UUID userId);

    @Query("""
            SELECT m FROM Memory m
            WHERE m.userId = :userId
              AND m.deletedAt IS NULL
              AND (LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY m.createdAt DESC
            """)
    List<Memory> searchByUserIdAndKeyword(@Param("userId") UUID userId, @Param("keyword") String keyword);
}
