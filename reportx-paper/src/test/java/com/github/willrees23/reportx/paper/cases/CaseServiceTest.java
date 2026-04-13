package com.github.willrees23.reportx.paper.cases;

import com.github.willrees23.reportx.core.config.ConfigYaml;
import com.github.willrees23.reportx.core.messaging.events.CaseCreatedEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportXEvent;
import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import com.github.willrees23.reportx.core.storage.CaseRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.reportx.messaging.local.LocalMessageBus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CaseServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-13T12:00:00Z");
    private static final ConfigYaml.DedupConfig DEDUP_DEFAULTS =
            new ConfigYaml.DedupConfig(true, 300, true, true);

    @Test
    void firstReport_createsNewUnclaimedCaseAndPublishesCaseCreated() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        LocalMessageBus bus = new LocalMessageBus();
        List<ReportXEvent> events = new ArrayList<>();
        bus.subscribe(ReportXEvent.class, events::add);

        CaseService service = new CaseService(repo, bus, Clock.fixed(NOW));
        UUID target = UUID.randomUUID();

        CaseService.CaseAssignment assignment = service.findOrCreateForReport(target, "chat", DEDUP_DEFAULTS);

        assertThat(assignment.newlyCreated()).isTrue();
        assertThat(assignment.value().targetId()).isEqualTo(target);
        assertThat(assignment.value().category()).isEqualTo("chat");
        assertThat(assignment.value().status()).isEqualTo(CaseStatus.UNCLAIMED);
        assertThat(assignment.value().createdAt()).isEqualTo(NOW);
        assertThat(repo.byId).containsKey(assignment.value().id());
        assertThat(events).singleElement().isInstanceOf(CaseCreatedEvent.class);
    }

    @Test
    void secondReportInWindowAndCategory_mergesIntoExistingCase() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        LocalMessageBus bus = new LocalMessageBus();
        CaseService service = new CaseService(repo, bus, Clock.fixed(NOW));
        UUID target = UUID.randomUUID();

        CaseService.CaseAssignment first = service.findOrCreateForReport(target, "chat", DEDUP_DEFAULTS);
        CaseService.CaseAssignment second = service.findOrCreateForReport(target, "chat", DEDUP_DEFAULTS);

        assertThat(second.newlyCreated()).isFalse();
        assertThat(second.value().id()).isEqualTo(first.value().id());
    }

    @Test
    void mergeAfterClaim_falseSkipsClaimedCandidate() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        LocalMessageBus bus = new LocalMessageBus();
        CaseService service = new CaseService(repo, bus, Clock.fixed(NOW));
        UUID target = UUID.randomUUID();

        CaseService.CaseAssignment first = service.findOrCreateForReport(target, "chat", DEDUP_DEFAULTS);
        // simulate the case being claimed
        Case claimed = new Case(first.value().id(), target, "chat", CaseStatus.CLAIMED,
                UUID.randomUUID(), NOW, null, null, null,
                first.value().createdAt(), NOW);
        repo.update(claimed);

        ConfigYaml.DedupConfig noMergeAfterClaim = new ConfigYaml.DedupConfig(true, 300, true, false);
        CaseService.CaseAssignment second = service.findOrCreateForReport(target, "chat", noMergeAfterClaim);

        assertThat(second.newlyCreated()).isTrue();
        assertThat(second.value().id()).isNotEqualTo(first.value().id());
    }

    @Test
    void differentCategory_createsNewCaseWhenSameCategoryRequired() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        LocalMessageBus bus = new LocalMessageBus();
        CaseService service = new CaseService(repo, bus, Clock.fixed(NOW));
        UUID target = UUID.randomUUID();

        service.findOrCreateForReport(target, "chat", DEDUP_DEFAULTS);
        CaseService.CaseAssignment second = service.findOrCreateForReport(target, "hacking", DEDUP_DEFAULTS);

        assertThat(second.newlyCreated()).isTrue();
    }

    @Test
    void differentCategory_mergesWhenSameCategoryNotRequired() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        LocalMessageBus bus = new LocalMessageBus();
        CaseService service = new CaseService(repo, bus, Clock.fixed(NOW));
        UUID target = UUID.randomUUID();

        ConfigYaml.DedupConfig anyCategory = new ConfigYaml.DedupConfig(true, 300, false, true);
        CaseService.CaseAssignment first = service.findOrCreateForReport(target, "chat", anyCategory);
        CaseService.CaseAssignment second = service.findOrCreateForReport(target, "hacking", anyCategory);

        assertThat(second.newlyCreated()).isFalse();
        assertThat(second.value().id()).isEqualTo(first.value().id());
    }

    @Test
    void claim_setsClaimerAndPublishesCaseClaimed() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        LocalMessageBus bus = new LocalMessageBus();
        List<ReportXEvent> events = new ArrayList<>();
        bus.subscribe(ReportXEvent.class, events::add);
        CaseService service = new CaseService(repo, bus, Clock.fixed(NOW));
        UUID target = UUID.randomUUID();

        CaseService.CaseAssignment assignment = service.findOrCreateForReport(target, "chat", DEDUP_DEFAULTS);
        events.clear();
        UUID staff = UUID.randomUUID();

        CaseLifecycleOutcome outcome = service.claim(assignment.value().id(), staff, "survival");

        assertThat(outcome).isInstanceOf(CaseLifecycleOutcome.Success.class);
        Case claimed = ((CaseLifecycleOutcome.Success) outcome).caseValue();
        assertThat(claimed.status()).isEqualTo(CaseStatus.CLAIMED);
        assertThat(claimed.claimedBy()).isEqualTo(staff);
        assertThat(claimed.claimedAt()).isEqualTo(NOW);
        assertThat(events).singleElement()
                .isInstanceOf(com.github.willrees23.reportx.core.messaging.events.CaseClaimedEvent.class);
    }

    @Test
    void claim_alreadyClaimedReportsCurrentClaimer() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        LocalMessageBus bus = new LocalMessageBus();
        CaseService service = new CaseService(repo, bus, Clock.fixed(NOW));
        UUID target = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        UUID caseId = service.findOrCreateForReport(target, "chat", DEDUP_DEFAULTS).value().id();
        service.claim(caseId, first, "survival");

        CaseLifecycleOutcome outcome = service.claim(caseId, second, "survival");

        assertThat(outcome).isInstanceOf(CaseLifecycleOutcome.AlreadyClaimed.class);
        assertThat(((CaseLifecycleOutcome.AlreadyClaimed) outcome).currentClaimer()).isEqualTo(first);
    }

    @Test
    void claim_unknownCaseReturnsNotFound() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        CaseService service = new CaseService(repo, new LocalMessageBus(), Clock.fixed(NOW));

        assertThat(service.claim(UUID.randomUUID(), UUID.randomUUID(), "s"))
                .isInstanceOf(CaseLifecycleOutcome.NotFound.class);
    }

    @Test
    void release_byClaimerReturnsCaseToUnclaimed() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        LocalMessageBus bus = new LocalMessageBus();
        CaseService service = new CaseService(repo, bus, Clock.fixed(NOW));
        UUID target = UUID.randomUUID();
        UUID staff = UUID.randomUUID();

        UUID caseId = service.findOrCreateForReport(target, "chat", DEDUP_DEFAULTS).value().id();
        service.claim(caseId, staff, "survival");

        CaseLifecycleOutcome outcome = service.release(caseId, staff);

        assertThat(outcome).isInstanceOf(CaseLifecycleOutcome.Success.class);
        Case released = ((CaseLifecycleOutcome.Success) outcome).caseValue();
        assertThat(released.status()).isEqualTo(CaseStatus.UNCLAIMED);
        assertThat(released.claimedBy()).isNull();
        assertThat(released.claimedAt()).isNull();
    }

    @Test
    void release_byNonClaimerIsBlocked() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        CaseService service = new CaseService(repo, new LocalMessageBus(), Clock.fixed(NOW));
        UUID staff = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        UUID caseId = service.findOrCreateForReport(UUID.randomUUID(), "chat", DEDUP_DEFAULTS).value().id();
        service.claim(caseId, staff, "survival");

        CaseLifecycleOutcome outcome = service.release(caseId, other);

        assertThat(outcome).isInstanceOf(CaseLifecycleOutcome.NotClaimer.class);
    }

    @Test
    void release_unclaimedCaseReturnsNotClaimed() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        CaseService service = new CaseService(repo, new LocalMessageBus(), Clock.fixed(NOW));
        UUID caseId = service.findOrCreateForReport(UUID.randomUUID(), "chat", DEDUP_DEFAULTS).value().id();

        assertThat(service.release(caseId, UUID.randomUUID()))
                .isInstanceOf(CaseLifecycleOutcome.NotClaimed.class);
    }

    @Test
    void handoff_movesClaimToNewStaff() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        LocalMessageBus bus = new LocalMessageBus();
        CaseService service = new CaseService(repo, bus, Clock.fixed(NOW));
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        UUID caseId = service.findOrCreateForReport(UUID.randomUUID(), "chat", DEDUP_DEFAULTS).value().id();
        service.claim(caseId, from, "survival");

        CaseLifecycleOutcome outcome = service.handoff(caseId, from, to);

        assertThat(outcome).isInstanceOf(CaseLifecycleOutcome.Success.class);
        Case handed = ((CaseLifecycleOutcome.Success) outcome).caseValue();
        assertThat(handed.status()).isEqualTo(CaseStatus.CLAIMED);
        assertThat(handed.claimedBy()).isEqualTo(to);
    }

    @Test
    void handoff_byNonClaimerIsBlocked() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        CaseService service = new CaseService(repo, new LocalMessageBus(), Clock.fixed(NOW));
        UUID staff = UUID.randomUUID();

        UUID caseId = service.findOrCreateForReport(UUID.randomUUID(), "chat", DEDUP_DEFAULTS).value().id();
        service.claim(caseId, staff, "survival");

        CaseLifecycleOutcome outcome = service.handoff(caseId, UUID.randomUUID(), UUID.randomUUID());

        assertThat(outcome).isInstanceOf(CaseLifecycleOutcome.NotClaimer.class);
    }

    @Test
    void resolve_acceptedSetsResolutionFieldsAndPublishesEvent() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        LocalMessageBus bus = new LocalMessageBus();
        List<ReportXEvent> events = new ArrayList<>();
        bus.subscribe(ReportXEvent.class, events::add);
        CaseService service = new CaseService(repo, bus, Clock.fixed(NOW));
        UUID staff = UUID.randomUUID();

        UUID caseId = service.findOrCreateForReport(UUID.randomUUID(), "chat", DEDUP_DEFAULTS).value().id();
        service.claim(caseId, staff, "survival");
        events.clear();

        CaseLifecycleOutcome outcome = service.resolve(caseId, staff, CaseStatus.RESOLVED_ACCEPTED, "confirmed");

        assertThat(outcome).isInstanceOf(CaseLifecycleOutcome.Success.class);
        Case resolved = ((CaseLifecycleOutcome.Success) outcome).caseValue();
        assertThat(resolved.status()).isEqualTo(CaseStatus.RESOLVED_ACCEPTED);
        assertThat(resolved.resolvedBy()).isEqualTo(staff);
        assertThat(resolved.resolutionReason()).isEqualTo("confirmed");
        assertThat(events).singleElement()
                .isInstanceOf(com.github.willrees23.reportx.core.messaging.events.CaseResolvedEvent.class);
    }

    @Test
    void resolve_invalidOutcomeRejected() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        CaseService service = new CaseService(repo, new LocalMessageBus(), Clock.fixed(NOW));
        UUID staff = UUID.randomUUID();
        UUID caseId = service.findOrCreateForReport(UUID.randomUUID(), "chat", DEDUP_DEFAULTS).value().id();
        service.claim(caseId, staff, "survival");

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.resolve(caseId, staff, CaseStatus.UNCLAIMED, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reopen_resolvedCaseGoesBackToUnclaimed() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        LocalMessageBus bus = new LocalMessageBus();
        List<ReportXEvent> events = new ArrayList<>();
        bus.subscribe(ReportXEvent.class, events::add);
        CaseService service = new CaseService(repo, bus, Clock.fixed(NOW));
        UUID staff = UUID.randomUUID();
        UUID admin = UUID.randomUUID();

        UUID caseId = service.findOrCreateForReport(UUID.randomUUID(), "chat", DEDUP_DEFAULTS).value().id();
        service.claim(caseId, staff, "survival");
        service.resolve(caseId, staff, CaseStatus.RESOLVED_DENIED, "wrong");
        events.clear();

        CaseLifecycleOutcome outcome = service.reopen(caseId, admin, "new evidence");

        assertThat(outcome).isInstanceOf(CaseLifecycleOutcome.Success.class);
        Case reopened = ((CaseLifecycleOutcome.Success) outcome).caseValue();
        assertThat(reopened.status()).isEqualTo(CaseStatus.UNCLAIMED);
        assertThat(reopened.claimedBy()).isNull();
        assertThat(reopened.resolvedBy()).isNull();
        assertThat(reopened.resolutionReason()).isNull();
        assertThat(events).singleElement()
                .isInstanceOf(com.github.willrees23.reportx.core.messaging.events.CaseReopenedEvent.class);
    }

    @Test
    void reopen_unresolvedCaseRejected() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        CaseService service = new CaseService(repo, new LocalMessageBus(), Clock.fixed(NOW));
        UUID caseId = service.findOrCreateForReport(UUID.randomUUID(), "chat", DEDUP_DEFAULTS).value().id();

        assertThat(service.reopen(caseId, UUID.randomUUID(), "x"))
                .isInstanceOf(CaseLifecycleOutcome.NotResolved.class);
    }

    @Test
    void claim_resolvedCaseRejected() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        CaseService service = new CaseService(repo, new LocalMessageBus(), Clock.fixed(NOW));
        UUID staff = UUID.randomUUID();
        UUID caseId = service.findOrCreateForReport(UUID.randomUUID(), "chat", DEDUP_DEFAULTS).value().id();
        service.claim(caseId, staff, "survival");
        service.resolve(caseId, staff, CaseStatus.RESOLVED_ACCEPTED, "ok");

        assertThat(service.claim(caseId, UUID.randomUUID(), "survival"))
                .isInstanceOf(CaseLifecycleOutcome.AlreadyResolved.class);
    }

    @Test
    void disabledDedup_alwaysCreatesNewCase() {
        InMemoryCaseRepo repo = new InMemoryCaseRepo();
        LocalMessageBus bus = new LocalMessageBus();
        CaseService service = new CaseService(repo, bus, Clock.fixed(NOW));
        UUID target = UUID.randomUUID();

        ConfigYaml.DedupConfig disabled = new ConfigYaml.DedupConfig(false, 300, true, true);
        CaseService.CaseAssignment first = service.findOrCreateForReport(target, "chat", disabled);
        CaseService.CaseAssignment second = service.findOrCreateForReport(target, "chat", disabled);

        assertThat(first.newlyCreated()).isTrue();
        assertThat(second.newlyCreated()).isTrue();
        assertThat(second.value().id()).isNotEqualTo(first.value().id());
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
                    .filter(this::isOpen)
                    .filter(c -> !c.createdAt().isBefore(notBefore))
                    .max((a, b) -> a.createdAt().compareTo(b.createdAt()));
        }

        @Override
        public Optional<Case> findOpenDedupCandidateAnyCategory(UUID targetId, Instant notBefore) {
            return byId.values().stream()
                    .filter(c -> c.targetId().equals(targetId))
                    .filter(this::isOpen)
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

        private boolean isOpen(Case c) {
            return c.status() == CaseStatus.UNCLAIMED || c.status() == CaseStatus.CLAIMED;
        }
    }
}
