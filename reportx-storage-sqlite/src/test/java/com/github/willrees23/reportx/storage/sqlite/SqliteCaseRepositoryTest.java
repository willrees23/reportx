package com.github.willrees23.reportx.storage.sqlite;

import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteCaseRepositoryTest extends SqliteTestBase {

    @Test
    void insertAndReadRoundTrip() {
        SqliteCaseRepository repo = storage.caseRepository();
        UUID id = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-13T12:00:00Z");

        Case value = new Case(id, target, "chat", CaseStatus.UNCLAIMED,
                null, null, null, null, null, now, now);
        repo.insert(value);

        assertThat(repo.findById(id)).contains(value);
    }

    @Test
    void update_changesStatusAndClaimedFields() {
        SqliteCaseRepository repo = storage.caseRepository();
        UUID id = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        UUID staff = UUID.randomUUID();
        Instant created = Instant.parse("2026-04-13T12:00:00Z");
        Instant claimed = created.plusSeconds(30);

        repo.insert(new Case(id, target, "chat", CaseStatus.UNCLAIMED,
                null, null, null, null, null, created, created));

        Case claimedValue = new Case(id, target, "chat", CaseStatus.CLAIMED,
                staff, claimed, null, null, null, created, claimed);
        repo.update(claimedValue);

        assertThat(repo.findById(id)).contains(claimedValue);
    }

    @Test
    void findOldestUnclaimedByCategory_returnsFifo() {
        SqliteCaseRepository repo = storage.caseRepository();
        Instant base = Instant.parse("2026-04-13T12:00:00Z");
        UUID first = insertUnclaimed(repo, "chat", base);
        insertUnclaimed(repo, "chat", base.plusSeconds(60));
        insertUnclaimed(repo, "hacking", base.plusSeconds(10));

        Optional<Case> result = repo.findOldestUnclaimedByCategory("chat");
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(first);
    }

    @Test
    void findOpenDedupCandidate_scopesByTargetCategoryAndWindow() {
        SqliteCaseRepository repo = storage.caseRepository();
        UUID target = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-13T12:00:00Z");

        UUID matchId = UUID.randomUUID();
        repo.insert(new Case(matchId, target, "chat", CaseStatus.UNCLAIMED,
                null, null, null, null, null, now, now));

        UUID staleId = UUID.randomUUID();
        repo.insert(new Case(staleId, target, "chat", CaseStatus.UNCLAIMED,
                null, null, null, null, null, now.minusSeconds(3600), now.minusSeconds(3600)));

        Instant windowStart = now.minusSeconds(300);
        Optional<Case> candidate = repo.findOpenDedupCandidate(target, "chat", windowStart);
        assertThat(candidate).map(Case::id).contains(matchId);
    }

    private UUID insertUnclaimed(SqliteCaseRepository repo, String category, Instant createdAt) {
        UUID id = UUID.randomUUID();
        repo.insert(new Case(id, UUID.randomUUID(), category, CaseStatus.UNCLAIMED,
                null, null, null, null, null, createdAt, createdAt));
        return id;
    }
}
