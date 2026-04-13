package com.github.willrees23.reportx.paper.notes;

import com.github.willrees23.reportx.core.messaging.events.NoteAddedEvent;
import com.github.willrees23.reportx.core.messaging.events.NoteDeletedEvent;
import com.github.willrees23.reportx.core.messaging.events.NoteEditedEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportXEvent;
import com.github.willrees23.reportx.core.model.Note;
import com.github.willrees23.reportx.core.storage.NoteRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.reportx.messaging.local.LocalMessageBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NoteServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-13T12:00:00Z");

    private InMemoryNoteRepo repo;
    private LocalMessageBus bus;
    private List<ReportXEvent> received;
    private NoteService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryNoteRepo();
        bus = new LocalMessageBus();
        received = new ArrayList<>();
        bus.subscribe(ReportXEvent.class, received::add);
        service = new NoteService(repo, bus, Clock.fixed(NOW));
    }

    @Test
    void add_storesNoteAndPublishesAddedEvent() {
        UUID caseId = UUID.randomUUID();
        UUID author = UUID.randomUUID();

        NoteOutcome outcome = service.add(caseId, "first impression: looks legit", author);

        assertThat(outcome).isInstanceOf(NoteOutcome.Success.class);
        Note note = ((NoteOutcome.Success) outcome).note();
        assertThat(note.caseId()).isEqualTo(caseId);
        assertThat(note.body()).isEqualTo("first impression: looks legit");
        assertThat(note.authorId()).isEqualTo(author);
        assertThat(note.createdAt()).isEqualTo(NOW);
        assertThat(note.editedAt()).isNull();
        assertThat(repo.byId).containsKey(note.id());
        assertThat(received).singleElement().isInstanceOf(NoteAddedEvent.class);
    }

    @Test
    void add_blankBodyRejected() {
        NoteOutcome outcome = service.add(UUID.randomUUID(), "   ", UUID.randomUUID());

        assertThat(outcome).isInstanceOf(NoteOutcome.EmptyBody.class);
        assertThat(repo.byId).isEmpty();
        assertThat(received).isEmpty();
    }

    @Test
    void edit_authorCanUpdateOwnNote() {
        UUID author = UUID.randomUUID();
        Note original = ((NoteOutcome.Success) service.add(UUID.randomUUID(), "v1", author)).note();
        received.clear();

        NoteOutcome outcome = service.edit(original.id(), "v2", author);

        assertThat(outcome).isInstanceOf(NoteOutcome.Success.class);
        Note updated = ((NoteOutcome.Success) outcome).note();
        assertThat(updated.body()).isEqualTo("v2");
        assertThat(updated.editedAt()).isEqualTo(NOW);
        assertThat(received).singleElement().isInstanceOf(NoteEditedEvent.class);
    }

    @Test
    void edit_nonAuthorIsBlocked() {
        Note original = ((NoteOutcome.Success) service.add(
                UUID.randomUUID(), "v1", UUID.randomUUID())).note();

        NoteOutcome outcome = service.edit(original.id(), "v2", UUID.randomUUID());

        assertThat(outcome).isInstanceOf(NoteOutcome.NotAuthor.class);
    }

    @Test
    void edit_unknownIdReturnsNotFound() {
        NoteOutcome outcome = service.edit(UUID.randomUUID(), "v2", UUID.randomUUID());

        assertThat(outcome).isInstanceOf(NoteOutcome.NotFound.class);
    }

    @Test
    void edit_blankBodyRejected() {
        UUID author = UUID.randomUUID();
        Note original = ((NoteOutcome.Success) service.add(UUID.randomUUID(), "v1", author)).note();

        NoteOutcome outcome = service.edit(original.id(), "  ", author);

        assertThat(outcome).isInstanceOf(NoteOutcome.EmptyBody.class);
    }

    @Test
    void delete_authorRemovesAndPublishesEvent() {
        UUID author = UUID.randomUUID();
        Note original = ((NoteOutcome.Success) service.add(UUID.randomUUID(), "v1", author)).note();
        received.clear();

        NoteOutcome outcome = service.delete(original.id(), author, false);

        assertThat(outcome).isInstanceOf(NoteOutcome.Success.class);
        assertThat(repo.byId).doesNotContainKey(original.id());
        assertThat(received).singleElement().isInstanceOf(NoteDeletedEvent.class);
    }

    @Test
    void delete_nonAuthorBlockedWithoutAdminOverride() {
        Note original = ((NoteOutcome.Success) service.add(
                UUID.randomUUID(), "v1", UUID.randomUUID())).note();

        NoteOutcome outcome = service.delete(original.id(), UUID.randomUUID(), false);

        assertThat(outcome).isInstanceOf(NoteOutcome.NotAuthor.class);
        assertThat(repo.byId).containsKey(original.id());
    }

    @Test
    void delete_adminOverrideAllowsRemovingOthersNote() {
        UUID author = UUID.randomUUID();
        Note original = ((NoteOutcome.Success) service.add(UUID.randomUUID(), "v1", author)).note();

        NoteOutcome outcome = service.delete(original.id(), UUID.randomUUID(), true);

        assertThat(outcome).isInstanceOf(NoteOutcome.Success.class);
        assertThat(repo.byId).doesNotContainKey(original.id());
    }

    @Test
    void listForCaseReturnsNotes() {
        UUID caseId = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        service.add(caseId, "n1", author);
        service.add(caseId, "n2", author);
        service.add(UUID.randomUUID(), "elsewhere", author);

        assertThat(service.listForCase(caseId)).hasSize(2);
    }

    private static final class InMemoryNoteRepo implements NoteRepository {

        final Map<UUID, Note> byId = new HashMap<>();

        @Override
        public void insert(Note note) {
            byId.put(note.id(), note);
        }

        @Override
        public void update(Note note) {
            byId.put(note.id(), note);
        }

        @Override
        public void delete(UUID id) {
            byId.remove(id);
        }

        @Override
        public Optional<Note> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Note> findByCase(UUID caseId) {
            return byId.values().stream().filter(n -> n.caseId().equals(caseId)).toList();
        }
    }
}
