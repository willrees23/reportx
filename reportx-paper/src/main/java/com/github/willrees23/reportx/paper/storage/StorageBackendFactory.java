package com.github.willrees23.reportx.paper.storage;

import com.github.willrees23.reportx.core.config.StorageYaml;
import com.github.willrees23.reportx.storage.sqlite.SqliteStorage;

import java.nio.file.Path;
import java.util.Locale;

public final class StorageBackendFactory {

    private StorageBackendFactory() {
    }

    public static SqliteStorage createSqlite(StorageYaml config, Path dataDirectory) {
        if (config == null || config.sqlite() == null || config.sqlite().file() == null) {
            throw new IllegalArgumentException("storage.yml is missing the sqlite.file setting");
        }
        Path file = Path.of(config.sqlite().file());
        if (!file.isAbsolute()) {
            file = dataDirectory.resolve(file);
        }
        SqliteStorage storage = new SqliteStorage(file);
        storage.initialise();
        return storage;
    }

    public static SqliteStorage create(StorageYaml config, Path dataDirectory) {
        if (config == null || config.backend() == null) {
            throw new IllegalArgumentException("storage.yml is missing the 'backend' setting");
        }
        String backend = config.backend().toLowerCase(Locale.ROOT);
        return switch (backend) {
            case "sqlite" -> createSqlite(config, dataDirectory);
            case "mysql", "postgres" -> throw new UnsupportedOperationException(
                    "Storage backend '" + backend + "' is not implemented yet (planned for milestone 3)");
            default -> throw new IllegalArgumentException("Unknown storage backend: " + config.backend());
        };
    }
}
