package com.github.willrees23.reportx.core.audit;

import com.github.willrees23.reportx.core.messaging.events.CaseClaimedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseCreatedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseHandedOffEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseReleasedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseReopenedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseResolvedEvent;
import com.github.willrees23.reportx.core.messaging.events.EvidenceAddedEvent;
import com.github.willrees23.reportx.core.messaging.events.EvidenceDeletedEvent;
import com.github.willrees23.reportx.core.messaging.events.EvidenceEditedEvent;
import com.github.willrees23.reportx.core.messaging.events.NoteAddedEvent;
import com.github.willrees23.reportx.core.messaging.events.NoteDeletedEvent;
import com.github.willrees23.reportx.core.messaging.events.NoteEditedEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportCreatedEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportMergedIntoCaseEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportXEvent;
import com.github.willrees23.reportx.core.messaging.events.TeleportToTargetRequestEvent;
import com.github.willrees23.reportx.core.model.AuditEntry;
import com.github.willrees23.reportx.core.model.AuditEventType;
import com.github.willrees23.reportx.core.model.CaseStatus;
import com.github.willrees23.reportx.core.util.Clock;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AuditEventTranslator {

    private final Clock clock;

    public AuditEventTranslator(Clock clock) {
        this.clock = clock;
    }

    public Optional<AuditEntry> translate(ReportXEvent event) {
        Instant now = clock.now();
        return switch (event) {
            case CaseCreatedEvent e -> Optional.of(entry(
                    e.value().id(), null, AuditEventType.CASE_CREATED,
                    payload(
                            "category", e.value().category(),
                            "targetId", e.value().targetId().toString()),
                    now));
            case ReportMergedIntoCaseEvent e -> Optional.of(entry(
                    e.caseId(), null, AuditEventType.REPORT_MERGED,
                    payload("reportId", e.reportId().toString()),
                    now));
            case CaseClaimedEvent e -> Optional.of(entry(
                    e.caseId(), e.staffId(), AuditEventType.CASE_CLAIMED,
                    payload("server", e.serverName()),
                    now));
            case CaseReleasedEvent e -> Optional.of(entry(
                    e.caseId(), e.staffId(), AuditEventType.CASE_RELEASED,
                    Map.of(),
                    now));
            case CaseHandedOffEvent e -> Optional.of(entry(
                    e.caseId(), e.fromStaffId(), AuditEventType.CASE_HANDED_OFF,
                    payload("toStaffId", e.toStaffId().toString()),
                    now));
            case CaseResolvedEvent e -> {
                AuditEventType type = e.outcome() == CaseStatus.RESOLVED_ACCEPTED
                        ? AuditEventType.CASE_RESOLVED_ACCEPTED
                        : AuditEventType.CASE_RESOLVED_DENIED;
                yield Optional.of(entry(
                        e.caseId(), e.staffId(), type,
                        payload("reason", nullToEmpty(e.reason())),
                        now));
            }
            case CaseReopenedEvent e -> Optional.of(entry(
                    e.caseId(), e.staffId(), AuditEventType.CASE_REOPENED,
                    payload("reason", nullToEmpty(e.reason())),
                    now));
            case EvidenceAddedEvent e -> Optional.of(entry(
                    e.caseId(), e.actorId(), AuditEventType.EVIDENCE_ADDED,
                    payload("evidenceId", e.evidenceId().toString()),
                    now));
            case EvidenceEditedEvent e -> Optional.of(entry(
                    e.caseId(), e.actorId(), AuditEventType.EVIDENCE_EDITED,
                    payload("evidenceId", e.evidenceId().toString()),
                    now));
            case EvidenceDeletedEvent e -> Optional.of(entry(
                    e.caseId(), e.actorId(), AuditEventType.EVIDENCE_DELETED,
                    payload("evidenceId", e.evidenceId().toString()),
                    now));
            case NoteAddedEvent e -> Optional.of(entry(
                    e.caseId(), e.actorId(), AuditEventType.NOTE_ADDED,
                    payload("noteId", e.noteId().toString()),
                    now));
            case NoteEditedEvent e -> Optional.of(entry(
                    e.caseId(), e.actorId(), AuditEventType.NOTE_EDITED,
                    payload("noteId", e.noteId().toString()),
                    now));
            case NoteDeletedEvent e -> Optional.of(entry(
                    e.caseId(), e.actorId(), AuditEventType.NOTE_DELETED,
                    payload("noteId", e.noteId().toString()),
                    now));
            case ReportCreatedEvent ignored -> Optional.empty();
            case TeleportToTargetRequestEvent ignored -> Optional.empty();
        };
    }

    private static AuditEntry entry(UUID caseId, UUID actorId, AuditEventType type, Map<String, Object> payload, Instant now) {
        return new AuditEntry(UUID.randomUUID(), caseId, actorId, type, payload, now);
    }

    private static Map<String, Object> payload(String k1, Object v1) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(k1, v1);
        return map;
    }

    private static Map<String, Object> payload(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
