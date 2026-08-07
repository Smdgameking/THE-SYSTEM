package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PolicyChangedEvent(
        UUID policyId,
        String policyCode,
        String changes,
        Instant occurredAt
) implements DomainEvent {
    public PolicyChangedEvent(UUID policyId, String policyCode, String changes) {
        this(policyId, policyCode, changes, Instant.now());
    }
}
