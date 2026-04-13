package com.github.willrees23.reportx.core.messaging.events;

import java.util.UUID;

public record EvidenceDeletedEvent(UUID evidenceId, UUID caseId, UUID actorId) implements ReportXEvent {
}
