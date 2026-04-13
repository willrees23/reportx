package com.github.willrees23.reportx.core.messaging.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import com.github.willrees23.reportx.core.model.Coords;
import com.github.willrees23.reportx.core.model.Report;
import com.github.willrees23.reportx.core.util.Json;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ReportXEventSerializationTest {

    private final ObjectMapper mapper = Json.newMapper();

    @Test
    void eachEventType_roundTripsViaJackson() {
        Stream.of(sampleEvents()).forEach(this::assertRoundTrips);
    }

    @Test
    void polymorphicDeserialization_pickCorrectSubtype() throws Exception {
        ReportXEvent original = new CaseClaimedEvent(UUID.randomUUID(), UUID.randomUUID(), "survival");
        String json = mapper.writeValueAsString(original);
        assertThat(json).contains("\"type\":\"case.claimed\"");

        ReportXEvent decoded = mapper.readValue(json, ReportXEvent.class);
        assertThat(decoded).isInstanceOf(CaseClaimedEvent.class);
        assertThat(decoded).isEqualTo(original);
    }

    private void assertRoundTrips(ReportXEvent event) {
        try {
            String json = mapper.writeValueAsString(event);
            ReportXEvent decoded = mapper.readValue(json, ReportXEvent.class);
            assertThat(decoded)
                    .as("round-trip of %s", event.getClass().getSimpleName())
                    .isEqualTo(event);
        } catch (Exception ex) {
            throw new AssertionError("Failed round-trip for " + event.getClass().getSimpleName(), ex);
        }
    }

    private ReportXEvent[] sampleEvents() {
        UUID id = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-13T12:00:00Z");

        Report report = new Report(
                id, caseId, actor, target, "chat", "spam",
                "survival", new Coords("world", 1.5, 64.0, -3.5), now);
        Case value = new Case(
                caseId, target, "chat", CaseStatus.UNCLAIMED,
                null, null, null, null, null, now, now);

        return new ReportXEvent[]{
                new ReportCreatedEvent(report),
                new CaseCreatedEvent(value),
                new ReportMergedIntoCaseEvent(id, caseId),
                new CaseClaimedEvent(caseId, actor, "survival"),
                new CaseReleasedEvent(caseId, actor),
                new CaseHandedOffEvent(caseId, actor, UUID.randomUUID()),
                new CaseResolvedEvent(caseId, CaseStatus.RESOLVED_ACCEPTED, actor, "confirmed"),
                new CaseReopenedEvent(caseId, actor, "new evidence"),
                new EvidenceAddedEvent(UUID.randomUUID(), caseId, actor),
                new EvidenceEditedEvent(UUID.randomUUID(), caseId, actor),
                new EvidenceDeletedEvent(UUID.randomUUID(), caseId, actor),
                new NoteAddedEvent(UUID.randomUUID(), caseId, actor),
                new NoteEditedEvent(UUID.randomUUID(), caseId, actor),
                new NoteDeletedEvent(UUID.randomUUID(), caseId, actor),
                new TeleportToTargetRequestEvent(actor, target)
        };
    }
}
