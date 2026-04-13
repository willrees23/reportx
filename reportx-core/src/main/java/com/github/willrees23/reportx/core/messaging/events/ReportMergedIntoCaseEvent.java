package com.github.willrees23.reportx.core.messaging.events;

import java.util.UUID;

public record ReportMergedIntoCaseEvent(UUID reportId, UUID caseId) implements ReportXEvent {
}
