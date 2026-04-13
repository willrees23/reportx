package com.github.willrees23.reportx.storage.sqlite;

import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import com.github.willrees23.reportx.core.model.Coords;
import com.github.willrees23.reportx.core.model.Report;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteReportRepositoryTest extends SqliteTestBase {

    @Test
    void insertReadAndCount() {
        UUID target = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-13T12:00:00Z");
        UUID caseId = insertCase(target, "chat", now);

        SqliteReportRepository repo = storage.reportRepository();
        Report report = sampleReport(caseId, target, now);
        repo.insert(report);

        assertThat(repo.findById(report.id())).contains(report);
        assertThat(repo.findByCase(caseId)).containsExactly(report);
        assertThat(repo.countByTargetSince(target, now.minusSeconds(60))).isEqualTo(1);
    }

    @Test
    void findByTargetSince_filtersOutOlderReports() {
        UUID target = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-13T12:00:00Z");
        UUID caseId = insertCase(target, "chat", now);

        SqliteReportRepository repo = storage.reportRepository();
        Report fresh = sampleReport(caseId, target, now);
        Report stale = sampleReport(caseId, target, now.minusSeconds(3600));
        repo.insert(fresh);
        repo.insert(stale);

        var result = repo.findByTargetSince(target, now.minusSeconds(300));
        assertThat(result).containsExactly(fresh);
    }

    private UUID insertCase(UUID target, String category, Instant now) {
        UUID caseId = UUID.randomUUID();
        storage.caseRepository().insert(new Case(caseId, target, category, CaseStatus.UNCLAIMED,
                null, null, null, null, null, now, now));
        return caseId;
    }

    private Report sampleReport(UUID caseId, UUID target, Instant createdAt) {
        return new Report(
                UUID.randomUUID(),
                caseId,
                UUID.randomUUID(),
                target,
                "chat",
                "spamming",
                "survival",
                new Coords("world", 10.0, 64.0, -5.5),
                createdAt
        );
    }
}
