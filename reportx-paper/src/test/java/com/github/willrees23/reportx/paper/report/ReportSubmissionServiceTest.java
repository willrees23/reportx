package com.github.willrees23.reportx.paper.report;

import com.github.willrees23.reportx.core.config.CategoriesYaml;
import com.github.willrees23.reportx.core.config.ConfigYaml;
import com.github.willrees23.reportx.core.messaging.events.CaseCreatedEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportCreatedEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportMergedIntoCaseEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportXEvent;
import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import com.github.willrees23.reportx.core.model.Coords;
import com.github.willrees23.reportx.core.model.Report;
import com.github.willrees23.reportx.core.storage.CaseRepository;
import com.github.willrees23.reportx.core.storage.ReportRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.reportx.messaging.local.LocalMessageBus;
import com.github.willrees23.reportx.paper.cases.CaseService;
import com.github.willrees23.solo.registry.CooldownRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportSubmissionServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-13T12:00:00Z");
    private static final ConfigYaml CONFIG = new ConfigYaml(
            new ConfigYaml.DedupConfig(true, 300, true, true),
            new ConfigYaml.ReportsConfig(60, 20, 0, false, false, false),
            new ConfigYaml.HandleConfig(2, 15),
            new ConfigYaml.GuiConfig(new ConfigYaml.ClickSoundConfig(true, "UI_BUTTON_CLICK", 1.0, 1.0)),
            new ConfigYaml.EvidenceConfig(false),
            new ConfigYaml.LogsConfig(new ConfigYaml.BufferConfig(200, 24, 100, 24, 30), true, true),
            new ConfigYaml.ProxyConfig(false));
    private static final CategoriesYaml CATEGORIES = new CategoriesYaml(
            List.of(
                    new CategoriesYaml.Category("hacking", "Hacking", "IRON_SWORD", List.of(), 100, 60),
                    new CategoriesYaml.Category("chat", "Chat", "PAPER", List.of(), 50, 30),
                    new CategoriesYaml.Category("other", "Other", "BOOK", List.of(), 25, 30)),
            new CategoriesYaml.EmptySlot("IRON_BARS", "None", List.of(), "BLOCK_NOTE_BLOCK_BASS"));

    private InMemoryReportRepo reports;
    private InMemoryCaseRepo casesRepo;
    private LocalMessageBus bus;
    private CooldownRegistry cooldowns;
    private List<ReportXEvent> received;
    private ReportSubmissionService service;

    @BeforeEach
    void setUp() {
        reports = new InMemoryReportRepo();
        casesRepo = new InMemoryCaseRepo();
        bus = new LocalMessageBus();
        cooldowns = new CooldownRegistry();
        received = new ArrayList<>();
        bus.subscribe(ReportXEvent.class, received::add);

        CaseService caseService = new CaseService(casesRepo, bus, Clock.fixed(NOW));
        service = new ReportSubmissionService(reports, caseService, bus, cooldowns, Clock.fixed(NOW));
    }

    @Test
    void firstReport_writesReportPublishesEventsAndAppliesCooldown() {
        UUID reporter = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        ReportSubmissionRequest request = request(reporter, target, "chat", "spamming");

        ReportSubmissionResult result = service.submit(request, CONFIG, CATEGORIES);

        assertThat(result).isInstanceOf(ReportSubmissionResult.Success.class);
        ReportSubmissionResult.Success success = (ReportSubmissionResult.Success) result;
        assertThat(success.mergedIntoExistingCase()).isFalse();
        assertThat(reports.byId).containsKey(success.report().id());
        assertThat(received).hasSize(2);
        assertThat(received.get(0)).isInstanceOf(CaseCreatedEvent.class);
        assertThat(received.get(1)).isInstanceOf(ReportCreatedEvent.class);
        assertThat(cooldowns.isOnCooldown(ReportSubmissionService.COOLDOWN_KEY, reporter)).isTrue();
    }

    @Test
    void secondReportInWindow_mergesAndPublishesMergeEvent() {
        UUID firstReporter = UUID.randomUUID();
        UUID secondReporter = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        service.submit(request(firstReporter, target, "chat", null), CONFIG, CATEGORIES);
        received.clear();

        ReportSubmissionResult result = service.submit(request(secondReporter, target, "chat", null), CONFIG, CATEGORIES);

        assertThat(result).isInstanceOf(ReportSubmissionResult.Success.class);
        ReportSubmissionResult.Success success = (ReportSubmissionResult.Success) result;
        assertThat(success.mergedIntoExistingCase()).isTrue();
        assertThat(received).hasSize(2);
        assertThat(received.get(0)).isInstanceOf(ReportCreatedEvent.class);
        assertThat(received.get(1)).isInstanceOf(ReportMergedIntoCaseEvent.class);
    }

    @Test
    void selfReportIsRejectedWithoutSideEffects() {
        UUID reporter = UUID.randomUUID();
        ReportSubmissionRequest request = request(reporter, reporter, "chat", null);

        ReportSubmissionResult result = service.submit(request, CONFIG, CATEGORIES);

        assertThat(result).isInstanceOf(ReportSubmissionResult.SelfReport.class);
        assertThat(reports.byId).isEmpty();
        assertThat(received).isEmpty();
        assertThat(cooldowns.isOnCooldown(ReportSubmissionService.COOLDOWN_KEY, reporter)).isFalse();
    }

    @Test
    void selfReport_isAcceptedWhenAllowSelfReportsTrue() {
        ConfigYaml allowingSelf = new ConfigYaml(
                CONFIG.dedup(),
                new ConfigYaml.ReportsConfig(60, 20, 0, false, false, true),
                CONFIG.handle(), CONFIG.gui(), CONFIG.evidence(), CONFIG.logs(), CONFIG.proxy());
        UUID reporter = UUID.randomUUID();

        ReportSubmissionResult result = service.submit(
                request(reporter, reporter, "chat", null), allowingSelf, CATEGORIES);

        assertThat(result).isInstanceOf(ReportSubmissionResult.Success.class);
        assertThat(reports.byId).hasSize(1);
    }

    @Test
    void unknownCategoryIsRejected() {
        ReportSubmissionRequest request = request(UUID.randomUUID(), UUID.randomUUID(), "imaginary", null);

        ReportSubmissionResult result = service.submit(request, CONFIG, CATEGORIES);

        assertThat(result).isInstanceOf(ReportSubmissionResult.UnknownCategory.class);
        assertThat(reports.byId).isEmpty();
    }

    @Test
    void cooldownBlocksRepeatReportFromSameReporter() {
        UUID reporter = UUID.randomUUID();
        cooldowns.setCooldown(ReportSubmissionService.COOLDOWN_KEY, reporter, 60_000);

        ReportSubmissionResult result = service.submit(
                request(reporter, UUID.randomUUID(), "chat", null), CONFIG, CATEGORIES);

        assertThat(result).isInstanceOf(ReportSubmissionResult.OnCooldown.class);
        ReportSubmissionResult.OnCooldown onCooldown = (ReportSubmissionResult.OnCooldown) result;
        assertThat(onCooldown.remainingSeconds()).isGreaterThan(0);
        assertThat(reports.byId).isEmpty();
    }

    @Test
    void requireReason_rejectsBlankDetail() {
        ConfigYaml requiringReason = new ConfigYaml(
                CONFIG.dedup(),
                new ConfigYaml.ReportsConfig(60, 20, 0, true, false, false),
                CONFIG.handle(), CONFIG.gui(), CONFIG.evidence(), CONFIG.logs(), CONFIG.proxy());

        ReportSubmissionResult result = service.submit(
                request(UUID.randomUUID(), UUID.randomUUID(), "chat", "   "),
                requiringReason, CATEGORIES);

        assertThat(result).isInstanceOf(ReportSubmissionResult.ReasonRequired.class);
    }

    private ReportSubmissionRequest request(UUID reporter, UUID target, String category, String detail) {
        return new ReportSubmissionRequest(
                reporter, target, category, detail,
                "survival",
                new Coords("world", 0, 64, 0));
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
            return byId.values().stream().filter(r -> r.caseId().equals(caseId)).toList();
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

    private static final class InMemoryCaseRepo implements CaseRepository {

        final Map<UUID, Case> byId = new HashMap<>();

        @Override
        public void insert(Case value) {
            byId.put(value.id(), value);
        }

        @Override
        public void update(Case value) {
            byId.put(value.id(), value);
        }

        @Override
        public Optional<Case> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Case> findOpenDedupCandidate(UUID targetId, String category, Instant notBefore) {
            return byId.values().stream()
                    .filter(c -> c.targetId().equals(targetId))
                    .filter(c -> category.equals(c.category()))
                    .filter(c -> c.status() == CaseStatus.UNCLAIMED || c.status() == CaseStatus.CLAIMED)
                    .filter(c -> !c.createdAt().isBefore(notBefore))
                    .max((a, b) -> a.createdAt().compareTo(b.createdAt()));
        }

        @Override
        public Optional<Case> findOpenDedupCandidateAnyCategory(UUID targetId, Instant notBefore) {
            return byId.values().stream()
                    .filter(c -> c.targetId().equals(targetId))
                    .filter(c -> c.status() == CaseStatus.UNCLAIMED || c.status() == CaseStatus.CLAIMED)
                    .filter(c -> !c.createdAt().isBefore(notBefore))
                    .max((a, b) -> a.createdAt().compareTo(b.createdAt()));
        }

        @Override
        public List<Case> findByStatus(CaseStatus status) {
            return byId.values().stream().filter(c -> c.status() == status).toList();
        }

        @Override
        public Optional<Case> findOldestUnclaimedByCategory(String category) {
            return byId.values().stream()
                    .filter(c -> c.status() == CaseStatus.UNCLAIMED)
                    .filter(c -> category.equals(c.category()))
                    .min((a, b) -> a.createdAt().compareTo(b.createdAt()));
        }

        @Override
        public List<Case> findClaimedBy(UUID staffId) {
            return byId.values().stream()
                    .filter(c -> c.status() == CaseStatus.CLAIMED && staffId.equals(c.claimedBy()))
                    .toList();
        }
    }
}
