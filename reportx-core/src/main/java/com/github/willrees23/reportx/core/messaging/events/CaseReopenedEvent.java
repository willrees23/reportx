package com.github.willrees23.reportx.core.messaging.events;

import java.util.UUID;

public record CaseReopenedEvent(UUID caseId, UUID staffId, String reason) implements ReportXEvent {
}
