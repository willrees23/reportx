package com.github.willrees23.reportx.core.model;

import java.time.Instant;
import java.util.UUID;

public record LogBufferEntry(
        UUID playerId,
        LogEntryType type,
        String content,
        String serverName,
        Instant createdAt
) {
}
