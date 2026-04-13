package com.github.willrees23.reportx.storage.sqlite;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.willrees23.reportx.core.model.AuditEntry;
import com.github.willrees23.reportx.core.model.AuditEventType;
import com.github.willrees23.reportx.core.storage.AuditRepository;
import com.github.willrees23.reportx.core.storage.StorageException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SqliteAuditRepository implements AuditRepository {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final SqliteStorage storage;
    private final ObjectMapper mapper;

    public SqliteAuditRepository(SqliteStorage storage, ObjectMapper mapper) {
        this.storage = storage;
        this.mapper = mapper;
    }

    @Override
    public void insert(AuditEntry entry) {
        String sql = "INSERT INTO audit_entries (id, case_id, actor_id, event_type, payload, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.id().toString());
            statement.setString(2, entry.caseId().toString());
            if (entry.actorId() == null) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, entry.actorId().toString());
            }
            statement.setString(4, entry.eventType().name());
            statement.setString(5, mapper.writeValueAsString(entry.payload() == null ? Map.of() : entry.payload()));
            statement.setLong(6, entry.createdAt().toEpochMilli());
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new StorageException("Failed to insert audit entry " + entry.id(), ex);
        }
    }

    @Override
    public List<AuditEntry> findByCase(UUID caseId) {
        String sql = "SELECT id, case_id, actor_id, event_type, payload, created_at " +
                "FROM audit_entries WHERE case_id = ? ORDER BY created_at";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, caseId.toString());
            List<AuditEntry> out = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
            return out;
        } catch (SQLException ex) {
            throw new StorageException("Failed to list audit entries for case " + caseId, ex);
        }
    }

    private AuditEntry map(ResultSet rs) throws SQLException {
        try {
            String actor = rs.getString("actor_id");
            Map<String, Object> payload = mapper.readValue(rs.getString("payload"), PAYLOAD_TYPE);
            return new AuditEntry(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("case_id")),
                    actor == null ? null : UUID.fromString(actor),
                    AuditEventType.valueOf(rs.getString("event_type")),
                    payload,
                    Instant.ofEpochMilli(rs.getLong("created_at"))
            );
        } catch (Exception ex) {
            throw new SQLException("Failed to map audit row", ex);
        }
    }
}
