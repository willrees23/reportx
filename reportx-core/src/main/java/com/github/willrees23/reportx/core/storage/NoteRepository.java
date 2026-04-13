package com.github.willrees23.reportx.core.storage;

import com.github.willrees23.reportx.core.model.Note;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteRepository {

    void insert(Note note);

    void update(Note note);

    void delete(UUID id);

    Optional<Note> findById(UUID id);

    List<Note> findByCase(UUID caseId);
}
