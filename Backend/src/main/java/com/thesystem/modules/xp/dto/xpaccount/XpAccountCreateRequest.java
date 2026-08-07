package com.thesystem.modules.xp.dto.xpaccount;

import java.util.UUID;

public record XpAccountCreateRequest(
        UUID userId
) {
}
