package com.github.willrees23.reportx.paper.cases;

import com.github.willrees23.reportx.core.config.ConfigYaml;
import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.core.messaging.events.CaseClaimedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseCreatedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseHandedOffEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseReleasedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseReopenedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseResolvedEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportMergedIntoCaseEvent;
import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import com.github.willrees23.reportx.core.storage.CaseRepository;
import com.github.willrees23.reportx.core.util.Clock;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class CaseService {

    private final CaseRepository repo;
    private final MessageBus bus;
    private final Clock clock;

    public CaseService(CaseRepository repo, MessageBus bus, Clock clock) {
        this.repo = repo;
        this.bus = bus;
        this.clock = clock;
    }

    public CaseAssignment findOrCreateForReport(UUID targetId, String category, ConfigYaml.DedupConfig dedup) {
        Instant now = clock.now();
        if (dedup.enabled()) {
            Optional<Case> candidate = lookupCandidate(targetId, category, dedup, now);
            if (candidate.isPresent()) {
                Case found = candidate.get();
                if (canMerge(found, dedup)) {
                    Case touched = touch(found, now);
                    repo.update(touched);
                    return new CaseAssignment(touched, false);
                }
            }
        }
        Case fresh = new Case(
                UUID.randomUUID(),
                targetId,
                category,
                CaseStatus.UNCLAIMED,
                null, null, null, null, null,
                now, now);
        repo.insert(fresh);
        bus.publish(new CaseCreatedEvent(fresh));
        return new CaseAssignment(fresh, true);
    }

    public void publishMerge(UUID reportId, UUID caseId) {
        bus.publish(new ReportMergedIntoCaseEvent(reportId, caseId));
    }

    public CaseLifecycleOutcome claim(UUID caseId, UUID staffId, String serverName) {
        Optional<Case> existing = repo.findById(caseId);
        if (existing.isEmpty()) {
            return new CaseLifecycleOutcome.NotFound();
        }
        Case current = existing.get();
        if (isResolved(current)) {
            return new CaseLifecycleOutcome.AlreadyResolved();
        }
        if (current.status() == CaseStatus.CLAIMED) {
            return new CaseLifecycleOutcome.AlreadyClaimed(current.claimedBy());
        }
        Instant now = clock.now();
        Case claimed = new Case(
                current.id(), current.targetId(), current.category(),
                CaseStatus.CLAIMED, staffId, now,
                null, null, null,
                current.createdAt(), now);
        repo.update(claimed);
        bus.publish(new CaseClaimedEvent(claimed.id(), staffId, serverName));
        return new CaseLifecycleOutcome.Success(claimed);
    }

    public CaseLifecycleOutcome release(UUID caseId, UUID staffId) {
        Optional<Case> existing = repo.findById(caseId);
        if (existing.isEmpty()) {
            return new CaseLifecycleOutcome.NotFound();
        }
        Case current = existing.get();
        if (current.status() != CaseStatus.CLAIMED) {
            return new CaseLifecycleOutcome.NotClaimed();
        }
        if (!staffId.equals(current.claimedBy())) {
            return new CaseLifecycleOutcome.NotClaimer(current.claimedBy());
        }
        Instant now = clock.now();
        Case released = new Case(
                current.id(), current.targetId(), current.category(),
                CaseStatus.UNCLAIMED, null, null,
                null, null, null,
                current.createdAt(), now);
        repo.update(released);
        bus.publish(new CaseReleasedEvent(released.id(), staffId));
        return new CaseLifecycleOutcome.Success(released);
    }

    public CaseLifecycleOutcome handoff(UUID caseId, UUID fromStaffId, UUID toStaffId) {
        Optional<Case> existing = repo.findById(caseId);
        if (existing.isEmpty()) {
            return new CaseLifecycleOutcome.NotFound();
        }
        Case current = existing.get();
        if (current.status() != CaseStatus.CLAIMED) {
            return new CaseLifecycleOutcome.NotClaimed();
        }
        if (!fromStaffId.equals(current.claimedBy())) {
            return new CaseLifecycleOutcome.NotClaimer(current.claimedBy());
        }
        Instant now = clock.now();
        Case handed = new Case(
                current.id(), current.targetId(), current.category(),
                CaseStatus.CLAIMED, toStaffId, now,
                null, null, null,
                current.createdAt(), now);
        repo.update(handed);
        bus.publish(new CaseHandedOffEvent(handed.id(), fromStaffId, toStaffId));
        return new CaseLifecycleOutcome.Success(handed);
    }

    public CaseLifecycleOutcome resolve(UUID caseId, UUID staffId, CaseStatus outcome, String reason) {
        if (outcome != CaseStatus.RESOLVED_ACCEPTED && outcome != CaseStatus.RESOLVED_DENIED) {
            throw new IllegalArgumentException("resolve outcome must be RESOLVED_ACCEPTED or RESOLVED_DENIED, got " + outcome);
        }
        Optional<Case> existing = repo.findById(caseId);
        if (existing.isEmpty()) {
            return new CaseLifecycleOutcome.NotFound();
        }
        Case current = existing.get();
        if (current.status() != CaseStatus.CLAIMED) {
            return new CaseLifecycleOutcome.NotClaimed();
        }
        if (!staffId.equals(current.claimedBy())) {
            return new CaseLifecycleOutcome.NotClaimer(current.claimedBy());
        }
        Instant now = clock.now();
        Case resolved = new Case(
                current.id(), current.targetId(), current.category(),
                outcome, current.claimedBy(), current.claimedAt(),
                staffId, now, reason,
                current.createdAt(), now);
        repo.update(resolved);
        bus.publish(new CaseResolvedEvent(resolved.id(), outcome, staffId, reason));
        return new CaseLifecycleOutcome.Success(resolved);
    }

    public CaseLifecycleOutcome reopen(UUID caseId, UUID staffId, String reason) {
        Optional<Case> existing = repo.findById(caseId);
        if (existing.isEmpty()) {
            return new CaseLifecycleOutcome.NotFound();
        }
        Case current = existing.get();
        if (!isResolved(current)) {
            return new CaseLifecycleOutcome.NotResolved();
        }
        Instant now = clock.now();
        Case reopened = new Case(
                current.id(), current.targetId(), current.category(),
                CaseStatus.UNCLAIMED, null, null,
                null, null, null,
                current.createdAt(), now);
        repo.update(reopened);
        bus.publish(new CaseReopenedEvent(reopened.id(), staffId, reason));
        return new CaseLifecycleOutcome.Success(reopened);
    }

    private static boolean isResolved(Case c) {
        return c.status() == CaseStatus.RESOLVED_ACCEPTED || c.status() == CaseStatus.RESOLVED_DENIED;
    }

    private Optional<Case> lookupCandidate(UUID targetId, String category, ConfigYaml.DedupConfig dedup, Instant now) {
        Instant windowStart = now.minusSeconds(dedup.windowSeconds());
        return dedup.sameCategoryRequired()
                ? repo.findOpenDedupCandidate(targetId, category, windowStart)
                : repo.findOpenDedupCandidateAnyCategory(targetId, windowStart);
    }

    private boolean canMerge(Case found, ConfigYaml.DedupConfig dedup) {
        if (found.status() == CaseStatus.CLAIMED && !dedup.mergeAfterClaim()) {
            return false;
        }
        return found.status() == CaseStatus.UNCLAIMED || found.status() == CaseStatus.CLAIMED;
    }

    private Case touch(Case existing, Instant now) {
        return new Case(
                existing.id(),
                existing.targetId(),
                existing.category(),
                existing.status(),
                existing.claimedBy(),
                existing.claimedAt(),
                existing.resolvedBy(),
                existing.resolvedAt(),
                existing.resolutionReason(),
                existing.createdAt(),
                now);
    }

    public record CaseAssignment(Case value, boolean newlyCreated) {
    }
}
