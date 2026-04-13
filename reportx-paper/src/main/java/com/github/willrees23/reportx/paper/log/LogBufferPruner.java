package com.github.willrees23.reportx.paper.log;

import com.github.willrees23.reportx.core.config.ConfigYaml;
import com.github.willrees23.reportx.core.model.LogEntryType;
import com.github.willrees23.reportx.core.storage.LogBufferRepository;
import com.github.willrees23.reportx.core.util.Clock;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class LogBufferPruner {

    private final LogBufferRepository repo;
    private final Clock clock;

    public LogBufferPruner(LogBufferRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    public PruneSummary prune(ConfigYaml.BufferConfig config) {
        Instant now = clock.now();
        int chat = repo.pruneOlderThan(LogEntryType.CHAT,
                now.minus(config.chatRetentionHours(), ChronoUnit.HOURS));
        int command = repo.pruneOlderThan(LogEntryType.COMMAND,
                now.minus(config.commandsRetentionHours(), ChronoUnit.HOURS));
        int connection = repo.pruneOlderThan(LogEntryType.CONNECTION,
                now.minus(config.connectionsRetentionDays(), ChronoUnit.DAYS));
        return new PruneSummary(chat, command, connection);
    }

    public record PruneSummary(int chatRemoved, int commandRemoved, int connectionRemoved) {

        public int total() {
            return chatRemoved + commandRemoved + connectionRemoved;
        }
    }
}
