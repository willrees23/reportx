package com.github.willrees23.reportx.core.messaging.events;

import com.github.willrees23.reportx.core.model.Report;

public record ReportCreatedEvent(Report report) implements ReportXEvent {
}
