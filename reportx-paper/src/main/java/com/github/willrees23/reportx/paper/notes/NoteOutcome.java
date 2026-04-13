package com.github.willrees23.reportx.paper.notes;

import com.github.willrees23.reportx.core.model.Note;

public sealed interface NoteOutcome {

    record Success(Note note) implements NoteOutcome {
    }

    record NotFound() implements NoteOutcome {
    }

    record NotAuthor() implements NoteOutcome {
    }

    record EmptyBody() implements NoteOutcome {
    }
}
