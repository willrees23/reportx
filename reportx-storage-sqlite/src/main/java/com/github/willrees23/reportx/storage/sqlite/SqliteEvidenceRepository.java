package com.github.willrees23.reportx.storage.sqlite;

import com.github.willrees23.reportx.core.model.Evidence;
import com.github.willrees23.reportx.core.storage.EvidenceRepository;
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

public final class SqliteEvidenceRepository implements EvidenceRepository {

    private static final String COLUMNS = "id, case_id, label, content, author_id, created_at, edited_at";

    private final SqliteStorage storage;

    public SqliteEvidenceRepository(SqliteStorage storage) {
        this.storage = storage;
    }

    @Override
    public void insert(Evidence evidence) {
        String sql = "INSERT INTO evidence (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, evidence.id().toString());
            statement.setString(2, evidence.caseId().toString());
            statement.setString(3, evidence.label());
            statement.setString(4, evidence.content());
            statement.setString(5, evidence.authorId().toString());
            statement.setLong(6, evidence.createdAt().toEpochMilli());
            SqliteCaseRepository.setNullableInstant(statement, 7, evidence.editedAt());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to insert evidence " + evidence.id(), ex);
        }
    }

    @Override
    public void update(Evidence evidence) {
        String sql = "UPDATE evidence SET label = ?, content = ?, edited_at = ? WHERE id = ?";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, evidence.label());
            statement.setString(2, evidence.content());
            SqliteCaseRepository.setNullableInstant(statement, 3, evidence.editedAt());
            statement.setString(4, evidence.id().toString());
            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new StorageException("No evidence with id " + evidence.id());
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to update evidence " + evidence.id(), ex);
        }
    }

    @Override
    public void delete(UUID id) {
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM evidence WHERE id = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to delete evidence " + id, ex);
        }
    }

    @Override
    public Optional<Evidence> findById(UUID id) {
        String sql = "SELECT " + COLUMNS + " FROM evidence WHERE id = ?";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to read evidence " + id, ex);
        }
    }

    @Override
    public List<Evidence> findByCase(UUID caseId) {
        String sql = "SELECT " + COLUMNS + " FROM evidence WHERE case_id = ? ORDER BY created_at";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, caseId.toString());
            List<Evidence> out = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
            return out;
        } catch (SQLException ex) {
            throw new StorageException("Failed to list evidence for case " + caseId, ex);
        }
    }

    private Evidence map(ResultSet rs) throws SQLException {
        return new Evidence(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("case_id")),
                rs.getString("label"),
                rs.getString("content"),
                UUID.fromString(rs.getString("author_id")),
                Instant.ofEpochMilli(rs.getLong("created_at")),
                SqliteCaseRepository.readNullableInstant(rs, "edited_at")
        );
    }
}
