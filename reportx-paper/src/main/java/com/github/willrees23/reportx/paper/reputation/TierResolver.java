package com.github.willrees23.reportx.paper.reputation;

import com.github.willrees23.reportx.core.config.ReputationYaml;

import java.util.List;

public final class TierResolver {

    private TierResolver() {
    }

    public static ReputationYaml.Tier resolve(int countForTier, List<ReputationYaml.Tier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            throw new IllegalStateException("reputation.yml must define at least one tier");
        }
        int normalised = Math.max(0, countForTier);
        for (ReputationYaml.Tier tier : tiers) {
            if (matches(tier, normalised)) {
                return tier;
            }
        }
        // Fall back to the highest-bound tier (covers the "11+" / max-reports = -1 case).
        ReputationYaml.Tier highest = tiers.get(0);
        for (ReputationYaml.Tier tier : tiers) {
            if (tier.minReports() >= highest.minReports()) {
                highest = tier;
            }
        }
        return highest;
    }

    private static boolean matches(ReputationYaml.Tier tier, int count) {
        if (count < tier.minReports()) {
            return false;
        }
        return tier.maxReports() < 0 || count <= tier.maxReports();
    }
}
