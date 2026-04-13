package com.github.willrees23.reportx.storage.sqlite;

import com.github.willrees23.reportx.core.model.AuditEntry;
import com.github.willrees23.reportx.core.model.AuditEventType;
import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteAuditRepositoryTest extends SqliteTestBase {

    @Test
    void insertAndList() {
        Instant now = Instant.parse("2026-04-13T12:00:00Z");
        UUID caseId = insertCase(now);
        SqliteAuditRepository repo = storage.auditRepository();

        AuditEntry claimed = new AuditEntry(UUID.randomUUID(), caseId,
                UUID.randomUUID(), AuditEventType.CASE_CLAIMED,
                Map.of("server", "survival"), now);
        AuditEntry resolved = new AuditEntry(UUID.randomUUID(), caseId,
                UUID.randomUUID(), AuditEventType.CASE_RESOLVED_ACCEPTED,
                Map.of("reason", "confirmed"), now.plusSeconds(60));

        repo.insert(claimed);
        repo.insert(resolved);

        var entries = repo.findByCase(caseId);
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).eventType()).isEqualTo(AuditEventType.CASE_CLAIMED);
        assertThat(entries.get(0).payload()).containsEntry("server", "survival");
        assertThat(entries.get(1).eventType()).isEqualTo(AuditEventType.CASE_RESOLVED_ACCEPTED);
    }

    @Test
    void insertWithoutActorAllowedForSystemEvents() {
        Instant now = Instant.parse("2026-04-13T12:00:00Z");
        UUID caseId = insertCase(now);
        SqliteAuditRepository repo = storage.auditRepository();

        AuditEntry systemEntry = new AuditEntry(UUID.randomUUID(), caseId,
                null, AuditEventType.CASE_CREATED, Map.of(), now);
        repo.insert(systemEntry);

        var entries = repo.findByCase(caseId);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).actorId()).isNull();
    }

    private UUID insertCase(Instant now) {
        UUID caseId = UUID.randomUUID();
        storage.caseRepository().insert(new Case(caseId, UUID.randomUUID(), "chat", CaseStatus.UNCLAIMED,
                null, null, null, null, null, now, now));
        return caseId;
    }
}
