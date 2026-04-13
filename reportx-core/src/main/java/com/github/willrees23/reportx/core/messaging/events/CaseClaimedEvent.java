package com.github.willrees23.reportx.core.messaging.events;

import java.util.UUID;

public record CaseClaimedEvent(UUID caseId, UUID staffId, String serverName) implements ReportXEvent {
}
