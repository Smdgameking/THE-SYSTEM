package com.thesystem.modules.xp.dto.level;

public record LevelInfo(
        int level,
        int xpRequired,
        int xpForNextLevel
) {
}
