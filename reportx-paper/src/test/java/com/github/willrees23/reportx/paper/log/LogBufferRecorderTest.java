package com.github.willrees23.reportx.paper.log;

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

class LogBufferRecorderTest {

    private static final Instant NOW = Instant.parse("2026-04-13T12:00:00Z");

    private final RecordingRepository repo = new RecordingRepository();
    private final LogBufferRecorder recorder = new LogBufferRecorder(repo, Clock.fixed(NOW), "survival");

    @Test
    void recordChat_writesChatEntry() {
        UUID player = UUID.randomUUID();

        recorder.recordChat(player, "hello world");

        assertThat(repo.entries).hasSize(1);
        LogBufferEntry entry = repo.entries.get(0);
        assertThat(entry.playerId()).isEqualTo(player);
        assertThat(entry.type()).isEqualTo(LogEntryType.CHAT);
        assertThat(entry.content()).isEqualTo("hello world");
        assertThat(entry.serverName()).isEqualTo("survival");
        assertThat(entry.createdAt()).isEqualTo(NOW);
    }

    @Test
    void recordCommand_writesCommandEntry() {
        UUID player = UUID.randomUUID();

        recorder.recordCommand(player, "/tp Notch");

        assertThat(repo.entries).hasSize(1);
        assertThat(repo.entries.get(0).type()).isEqualTo(LogEntryType.COMMAND);
        assertThat(repo.entries.get(0).content()).isEqualTo("/tp Notch");
    }

    @Test
    void recordConnection_writesConnectionTypeAsContent() {
        UUID player = UUID.randomUUID();

        recorder.recordConnection(player, ConnectionType.JOIN);
        recorder.recordConnection(player, ConnectionType.QUIT);

        assertThat(repo.entries).hasSize(2);
        assertThat(repo.entries.get(0).type()).isEqualTo(LogEntryType.CONNECTION);
        assertThat(repo.entries.get(0).content()).isEqualTo("JOIN");
        assertThat(repo.entries.get(1).content()).isEqualTo("QUIT");
    }

    private static final class RecordingRepository implements LogBufferRepository {

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

        Instant cutoffFor(int hours) {
            return NOW.minus(hours, ChronoUnit.HOURS);
        }
    }
}
