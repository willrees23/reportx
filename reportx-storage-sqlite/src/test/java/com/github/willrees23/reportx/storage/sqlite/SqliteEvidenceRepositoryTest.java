package com.github.willrees23.reportx.storage.sqlite;

import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import com.github.willrees23.reportx.core.model.Evidence;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteEvidenceRepositoryTest extends SqliteTestBase {

    @Test
    void insertUpdateDeleteRoundTrip() {
        Instant now = Instant.parse("2026-04-13T12:00:00Z");
        UUID caseId = insertCase(now);
        SqliteEvidenceRepository repo = storage.evidenceRepository();

        Evidence evidence = new Evidence(UUID.randomUUID(), caseId,
                "Spam 1", "http://example.com/img.png",
                UUID.randomUUID(), now, null);
        repo.insert(evidence);

        assertThat(repo.findById(evidence.id())).contains(evidence);
        assertThat(repo.findByCase(caseId)).containsExactly(evidence);

        Evidence edited = new Evidence(evidence.id(), caseId,
                "Spam 1 (edited)", "http://example.com/img.png",
                evidence.authorId(), now, now.plusSeconds(10));
        repo.update(edited);
        assertThat(repo.findById(evidence.id())).contains(edited);

        repo.delete(evidence.id());
        assertThat(repo.findById(evidence.id())).isEmpty();
    }

    private UUID insertCase(Instant now) {
        UUID caseId = UUID.randomUUID();
        storage.caseRepository().insert(new Case(caseId, UUID.randomUUID(), "chat", CaseStatus.UNCLAIMED,
                null, null, null, null, null, now, now));
        return caseId;
    }
}
