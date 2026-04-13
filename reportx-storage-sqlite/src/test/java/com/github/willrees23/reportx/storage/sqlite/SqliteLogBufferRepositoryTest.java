package com.github.willrees23.reportx.storage.sqlite;

import com.github.willrees23.reportx.core.model.LogBufferEntry;
import com.github.willrees23.reportx.core.model.LogEntryType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteLogBufferRepositoryTest extends SqliteTestBase {

    @Test
    void insertAndQueryByTypeAndWindow() {
        SqliteLogBufferRepository repo = storage.logBufferRepository();
        UUID player = UUID.randomUUID();
        Instant base = Instant.parse("2026-04-13T12:00:00Z");

        LogBufferEntry recentChat = new LogBufferEntry(player, LogEntryType.CHAT, "hey", "survival", base);
        LogBufferEntry staleChat = new LogBufferEntry(player, LogEntryType.CHAT, "old", "survival", base.minusSeconds(7200));
        LogBufferEntry recentCmd = new LogBufferEntry(player, LogEntryType.COMMAND, "/tp", "survival", base);

        repo.insert(recentChat);
        repo.insert(staleChat);
        repo.insert(recentCmd);

        assertThat(repo.findByPlayerSince(player, base.minusSeconds(300)))
                .containsExactly(recentChat, recentCmd);
        assertThat(repo.findByPlayerSince(player, LogEntryType.CHAT, base.minusSeconds(300)))
                .containsExactly(recentChat);
    }

    @Test
    void pruneRemovesEntriesOlderThanCutoff() {
        SqliteLogBufferRepository repo = storage.logBufferRepository();
        UUID player = UUID.randomUUID();
        Instant base = Instant.parse("2026-04-13T12:00:00Z");

        repo.insert(new LogBufferEntry(player, LogEntryType.CHAT, "recent", "survival", base));
        repo.insert(new LogBufferEntry(player, LogEntryType.CHAT, "old", "survival", base.minusSeconds(7200)));

        int pruned = repo.pruneOlderThan(LogEntryType.CHAT, base.minusSeconds(3600));
        assertThat(pruned).isEqualTo(1);
        assertThat(repo.findByPlayerSince(player, base.minusSeconds(10000))).hasSize(1);
    }
}
