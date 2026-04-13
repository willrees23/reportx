package com.github.willrees23.reportx.storage.sqlite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.willrees23.reportx.core.storage.StorageException;
import com.github.willrees23.reportx.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class SqliteStorage implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SqliteStorage.class);
    private static final String MIGRATIONS_RESOURCE_ROOT = "db/migrations";

    private final SQLiteDataSource dataSource;
    private final ObjectMapper mapper = Json.newMapper();
    private final Path databasePath;

    public SqliteStorage(Path databasePath) {
        this.databasePath = databasePath;
        this.dataSource = new SQLiteDataSource();
        this.dataSource.setUrl("jdbc:sqlite:" + databasePath.toAbsolutePath());
    }

    public void initialise() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");

            ensureSchemaVersionTable(connection);
            applyMigrations(connection);
        } catch (SQLException | IOException ex) {
            throw new StorageException("Failed to initialise SQLite storage", ex);
        }
    }

    private void ensureSchemaVersionTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS schema_version (" +
                            "version INTEGER PRIMARY KEY, " +
                            "applied_at INTEGER NOT NULL)");
        }
    }

    private void applyMigrations(Connection connection) throws SQLException, IOException {
        Set<Integer> applied = readAppliedVersions(connection);
        List<Migration> migrations = loadMigrations();
        for (Migration migration : migrations) {
            if (applied.contains(migration.version())) {
                continue;
            }
            LOG.info("Applying migration V{}: {}", migration.version(), migration.name());
            runMigration(connection, migration);
        }
    }

    private Set<Integer> readAppliedVersions(Connection connection) throws SQLException {
        Set<Integer> versions = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT version FROM schema_version")) {
            while (resultSet.next()) {
                versions.add(resultSet.getInt(1));
            }
        }
        return versions;
    }

    private void runMigration(Connection connection, Migration migration) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            for (String piece : splitStatements(migration.sql())) {
                if (!piece.isBlank()) {
                    statement.execute(piece);
                }
            }
            try (var insert = connection.prepareStatement(
                    "INSERT INTO schema_version (version, applied_at) VALUES (?, ?)")) {
                insert.setInt(1, migration.version());
                insert.setLong(2, System.currentTimeMillis());
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private List<String> splitStatements(String sql) {
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : sql.split("\\R")) {
            if (line.stripLeading().startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (line.stripTrailing().endsWith(";")) {
                pieces.add(current.toString().trim());
                current.setLength(0);
            }
        }
        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            pieces.add(remaining);
        }
        return pieces;
    }

    private List<Migration> loadMigrations() throws IOException {
        List<Migration> migrations = new ArrayList<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = SqliteStorage.class.getClassLoader();
        }

        // SQLite module ships one migration file today; scan classpath explicitly.
        // We read the file list from a bundled manifest to avoid fragile directory walking.
        // If the manifest is absent (tests / alt packaging), fall back to known file.
        List<String> files = readMigrationsManifest(loader);
        if (files.isEmpty()) {
            URL singleFile = loader.getResource(MIGRATIONS_RESOURCE_ROOT + "/V1__initial_schema.sql");
            if (singleFile != null) {
                files = List.of("V1__initial_schema.sql");
            }
        }
        for (String file : files) {
            URL resource = loader.getResource(MIGRATIONS_RESOURCE_ROOT + "/" + file);
            if (resource == null) {
                continue;
            }
            int version = parseVersion(file);
            String name = parseName(file);
            String sql = readResource(resource);
            migrations.add(new Migration(version, name, sql));
        }
        migrations.sort((a, b) -> Integer.compare(a.version(), b.version()));
        return migrations;
    }

    private List<String> readMigrationsManifest(ClassLoader loader) throws IOException {
        URL manifest = loader.getResource(MIGRATIONS_RESOURCE_ROOT + "/manifest.txt");
        if (manifest == null) {
            return Collections.emptyList();
        }
        try (InputStream in = manifest.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toList());
        }
    }

    private int parseVersion(String fileName) {
        String prefix = fileName.startsWith("V") ? fileName.substring(1) : fileName;
        int end = prefix.indexOf("__");
        if (end < 0) {
            throw new StorageException("Migration filename missing version: " + fileName);
        }
        try {
            return Integer.parseInt(prefix.substring(0, end));
        } catch (NumberFormatException ex) {
            throw new StorageException("Migration filename has non-numeric version: " + fileName, ex);
        }
    }

    private String parseName(String fileName) {
        int end = fileName.indexOf("__");
        if (end < 0) {
            return fileName;
        }
        String tail = fileName.substring(end + 2);
        return tail.endsWith(".sql") ? tail.substring(0, tail.length() - 4) : tail;
    }

    private String readResource(URL resource) throws IOException {
        try (InputStream in = resource.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException ex) {
            throw new StorageException("Failed to open SQLite connection", ex);
        }
    }

    public SqliteReportRepository reportRepository() {
        return new SqliteReportRepository(this, mapper);
    }

    public SqliteCaseRepository caseRepository() {
        return new SqliteCaseRepository(this);
    }

    public SqliteEvidenceRepository evidenceRepository() {
        return new SqliteEvidenceRepository(this);
    }

    public SqliteNoteRepository noteRepository() {
        return new SqliteNoteRepository(this);
    }

    public SqliteAuditRepository auditRepository() {
        return new SqliteAuditRepository(this, mapper);
    }

    public SqliteLogBufferRepository logBufferRepository() {
        return new SqliteLogBufferRepository(this);
    }

    public Path databasePath() {
        return databasePath;
    }

    @Override
    public void close() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(TRUNCATE)");
        } catch (SQLException ex) {
            LOG.warn("Failed to checkpoint SQLite WAL during close", ex);
        }
    }

    public boolean exists() {
        return Files.exists(databasePath);
    }

    record Migration(int version, String name, String sql) {
    }
}
