package com.thesystem.modules.xp.dto.streak;

import java.time.LocalDate;

public record UserStreakResponse(
        Integer currentStreak,
        Integer longestStreak,
        LocalDate currentStreakStartDate,
        LocalDate lastActivityDate
) {
}
