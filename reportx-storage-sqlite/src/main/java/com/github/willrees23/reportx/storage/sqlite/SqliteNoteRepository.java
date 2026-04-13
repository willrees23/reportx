package com.github.willrees23.reportx.storage.sqlite;

import com.github.willrees23.reportx.core.model.Note;
import com.github.willrees23.reportx.core.storage.NoteRepository;
import com.github.willrees23.reportx.core.storage.StorageException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteNoteRepository implements NoteRepository {

    private static final String COLUMNS = "id, case_id, body, author_id, created_at, edited_at";

    private final SqliteStorage storage;

    public SqliteNoteRepository(SqliteStorage storage) {
        this.storage = storage;
    }

    @Override
    public void insert(Note note) {
        String sql = "INSERT INTO notes (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, note.id().toString());
            statement.setString(2, note.caseId().toString());
            statement.setString(3, note.body());
            statement.setString(4, note.authorId().toString());
            statement.setLong(5, note.createdAt().toEpochMilli());
            SqliteCaseRepository.setNullableInstant(statement, 6, note.editedAt());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to insert note " + note.id(), ex);
        }
    }

    @Override
    public void update(Note note) {
        String sql = "UPDATE notes SET body = ?, edited_at = ? WHERE id = ?";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, note.body());
            SqliteCaseRepository.setNullableInstant(statement, 2, note.editedAt());
            statement.setString(3, note.id().toString());
            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new StorageException("No note with id " + note.id());
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to update note " + note.id(), ex);
        }
    }

    @Override
    public void delete(UUID id) {
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM notes WHERE id = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to delete note " + id, ex);
        }
    }

    @Override
    public Optional<Note> findById(UUID id) {
        String sql = "SELECT " + COLUMNS + " FROM notes WHERE id = ?";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to read note " + id, ex);
        }
    }

    @Override
    public List<Note> findByCase(UUID caseId) {
        String sql = "SELECT " + COLUMNS + " FROM notes WHERE case_id = ? ORDER BY created_at";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, caseId.toString());
            List<Note> out = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
            return out;
        } catch (SQLException ex) {
            throw new StorageException("Failed to list notes for case " + caseId, ex);
        }
    }

    private Note map(ResultSet rs) throws SQLException {
        return new Note(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("case_id")),
                rs.getString("body"),
                UUID.fromString(rs.getString("author_id")),
                Instant.ofEpochMilli(rs.getLong("created_at")),
                SqliteCaseRepository.readNullableInstant(rs, "edited_at")
        );
    }
}
