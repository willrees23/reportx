package com.github.willrees23.reportx.storage.sqlite;

import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import com.github.willrees23.reportx.core.model.Note;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteNoteRepositoryTest extends SqliteTestBase {

    @Test
    void insertUpdateDelete() {
        Instant now = Instant.parse("2026-04-13T12:00:00Z");
        UUID caseId = insertCase(now);
        SqliteNoteRepository repo = storage.noteRepository();

        Note note = new Note(UUID.randomUUID(), caseId, "first look", UUID.randomUUID(), now, null);
        repo.insert(note);

        assertThat(repo.findByCase(caseId)).containsExactly(note);

        Note edited = new Note(note.id(), caseId, "first look (revised)", note.authorId(), now, now.plusSeconds(5));
        repo.update(edited);
        assertThat(repo.findById(note.id())).contains(edited);

        repo.delete(note.id());
        assertThat(repo.findById(note.id())).isEmpty();
    }

    private UUID insertCase(Instant now) {
        UUID caseId = UUID.randomUUID();
        storage.caseRepository().insert(new Case(caseId, UUID.randomUUID(), "chat", CaseStatus.UNCLAIMED,
                null, null, null, null, null, now, now));
        return caseId;
    }
}
