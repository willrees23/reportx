package com.github.willrees23.reportx.core.model;

import java.util.UUID;

public record ReputationSnapshot(
        UUID targetId,
        int rawCount,
        double decayedScore,
        String tierId
) {
}
