package com.github.willrees23.reportx.core.model;

import java.time.Instant;
import java.util.UUID;

public record Note(
        UUID id,
        UUID caseId,
        String body,
        UUID authorId,
        Instant createdAt,
        Instant editedAt
) {
}
