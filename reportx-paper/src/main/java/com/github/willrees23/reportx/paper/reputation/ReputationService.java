package com.github.willrees23.reportx.paper.reputation;

import com.github.willrees23.reportx.core.config.ReputationYaml;
import com.github.willrees23.reportx.core.model.Report;
import com.github.willrees23.reportx.core.model.ReputationSnapshot;
import com.github.willrees23.reportx.core.storage.ReportRepository;
import com.github.willrees23.reportx.core.util.Clock;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReputationService {

    private static final Instant ALL_TIME = Instant.EPOCH;
    private static final double MILLIS_PER_DAY = 1000.0 * 60 * 60 * 24;

    private final ReportRepository reports;
    private final Clock clock;

    public ReputationService(ReportRepository reports, Clock clock) {
        this.reports = reports;
        this.clock = clock;
    }

    public ReputationSnapshot compute(UUID targetId, ReputationYaml config) {
        List<Report> all = reports.findByTargetSince(targetId, ALL_TIME);
        int rawCount = all.size();
        Instant now = clock.now();
        double decayedScore = computeDecayedScore(all, now, config.decay());
        int countForTier = config.decay().enabled()
                ? (int) Math.round(decayedScore)
                : rawCount;
        ReputationYaml.Tier tier = TierResolver.resolve(countForTier, config.tiers());
        return new ReputationSnapshot(targetId, rawCount, decayedScore, tier.id());
    }

    private static double computeDecayedScore(List<Report> reports, Instant now, ReputationYaml.Decay decay) {
        if (!decay.enabled() || decay.halfLifeDays() <= 0) {
            return reports.size();
        }
        double total = 0.0;
        double halfLifeDays = decay.halfLifeDays();
        for (Report report : reports) {
            double ageDays = (now.toEpochMilli() - report.createdAt().toEpochMilli()) / MILLIS_PER_DAY;
            if (ageDays < 0) {
                ageDays = 0;
            }
            total += Math.pow(0.5, ageDays / halfLifeDays);
        }
        return total;
    }
}
