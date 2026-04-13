package com.github.willrees23.reportx.paper.staff;

import com.github.willrees23.reportx.core.util.Clock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StaffSessionRegistryTest {

    private static final Instant NOW = Instant.parse("2026-04-13T12:00:00Z");

    @Test
    void noSessionByDefault() {
        StaffSessionRegistry registry = new StaffSessionRegistry(Clock.fixed(NOW));

        assertThat(registry.isHandling(UUID.randomUUID())).isFalse();
        assertThat(registry.currentFor(UUID.randomUUID())).isEmpty();
    }

    @Test
    void start_recordsSessionWithClockTime() {
        StaffSessionRegistry registry = new StaffSessionRegistry(Clock.fixed(NOW));
        UUID staff = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        StaffSession session = registry.start(staff, caseId);

        assertThat(session.staffId()).isEqualTo(staff);
        assertThat(session.caseId()).isEqualTo(caseId);
        assertThat(session.claimedAt()).isEqualTo(NOW);
        assertThat(registry.isHandling(staff)).isTrue();
        assertThat(registry.currentFor(staff)).contains(session);
    }

    @Test
    void start_overwritesExistingSessionForSameStaff() {
        StaffSessionRegistry registry = new StaffSessionRegistry(Clock.fixed(NOW));
        UUID staff = UUID.randomUUID();

        registry.start(staff, UUID.randomUUID());
        UUID newCase = UUID.randomUUID();
        registry.start(staff, newCase);

        assertThat(registry.currentFor(staff)).map(StaffSession::caseId).contains(newCase);
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    void end_removesSessionAndReturnsIt() {
        StaffSessionRegistry registry = new StaffSessionRegistry(Clock.fixed(NOW));
        UUID staff = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        registry.start(staff, caseId);

        assertThat(registry.end(staff)).map(StaffSession::caseId).contains(caseId);
        assertThat(registry.isHandling(staff)).isFalse();
    }

    @Test
    void findByCase_returnsSessionWhenStaffHandlingMatchingCase() {
        StaffSessionRegistry registry = new StaffSessionRegistry(Clock.fixed(NOW));
        UUID staff = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        registry.start(staff, caseId);

        assertThat(registry.findByCase(caseId)).map(StaffSession::staffId).contains(staff);
        assertThat(registry.findByCase(UUID.randomUUID())).isEmpty();
    }
}
