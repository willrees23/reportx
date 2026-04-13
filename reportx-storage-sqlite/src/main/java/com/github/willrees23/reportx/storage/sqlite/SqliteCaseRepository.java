package com.github.willrees23.reportx.storage.sqlite;

import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import com.github.willrees23.reportx.core.storage.CaseRepository;
import com.github.willrees23.reportx.core.storage.StorageException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteCaseRepository implements CaseRepository {

    private static final String SELECT_COLUMNS =
            "id, target_id, category, status, claimed_by, claimed_at, resolved_by, " +
                    "resolved_at, resolution_reason, created_at, last_activity_at";

    private final SqliteStorage storage;

    public SqliteCaseRepository(SqliteStorage storage) {
        this.storage = storage;
    }

    @Override
    public void insert(Case value) {
        String sql = "INSERT INTO cases (" + SELECT_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindAll(statement, value);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to insert case " + value.id(), ex);
        }
    }

    @Override
    public void update(Case value) {
        String sql = "UPDATE cases SET target_id = ?, category = ?, status = ?, claimed_by = ?, " +
                "claimed_at = ?, resolved_by = ?, resolved_at = ?, resolution_reason = ?, " +
                "created_at = ?, last_activity_at = ? WHERE id = ?";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value.targetId().toString());
            statement.setString(2, value.category());
            statement.setString(3, value.status().name());
            setNullableUuid(statement, 4, value.claimedBy());
            setNullableInstant(statement, 5, value.claimedAt());
            setNullableUuid(statement, 6, value.resolvedBy());
            setNullableInstant(statement, 7, value.resolvedAt());
            statement.setString(8, value.resolutionReason());
            statement.setLong(9, value.createdAt().toEpochMilli());
            statement.setLong(10, value.lastActivityAt().toEpochMilli());
            statement.setString(11, value.id().toString());
            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new StorageException("No case with id " + value.id());
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to update case " + value.id(), ex);
        }
    }

    @Override
    public Optional<Case> findById(UUID id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM cases WHERE id = ?";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to read case " + id, ex);
        }
    }

    @Override
    public Optional<Case> findOpenDedupCandidate(UUID targetId, String category, Instant notBefore) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM cases " +
                "WHERE target_id = ? AND category = ? AND status IN ('UNCLAIMED', 'CLAIMED') " +
                "AND created_at >= ? ORDER BY created_at DESC LIMIT 1";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetId.toString());
            statement.setString(2, category);
            statement.setLong(3, notBefore.toEpochMilli());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to find dedup candidate", ex);
        }
    }

    @Override
    public List<Case> findByStatus(CaseStatus status) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM cases WHERE status = ? ORDER BY created_at";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            return runList(statement);
        } catch (SQLException ex) {
            throw new StorageException("Failed to list cases by status " + status, ex);
        }
    }

    @Override
    public Optional<Case> findOldestUnclaimedByCategory(String category) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM cases " +
                "WHERE status = 'UNCLAIMED' AND category = ? ORDER BY created_at ASC LIMIT 1";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to find oldest unclaimed", ex);
        }
    }

    @Override
    public List<Case> findClaimedBy(UUID staffId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM cases " +
                "WHERE status = 'CLAIMED' AND claimed_by = ? ORDER BY claimed_at";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, staffId.toString());
            return runList(statement);
        } catch (SQLException ex) {
            throw new StorageException("Failed to list cases claimed by " + staffId, ex);
        }
    }

    private void bindAll(PreparedStatement statement, Case value) throws SQLException {
        statement.setString(1, value.id().toString());
        statement.setString(2, value.targetId().toString());
        statement.setString(3, value.category());
        statement.setString(4, value.status().name());
        setNullableUuid(statement, 5, value.claimedBy());
        setNullableInstant(statement, 6, value.claimedAt());
        setNullableUuid(statement, 7, value.resolvedBy());
        setNullableInstant(statement, 8, value.resolvedAt());
        statement.setString(9, value.resolutionReason());
        statement.setLong(10, value.createdAt().toEpochMilli());
        statement.setLong(11, value.lastActivityAt().toEpochMilli());
    }

    private List<Case> runList(PreparedStatement statement) throws SQLException {
        List<Case> out = new ArrayList<>();
        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        }
        return out;
    }

    private Case map(ResultSet rs) throws SQLException {
        return new Case(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("target_id")),
                rs.getString("category"),
                CaseStatus.valueOf(rs.getString("status")),
                readNullableUuid(rs, "claimed_by"),
                readNullableInstant(rs, "claimed_at"),
                readNullableUuid(rs, "resolved_by"),
                readNullableInstant(rs, "resolved_at"),
                rs.getString("resolution_reason"),
                Instant.ofEpochMilli(rs.getLong("created_at")),
                Instant.ofEpochMilli(rs.getLong("last_activity_at"))
        );
    }

    static void setNullableUuid(PreparedStatement statement, int index, UUID uuid) throws SQLException {
        if (uuid == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, uuid.toString());
        }
    }

    static void setNullableInstant(PreparedStatement statement, int index, Instant instant) throws SQLException {
        if (instant == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setLong(index, instant.toEpochMilli());
        }
    }

    static UUID readNullableUuid(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : UUID.fromString(value);
    }

    static Instant readNullableInstant(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Instant.ofEpochMilli(value);
    }
}
