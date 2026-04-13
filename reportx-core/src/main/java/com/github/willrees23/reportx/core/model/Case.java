package com.github.willrees23.reportx.core.model;

import java.time.Instant;
import java.util.UUID;

public record Case(
        UUID id,
        UUID targetId,
        String category,
        CaseStatus status,
        UUID claimedBy,
        Instant claimedAt,
        UUID resolvedBy,
        Instant resolvedAt,
        String resolutionReason,
        Instant createdAt,
        Instant lastActivityAt
) {
}
