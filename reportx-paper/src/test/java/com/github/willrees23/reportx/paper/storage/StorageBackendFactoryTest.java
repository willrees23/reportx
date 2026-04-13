package com.github.willrees23.reportx.paper.storage;

import com.github.willrees23.reportx.core.config.StorageYaml;
import com.github.willrees23.reportx.storage.sqlite.SqliteStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageBackendFactoryTest {

    @TempDir
    Path dataDir;

    @Test
    void create_sqliteWithRelativePathResolvesAgainstDataDir() {
        StorageYaml config = sqliteConfig("reports.db");

        SqliteStorage storage = StorageBackendFactory.create(config, dataDir);

        try {
            assertThat(storage.databasePath()).isEqualTo(dataDir.resolve("reports.db"));
            assertThat(Files.exists(storage.databasePath())).isTrue();
        } finally {
            storage.close();
        }
    }

    @Test
    void create_sqliteWithAbsolutePathUsesPathAsIs() throws Exception {
        Path absolute = Files.createTempFile("reportx-absolute-", ".db");
        Files.deleteIfExists(absolute);
        StorageYaml config = sqliteConfig(absolute.toString());

        SqliteStorage storage = StorageBackendFactory.create(config, dataDir);

        try {
            assertThat(storage.databasePath()).isEqualTo(absolute);
        } finally {
            storage.close();
            Files.deleteIfExists(absolute);
            Files.deleteIfExists(Path.of(absolute + "-wal"));
            Files.deleteIfExists(Path.of(absolute + "-shm"));
        }
    }

    @Test
    void create_mysqlBackendNotYetSupported() {
        StorageYaml config = new StorageYaml("mysql", null, null, null);

        assertThatThrownBy(() -> StorageBackendFactory.create(config, dataDir))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("milestone 3");
    }

    @Test
    void create_unknownBackendIsRejected() {
        StorageYaml config = new StorageYaml("nosuch", null, null, null);

        assertThatThrownBy(() -> StorageBackendFactory.create(config, dataDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown storage backend");
    }

    @Test
    void create_missingBackendKeyIsRejected() {
        StorageYaml config = new StorageYaml(null, null, null, null);

        assertThatThrownBy(() -> StorageBackendFactory.create(config, dataDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("backend");
    }

    private StorageYaml sqliteConfig(String file) {
        return new StorageYaml("sqlite", new StorageYaml.Sqlite(file), null, null);
    }
}
