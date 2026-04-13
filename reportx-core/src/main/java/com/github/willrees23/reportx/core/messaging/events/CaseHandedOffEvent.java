package com.github.willrees23.reportx.core.messaging.events;

import java.util.UUID;

public record CaseHandedOffEvent(UUID caseId, UUID fromStaffId, UUID toStaffId) implements ReportXEvent {
}
