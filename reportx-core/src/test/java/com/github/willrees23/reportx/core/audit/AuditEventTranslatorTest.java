package com.github.willrees23.reportx.core.audit;

import com.github.willrees23.reportx.core.messaging.events.CaseClaimedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseCreatedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseHandedOffEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseReleasedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseReopenedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseResolvedEvent;
import com.github.willrees23.reportx.core.messaging.events.EvidenceAddedEvent;
import com.github.willrees23.reportx.core.messaging.events.NoteAddedEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportCreatedEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportMergedIntoCaseEvent;
import com.github.willrees23.reportx.core.messaging.events.TeleportToTargetRequestEvent;
import com.github.willrees23.reportx.core.model.AuditEntry;
import com.github.willrees23.reportx.core.model.AuditEventType;
import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import com.github.willrees23.reportx.core.model.Coords;
import com.github.willrees23.reportx.core.model.Report;
import com.github.willrees23.reportx.core.util.Clock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventTranslatorTest {

    private static final Instant NOW = Instant.parse("2026-04-13T12:00:00Z");
    private final AuditEventTranslator translator = new AuditEventTranslator(Clock.fixed(NOW));

    @Test
    void caseCreated_translatesWithCategoryAndTargetPayload() {
        UUID caseId = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Case value = new Case(caseId, target, "chat", CaseStatus.UNCLAIMED,
                null, null, null, null, null, NOW, NOW);

        AuditEntry entry = translator.translate(new CaseCreatedEvent(value)).orElseThrow();

        assertThat(entry.caseId()).isEqualTo(caseId);
        assertThat(entry.actorId()).isNull();
        assertThat(entry.eventType()).isEqualTo(AuditEventType.CASE_CREATED);
        assertThat(entry.payload())
                .containsEntry("category", "chat")
                .containsEntry("targetId", target.toString());
        assertThat(entry.createdAt()).isEqualTo(NOW);
    }

    @Test
    void caseClaimed_capturesActorAndServer() {
        UUID caseId = UUID.randomUUID();
        UUID staff = UUID.randomUUID();

        AuditEntry entry = translator.translate(new CaseClaimedEvent(caseId, staff, "survival")).orElseThrow();

        assertThat(entry.actorId()).isEqualTo(staff);
        assertThat(entry.eventType()).isEqualTo(AuditEventType.CASE_CLAIMED);
        assertThat(entry.payload()).containsEntry("server", "survival");
    }

    @Test
    void caseResolved_picksAcceptedOrDeniedTypeFromOutcome() {
        UUID caseId = UUID.randomUUID();
        UUID staff = UUID.randomUUID();

        AuditEntry accepted = translator.translate(
                new CaseResolvedEvent(caseId, CaseStatus.RESOLVED_ACCEPTED, staff, "confirmed")).orElseThrow();
        AuditEntry denied = translator.translate(
                new CaseResolvedEvent(caseId, CaseStatus.RESOLVED_DENIED, staff, "insufficient")).orElseThrow();

        assertThat(accepted.eventType()).isEqualTo(AuditEventType.CASE_RESOLVED_ACCEPTED);
        assertThat(accepted.payload()).containsEntry("reason", "confirmed");
        assertThat(denied.eventType()).isEqualTo(AuditEventType.CASE_RESOLVED_DENIED);
        assertThat(denied.payload()).containsEntry("reason", "insufficient");
    }

    @Test
    void caseHandedOff_capturesFromAndToStaff() {
        UUID caseId = UUID.randomUUID();
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        AuditEntry entry = translator.translate(new CaseHandedOffEvent(caseId, from, to)).orElseThrow();

        assertThat(entry.actorId()).isEqualTo(from);
        assertThat(entry.eventType()).isEqualTo(AuditEventType.CASE_HANDED_OFF);
        assertThat(entry.payload()).containsEntry("toStaffId", to.toString());
    }

    @Test
    void caseReleasedAndReopened_includeExpectedFields() {
        UUID caseId = UUID.randomUUID();
        UUID staff = UUID.randomUUID();

        AuditEntry released = translator.translate(new CaseReleasedEvent(caseId, staff)).orElseThrow();
        AuditEntry reopened = translator.translate(new CaseReopenedEvent(caseId, staff, "new evidence")).orElseThrow();

        assertThat(released.eventType()).isEqualTo(AuditEventType.CASE_RELEASED);
        assertThat(released.payload()).isEmpty();
        assertThat(reopened.eventType()).isEqualTo(AuditEventType.CASE_REOPENED);
        assertThat(reopened.payload()).containsEntry("reason", "new evidence");
    }

    @Test
    void evidenceAndNoteEvents_recordIdAndActor() {
        UUID caseId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        UUID evidenceId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        AuditEntry evidence = translator.translate(new EvidenceAddedEvent(evidenceId, caseId, actor)).orElseThrow();
        AuditEntry note = translator.translate(new NoteAddedEvent(noteId, caseId, actor)).orElseThrow();

        assertThat(evidence.eventType()).isEqualTo(AuditEventType.EVIDENCE_ADDED);
        assertThat(evidence.actorId()).isEqualTo(actor);
        assertThat(evidence.payload()).containsEntry("evidenceId", evidenceId.toString());

        assertThat(note.eventType()).isEqualTo(AuditEventType.NOTE_ADDED);
        assertThat(note.payload()).containsEntry("noteId", noteId.toString());
    }

    @Test
    void reportMerged_logsReportIdAgainstCase() {
        UUID caseId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        AuditEntry entry = translator.translate(new ReportMergedIntoCaseEvent(reportId, caseId)).orElseThrow();

        assertThat(entry.eventType()).isEqualTo(AuditEventType.REPORT_MERGED);
        assertThat(entry.payload()).containsEntry("reportId", reportId.toString());
    }

    @Test
    void reportCreatedAndTeleportRequest_areNotAudited() {
        Report report = new Report(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "chat", null, "survival",
                new Coords("world", 0, 0, 0), NOW);

        Optional<AuditEntry> reportCreated = translator.translate(new ReportCreatedEvent(report));
        Optional<AuditEntry> teleport = translator.translate(
                new TeleportToTargetRequestEvent(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(reportCreated).isEmpty();
        assertThat(teleport).isEmpty();
    }

    @Test
    void nullReason_storedAsEmptyString() {
        UUID caseId = UUID.randomUUID();
        UUID staff = UUID.randomUUID();

        AuditEntry entry = translator.translate(
                new CaseResolvedEvent(caseId, CaseStatus.RESOLVED_ACCEPTED, staff, null)).orElseThrow();

        assertThat(entry.payload()).containsEntry("reason", "");
    }
}
