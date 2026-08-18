package com.thesystem.modules.analytics.dto;

import java.time.LocalDate;

public record DailyPoint(
        LocalDate date,
        long value
) {
}
