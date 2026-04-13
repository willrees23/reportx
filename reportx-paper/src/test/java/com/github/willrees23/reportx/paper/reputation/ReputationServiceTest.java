package com.github.willrees23.reportx.paper.reputation;

import com.github.willrees23.reportx.core.config.ReputationYaml;
import com.github.willrees23.reportx.core.model.Coords;
import com.github.willrees23.reportx.core.model.Report;
import com.github.willrees23.reportx.core.model.ReputationSnapshot;
import com.github.willrees23.reportx.core.storage.ReportRepository;
import com.github.willrees23.reportx.core.util.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ReputationServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-13T12:00:00Z");
    private static final ReputationYaml.Decay DECAY_ON = new ReputationYaml.Decay(true, 30);
    private static final ReputationYaml.Decay DECAY_OFF = new ReputationYaml.Decay(false, 30);
    private static final List<ReputationYaml.Tier> TIERS = List.of(
            new ReputationYaml.Tier("outstanding", 0, 0, "Outstanding", ""),
            new ReputationYaml.Tier("good", 1, 2, "Good", ""),
            new ReputationYaml.Tier("neutral", 3, 5, "Neutral", ""),
            new ReputationYaml.Tier("questionable", 6, 10, "Questionable", ""),
            new ReputationYaml.Tier("bad", 11, -1, "Bad", ""));

    private InMemoryReportRepo repo;
    private ReputationService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryReportRepo();
        service = new ReputationService(repo, Clock.fixed(NOW));
    }

    @Test
    void noReportsResolvesToOutstandingWithZeroes() {
        UUID target = UUID.randomUUID();

        ReputationSnapshot snapshot = service.compute(target, new ReputationYaml(DECAY_ON, TIERS));

        assertThat(snapshot.targetId()).isEqualTo(target);
        assertThat(snapshot.rawCount()).isZero();
        assertThat(snapshot.decayedScore()).isZero();
        assertThat(snapshot.tierId()).isEqualTo("outstanding");
    }

    @Test
    void allRecentReports_decayedRoughlyEqualsRaw() {
        UUID target = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            insertReport(target, NOW.minusSeconds(60L * i));
        }

        ReputationSnapshot snapshot = service.compute(target, new ReputationYaml(DECAY_ON, TIERS));

        assertThat(snapshot.rawCount()).isEqualTo(5);
        assertThat(snapshot.decayedScore()).isCloseTo(5.0, within(0.001));
        assertThat(snapshot.tierId()).isEqualTo("neutral");
    }

    @Test
    void oldReportsDecayBelowTier() {
        UUID target = UUID.randomUUID();
        // 12 ancient reports — without decay would be "bad", with decay should fall to outstanding.
        for (int i = 0; i < 12; i++) {
            insertReport(target, NOW.minus(365, ChronoUnit.DAYS));
        }

        ReputationSnapshot withDecay = service.compute(target, new ReputationYaml(DECAY_ON, TIERS));
        ReputationSnapshot noDecay = service.compute(target, new ReputationYaml(DECAY_OFF, TIERS));

        assertThat(withDecay.rawCount()).isEqualTo(12);
        assertThat(withDecay.decayedScore()).isLessThan(1.0);
        assertThat(withDecay.tierId()).isEqualTo("outstanding");
        assertThat(noDecay.tierId()).isEqualTo("bad");
    }

    @Test
    void oneReportAtHalfLife_contributesHalfPoint() {
        UUID target = UUID.randomUUID();
        insertReport(target, NOW.minus(30, ChronoUnit.DAYS));

        ReputationSnapshot snapshot = service.compute(target, new ReputationYaml(DECAY_ON, TIERS));

        assertThat(snapshot.rawCount()).isEqualTo(1);
        assertThat(snapshot.decayedScore()).isCloseTo(0.5, within(0.01));
        // Math.round(0.5) is 1 in Java, so the player still lands in "good" (1-2 reports).
        assertThat(snapshot.tierId()).isEqualTo("good");
    }

    @Test
    void oneReportAtTwoHalfLives_roundsBelowOneAndLandsInOutstanding() {
        UUID target = UUID.randomUUID();
        insertReport(target, NOW.minus(60, ChronoUnit.DAYS));

        ReputationSnapshot snapshot = service.compute(target, new ReputationYaml(DECAY_ON, TIERS));

        assertThat(snapshot.decayedScore()).isCloseTo(0.25, within(0.01));
        assertThat(snapshot.tierId()).isEqualTo("outstanding");
    }

    @Test
    void decayDisabled_pickedTierUsesRawCount() {
        UUID target = UUID.randomUUID();
        for (int i = 0; i < 7; i++) {
            insertReport(target, NOW.minus(180, ChronoUnit.DAYS));
        }

        ReputationSnapshot snapshot = service.compute(target, new ReputationYaml(DECAY_OFF, TIERS));

        assertThat(snapshot.rawCount()).isEqualTo(7);
        assertThat(snapshot.tierId()).isEqualTo("questionable");
    }

    private void insertReport(UUID target, Instant createdAt) {
        repo.insert(new Report(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), target,
                "chat", null, "survival",
                new Coords("world", 0, 64, 0), createdAt));
    }

    private static final class InMemoryReportRepo implements ReportRepository {

        final Map<UUID, Report> byId = new HashMap<>();

        @Override
        public void insert(Report report) {
            byId.put(report.id(), report);
        }

        @Override
        public Optional<Report> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Report> findByCase(UUID caseId) {
            return new ArrayList<>(byId.values()).stream()
                    .filter(r -> r.caseId().equals(caseId)).toList();
        }

        @Override
        public List<Report> findByTargetSince(UUID targetId, Instant since) {
            return byId.values().stream()
                    .filter(r -> r.targetId().equals(targetId) && !r.createdAt().isBefore(since))
                    .toList();
        }

        @Override
        public int countByTargetSince(UUID targetId, Instant since) {
            return findByTargetSince(targetId, since).size();
        }
    }
}
