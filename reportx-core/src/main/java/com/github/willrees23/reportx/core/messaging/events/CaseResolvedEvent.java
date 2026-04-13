package com.github.willrees23.reportx.core.messaging.events;

import com.github.willrees23.reportx.core.model.CaseStatus;

import java.util.UUID;

public record CaseResolvedEvent(UUID caseId, CaseStatus outcome, UUID staffId, String reason) implements ReportXEvent {
}
