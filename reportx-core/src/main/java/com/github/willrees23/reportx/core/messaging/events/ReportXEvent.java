package com.github.willrees23.reportx.core.messaging.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReportCreatedEvent.class, name = "report.created"),
        @JsonSubTypes.Type(value = CaseCreatedEvent.class, name = "case.created"),
        @JsonSubTypes.Type(value = ReportMergedIntoCaseEvent.class, name = "report.merged"),
        @JsonSubTypes.Type(value = CaseClaimedEvent.class, name = "case.claimed"),
        @JsonSubTypes.Type(value = CaseReleasedEvent.class, name = "case.released"),
        @JsonSubTypes.Type(value = CaseHandedOffEvent.class, name = "case.handed_off"),
        @JsonSubTypes.Type(value = CaseResolvedEvent.class, name = "case.resolved"),
        @JsonSubTypes.Type(value = CaseReopenedEvent.class, name = "case.reopened"),
        @JsonSubTypes.Type(value = EvidenceAddedEvent.class, name = "evidence.added"),
        @JsonSubTypes.Type(value = EvidenceEditedEvent.class, name = "evidence.edited"),
        @JsonSubTypes.Type(value = EvidenceDeletedEvent.class, name = "evidence.deleted"),
        @JsonSubTypes.Type(value = NoteAddedEvent.class, name = "note.added"),
        @JsonSubTypes.Type(value = NoteEditedEvent.class, name = "note.edited"),
        @JsonSubTypes.Type(value = NoteDeletedEvent.class, name = "note.deleted"),
        @JsonSubTypes.Type(value = TeleportToTargetRequestEvent.class, name = "teleport.request")
})
public sealed interface ReportXEvent
        permits ReportCreatedEvent,
                CaseCreatedEvent,
                ReportMergedIntoCaseEvent,
                CaseClaimedEvent,
                CaseReleasedEvent,
                CaseHandedOffEvent,
                CaseResolvedEvent,
                CaseReopenedEvent,
                EvidenceAddedEvent,
                EvidenceEditedEvent,
                EvidenceDeletedEvent,
                NoteAddedEvent,
                NoteEditedEvent,
                NoteDeletedEvent,
                TeleportToTargetRequestEvent {
}
