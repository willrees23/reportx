package com.github.willrees23.reportx.core.config;

import java.util.List;

public record ReputationYaml(
        Decay decay,
        List<Tier> tiers
) {

    public record Decay(
            boolean enabled,
            int halfLifeDays
    ) {
    }

    public record Tier(
            String id,
            int minReports,
            int maxReports,
            String display,
            String description
    ) {
    }
}
