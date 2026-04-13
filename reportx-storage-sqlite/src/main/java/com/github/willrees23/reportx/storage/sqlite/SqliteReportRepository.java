package com.github.willrees23.reportx.storage.sqlite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.willrees23.reportx.core.model.Coords;
import com.github.willrees23.reportx.core.model.Report;
import com.github.willrees23.reportx.core.storage.ReportRepository;
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

public final class SqliteReportRepository implements ReportRepository {

    private final SqliteStorage storage;
    private final ObjectMapper mapper;

    public SqliteReportRepository(SqliteStorage storage, ObjectMapper mapper) {
        this.storage = storage;
        this.mapper = mapper;
    }

    @Override
    public void insert(Report report) {
        String sql = "INSERT INTO reports (id, case_id, reporter_id, target_id, category, detail, " +
                "server_name, reporter_coords, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, report.id().toString());
            statement.setString(2, report.caseId().toString());
            statement.setString(3, report.reporterId().toString());
            statement.setString(4, report.targetId().toString());
            statement.setString(5, report.category());
            statement.setString(6, report.detail());
            statement.setString(7, report.serverName());
            statement.setString(8, mapper.writeValueAsString(report.reporterCoords()));
            statement.setLong(9, report.createdAt().toEpochMilli());
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new StorageException("Failed to insert report " + report.id(), ex);
        }
    }

    @Override
    public Optional<Report> findById(UUID id) {
        String sql = "SELECT id, case_id, reporter_id, target_id, category, detail, " +
                "server_name, reporter_coords, created_at FROM reports WHERE id = ?";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to read report " + id, ex);
        }
    }

    @Override
    public List<Report> findByCase(UUID caseId) {
        String sql = "SELECT id, case_id, reporter_id, target_id, category, detail, " +
                "server_name, reporter_coords, created_at FROM reports WHERE case_id = ? ORDER BY created_at";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, caseId.toString());
            return runList(statement);
        } catch (SQLException ex) {
            throw new StorageException("Failed to list reports for case " + caseId, ex);
        }
    }

    @Override
    public List<Report> findByTargetSince(UUID targetId, Instant since) {
        String sql = "SELECT id, case_id, reporter_id, target_id, category, detail, " +
                "server_name, reporter_coords, created_at FROM reports " +
                "WHERE target_id = ? AND created_at >= ? ORDER BY created_at";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetId.toString());
            statement.setLong(2, since.toEpochMilli());
            return runList(statement);
        } catch (SQLException ex) {
            throw new StorageException("Failed to list reports for target " + targetId, ex);
        }
    }

    @Override
    public int countByTargetSince(UUID targetId, Instant since) {
        String sql = "SELECT COUNT(*) FROM reports WHERE target_id = ? AND created_at >= ?";
        try (Connection connection = storage.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetId.toString());
            statement.setLong(2, since.toEpochMilli());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to count reports for target " + targetId, ex);
        }
    }

    private List<Report> runList(PreparedStatement statement) throws SQLException {
        List<Report> out = new ArrayList<>();
        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        }
        return out;
    }

    private Report map(ResultSet rs) throws SQLException {
        try {
            Coords coords = mapper.readValue(rs.getString("reporter_coords"), Coords.class);
            return new Report(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("case_id")),
                    UUID.fromString(rs.getString("reporter_id")),
                    UUID.fromString(rs.getString("target_id")),
                    rs.getString("category"),
                    rs.getString("detail"),
                    rs.getString("server_name"),
                    coords,
                    Instant.ofEpochMilli(rs.getLong("created_at"))
            );
        } catch (Exception ex) {
            throw new SQLException("Failed to map report row", ex);
        }
    }
}
