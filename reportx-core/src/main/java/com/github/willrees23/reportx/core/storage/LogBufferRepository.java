package com.github.willrees23.reportx.core.storage;

import com.github.willrees23.reportx.core.model.LogBufferEntry;
import com.github.willrees23.reportx.core.model.LogEntryType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LogBufferRepository {

    void insert(LogBufferEntry entry);

    List<LogBufferEntry> findByPlayerSince(UUID playerId, Instant since);

    List<LogBufferEntry> findByPlayerSince(UUID playerId, LogEntryType type, Instant since);

    int pruneOlderThan(LogEntryType type, Instant cutoff);
}
