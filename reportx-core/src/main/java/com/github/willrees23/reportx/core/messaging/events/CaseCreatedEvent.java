package com.github.willrees23.reportx.core.messaging.events;

import com.github.willrees23.reportx.core.model.Case;

public record CaseCreatedEvent(Case value) implements ReportXEvent {
}
