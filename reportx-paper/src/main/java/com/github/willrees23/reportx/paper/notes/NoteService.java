package com.github.willrees23.reportx.paper.notes;

import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.core.messaging.events.NoteAddedEvent;
import com.github.willrees23.reportx.core.messaging.events.NoteDeletedEvent;
import com.github.willrees23.reportx.core.messaging.events.NoteEditedEvent;
import com.github.willrees23.reportx.core.model.Note;
import com.github.willrees23.reportx.core.storage.NoteRepository;
import com.github.willrees23.reportx.core.util.Clock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class NoteService {

    private final NoteRepository repo;
    private final MessageBus bus;
    private final Clock clock;

    public NoteService(NoteRepository repo, MessageBus bus, Clock clock) {
        this.repo = repo;
        this.bus = bus;
        this.clock = clock;
    }

    public NoteOutcome add(UUID caseId, String body, UUID authorId) {
        if (body == null || body.isBlank()) {
            return new NoteOutcome.EmptyBody();
        }
        Note note = new Note(
                UUID.randomUUID(),
                caseId,
                body,
                authorId,
                clock.now(),
                null);
        repo.insert(note);
        bus.publish(new NoteAddedEvent(note.id(), caseId, authorId));
        return new NoteOutcome.Success(note);
    }

    public NoteOutcome edit(UUID noteId, String newBody, UUID actorId) {
        Optional<Note> existing = repo.findById(noteId);
        if (existing.isEmpty()) {
            return new NoteOutcome.NotFound();
        }
        Note current = existing.get();
        if (!current.authorId().equals(actorId)) {
            return new NoteOutcome.NotAuthor();
        }
        if (newBody == null || newBody.isBlank()) {
            return new NoteOutcome.EmptyBody();
        }
        Note updated = new Note(
                current.id(),
                current.caseId(),
                newBody,
                current.authorId(),
                current.createdAt(),
                clock.now());
        repo.update(updated);
        bus.publish(new NoteEditedEvent(current.id(), current.caseId(), actorId));
        return new NoteOutcome.Success(updated);
    }

    public NoteOutcome delete(UUID noteId, UUID actorId, boolean adminOverride) {
        Optional<Note> existing = repo.findById(noteId);
        if (existing.isEmpty()) {
            return new NoteOutcome.NotFound();
        }
        Note current = existing.get();
        if (!adminOverride && !current.authorId().equals(actorId)) {
            return new NoteOutcome.NotAuthor();
        }
        repo.delete(noteId);
        bus.publish(new NoteDeletedEvent(current.id(), current.caseId(), actorId));
        return new NoteOutcome.Success(current);
    }

    public List<Note> listForCase(UUID caseId) {
        return repo.findByCase(caseId);
    }
}
