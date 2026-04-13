package com.github.willrees23.reportx.paper.log;

import com.github.willrees23.reportx.core.model.LogBufferEntry;
import com.github.willrees23.reportx.core.model.LogEntryType;
import com.github.willrees23.reportx.core.storage.LogBufferRepository;
import com.github.willrees23.reportx.core.util.Clock;

import java.util.UUID;

public final class LogBufferRecorder {

    private final LogBufferRepository repo;
    private final Clock clock;
    private final String serverName;

    public LogBufferRecorder(LogBufferRepository repo, Clock clock, String serverName) {
        this.repo = repo;
        this.clock = clock;
        this.serverName = serverName;
    }

    public void recordChat(UUID playerId, String message) {
        repo.insert(new LogBufferEntry(playerId, LogEntryType.CHAT, message, serverName, clock.now()));
    }

    public void recordCommand(UUID playerId, String command) {
        repo.insert(new LogBufferEntry(playerId, LogEntryType.COMMAND, command, serverName, clock.now()));
    }

    public void recordConnection(UUID playerId, ConnectionType type) {
        repo.insert(new LogBufferEntry(playerId, LogEntryType.CONNECTION, type.name(), serverName, clock.now()));
    }
}
