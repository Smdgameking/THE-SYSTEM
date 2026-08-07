package com.thesystem.modules.xp.dto.transaction;

import com.thesystem.modules.xp.enums.TransactionType;
import java.util.Map;
import java.util.UUID;

public record TransactionCreateRequest(
        TransactionType transactionType,
        Integer amount,
        String sourceEngine,
        UUID sourceId,
        String sourceType,
        String reason,
        Map<String, Object> metadata
) {
}
