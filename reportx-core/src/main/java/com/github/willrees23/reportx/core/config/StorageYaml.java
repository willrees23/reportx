package com.github.willrees23.reportx.core.config;

public record StorageYaml(
        String backend,
        Sqlite sqlite,
        Sql mysql,
        Sql postgres
) {

    public record Sqlite(String file) {
    }

    public record Sql(
            String host,
            int port,
            String database,
            String username,
            String password,
            int poolSize
    ) {
    }
}
