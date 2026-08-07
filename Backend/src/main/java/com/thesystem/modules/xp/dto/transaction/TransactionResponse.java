package com.thesystem.modules.xp.dto.transaction;

import com.thesystem.modules.xp.enums.TransactionType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID userId,
        TransactionType transactionType,
        Integer amount,
        Integer balanceAfter,
        String sourceEngine,
        UUID sourceId,
        String sourceType,
        UUID policyId,
        Double multiplierApplied,
        Integer baseAmount,
        String reason,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
