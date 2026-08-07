package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record PolicyChangedEvent(
        Long policyId,
        String policyCode,
        String changes,
        Instant occurredAt
) implements DomainEvent {
    public PolicyChangedEvent(Long policyId, String policyCode, String changes) {
        this(policyId, policyCode, changes, Instant.now());
    }
}
