package com.github.willrees23.reportx.core.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEntry(
        UUID id,
        UUID caseId,
        UUID actorId,
        AuditEventType eventType,
        Map<String, Object> payload,
        Instant createdAt
) {
}
