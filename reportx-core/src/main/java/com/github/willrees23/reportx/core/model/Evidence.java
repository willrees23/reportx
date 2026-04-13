package com.github.willrees23.reportx.core.model;

import java.time.Instant;
import java.util.UUID;

public record Evidence(
        UUID id,
        UUID caseId,
        String label,
        String content,
        UUID authorId,
        Instant createdAt,
        Instant editedAt
) {
}
