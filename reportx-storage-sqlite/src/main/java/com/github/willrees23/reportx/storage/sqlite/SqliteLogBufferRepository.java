package com.github.willrees23.reportx.storage.sqlite;

import com.github.willrees23.reportx.core.model.LogBufferEntry;
import com.github.willrees23.reportx.core.model.LogEntryType;
import com.github.willrees23.reportx.core.storage.LogBufferRepository;
import com.github.willrees23.reportx.core.storage.StorageException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SqliteLogBufferRepository implements LogBufferRepository {

    private final SqliteStorage storage;

    public SqliteLogBufferRepository(SqliteStorage storage) {
        this.storage = storage;
    }

    @Override
    public void insert(LogBufferEntry entry) {
        String sql = "INSERT INTO log_buffer (player_id, type, content, server_name, created_at) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.playerId().toString());
            statement.setString(2, entry.type().name());
            statement.setString(3, entry.content());
            statement.setString(4, entry.serverName());
            statement.setLong(5, entry.createdAt().toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to insert log buffer entry", ex);
        }
    }

    @Override
    public List<LogBufferEntry> findByPlayerSince(UUID playerId, Instant since) {
        String sql = "SELECT player_id, type, content, server_name, created_at FROM log_buffer " +
                "WHERE player_id = ? AND created_at >= ? ORDER BY created_at";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setLong(2, since.toEpochMilli());
            return runList(statement);
        } catch (SQLException ex) {
            throw new StorageException("Failed to list log buffer for player " + playerId, ex);
        }
    }

    @Override
    public List<LogBufferEntry> findByPlayerSince(UUID playerId, LogEntryType type, Instant since) {
        String sql = "SELECT player_id, type, content, server_name, created_at FROM log_buffer " +
                "WHERE player_id = ? AND type = ? AND created_at >= ? ORDER BY created_at";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, type.name());
            statement.setLong(3, since.toEpochMilli());
            return runList(statement);
        } catch (SQLException ex) {
            throw new StorageException("Failed to list log buffer for player " + playerId, ex);
        }
    }

    @Override
    public int pruneOlderThan(LogEntryType type, Instant cutoff) {
        String sql = "DELETE FROM log_buffer WHERE type = ? AND created_at < ?";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type.name());
            statement.setLong(2, cutoff.toEpochMilli());
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to prune log buffer", ex);
        }
    }

    private List<LogBufferEntry> runList(PreparedStatement statement) throws SQLException {
        List<LogBufferEntry> out = new ArrayList<>();
        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                out.add(new LogBufferEntry(
                        UUID.fromString(rs.getString("player_id")),
                        LogEntryType.valueOf(rs.getString("type")),
                        rs.getString("content"),
                        rs.getString("server_name"),
                        Instant.ofEpochMilli(rs.getLong("created_at"))
                ));
            }
        }
        return out;
    }
}
