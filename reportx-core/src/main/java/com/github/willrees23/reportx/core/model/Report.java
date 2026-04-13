package com.github.willrees23.reportx.core.model;

import java.time.Instant;
import java.util.UUID;

public record Report(
        UUID id,
        UUID caseId,
        UUID reporterId,
        UUID targetId,
        String category,
        String detail,
        String serverName,
        Coords reporterCoords,
        Instant createdAt
) {
}
