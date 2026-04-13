package com.github.willrees23.reportx.core.messaging.events;

import java.util.UUID;

public record CaseReleasedEvent(UUID caseId, UUID staffId) implements ReportXEvent {
}
