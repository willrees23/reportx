package com.github.willrees23.reportx.core.messaging.events;

import java.util.UUID;

public record NoteDeletedEvent(UUID noteId, UUID caseId, UUID actorId) implements ReportXEvent {
}
