package com.github.willrees23.reportx.paper.cases;

import com.github.willrees23.reportx.core.config.ConfigYaml;
import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.core.messaging.events.CaseCreatedEvent;
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
