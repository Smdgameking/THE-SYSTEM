package com.thesystem.modules.xp.dto.transaction;

import com.thesystem.modules.xp.enums.TransactionType;
import java.time.Instant;

public record TransactionHistoryFilter(
        TransactionType transactionType,
        String sourceType,
        Instant fromDate,
        Instant toDate
) {
}
