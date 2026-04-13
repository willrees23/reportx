package com.github.willrees23.reportx.storage.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

abstract class SqliteTestBase {

    protected Path databasePath;
    protected SqliteStorage storage;

    @BeforeEach
    void setUpDatabase() throws IOException {
        Path temp = Files.createTempFile("reportx-test-", ".db");
        Files.deleteIfExists(temp);
        this.databasePath = temp;
        this.storage = new SqliteStorage(temp);
        this.storage.initialise();
    }

    @AfterEach
    void tearDownDatabase() throws IOException {
        if (storage != null) {
            storage.close();
        }
        Files.deleteIfExists(databasePath);
        Files.deleteIfExists(Path.of(databasePath + "-wal"));
        Files.deleteIfExists(Path.of(databasePath + "-shm"));
    }
}
