package com.github.willrees23.reportx.paper.log;

import com.github.willrees23.reportx.core.config.ConfigYaml;
import com.github.willrees23.reportx.core.model.LogBufferEntry;
import com.github.willrees23.reportx.core.model.LogEntryType;
import com.github.willrees23.reportx.core.storage.LogBufferRepository;
import com.github.willrees23.reportx.core.util.Clock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LogBufferPrunerTest {

    private static final Instant NOW = Instant.parse("2026-04-13T12:00:00Z");

    @Test
    void prune_removesEntriesOlderThanRetentionPerType() {
        InMemoryRepo repo = new InMemoryRepo();
        UUID player = UUID.randomUUID();

        repo.insert(entry(player, LogEntryType.CHAT, NOW.minus(2, ChronoUnit.HOURS)));
        repo.insert(entry(player, LogEntryType.CHAT, NOW.minus(48, ChronoUnit.HOURS)));
        repo.insert(entry(player, LogEntryType.COMMAND, NOW.minus(1, ChronoUnit.HOURS)));
        repo.insert(entry(player, LogEntryType.COMMAND, NOW.minus(48, ChronoUnit.HOURS)));
        repo.insert(entry(player, LogEntryType.CONNECTION, NOW.minus(5, ChronoUnit.DAYS)));
        repo.insert(entry(player, LogEntryType.CONNECTION, NOW.minus(60, ChronoUnit.DAYS)));

        ConfigYaml.BufferConfig config = new ConfigYaml.BufferConfig(200, 24, 100, 24, 30);
        LogBufferPruner pruner = new LogBufferPruner(repo, Clock.fixed(NOW));

        LogBufferPruner.PruneSummary summary = pruner.prune(config);

        assertThat(summary.chatRemoved()).isEqualTo(1);
        assertThat(summary.commandRemoved()).isEqualTo(1);
        assertThat(summary.connectionRemoved()).isEqualTo(1);
        assertThat(summary.total()).isEqualTo(3);
        assertThat(repo.entries).hasSize(3);
    }

    @Test
    void prune_returnsZeroSummaryWhenNothingExpired() {
        InMemoryRepo repo = new InMemoryRepo();
        UUID player = UUID.randomUUID();
        repo.insert(entry(player, LogEntryType.CHAT, NOW));

        ConfigYaml.BufferConfig config = new ConfigYaml.BufferConfig(200, 24, 100, 24, 30);
        LogBufferPruner pruner = new LogBufferPruner(repo, Clock.fixed(NOW));

        assertThat(pruner.prune(config).total()).isZero();
    }

    private static LogBufferEntry entry(UUID player, LogEntryType type, Instant at) {
        return new LogBufferEntry(player, type, "ignored", "default", at);
    }

    private static final class InMemoryRepo implements LogBufferRepository {

        final List<LogBufferEntry> entries = new ArrayList<>();

        @Override
        public void insert(LogBufferEntry entry) {
            entries.add(entry);
        }

        @Override
        public List<LogBufferEntry> findByPlayerSince(UUID playerId, Instant since) {
            return entries.stream()
                    .filter(e -> e.playerId().equals(playerId) && !e.createdAt().isBefore(since))
                    .toList();
        }

        @Override
        public List<LogBufferEntry> findByPlayerSince(UUID playerId, LogEntryType type, Instant since) {
            return entries.stream()
                    .filter(e -> e.playerId().equals(playerId) && e.type() == type && !e.createdAt().isBefore(since))
                    .toList();
        }

        @Override
        public int pruneOlderThan(LogEntryType type, Instant cutoff) {
            int before = entries.size();
            entries.removeIf(e -> e.type() == type && e.createdAt().isBefore(cutoff));
            return before - entries.size();
        }
    }
}
