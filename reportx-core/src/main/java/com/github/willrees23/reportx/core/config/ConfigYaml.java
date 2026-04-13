package com.github.willrees23.reportx.core.config;

public record ConfigYaml(
        DedupConfig dedup,
        ReportsConfig reports,
        HandleConfig handle,
        GuiConfig gui,
        EvidenceConfig evidence,
        LogsConfig logs,
        ProxyConfig proxy
) {

    public record DedupConfig(
            boolean enabled,
            int windowSeconds,
            boolean sameCategoryRequired,
            boolean mergeAfterClaim
    ) {
    }

    public record ReportsConfig(
            int cooldownSeconds,
            int maxPerDay,
            int minAccountAgeHours,
            boolean requireReason
    ) {
    }

    public record HandleConfig(
            int emptyQueueRevertSeconds,
            int staleClaimReclaimMinutes
    ) {
    }

    public record GuiConfig(
            ClickSoundConfig clickSound
    ) {
    }

    public record ClickSoundConfig(
            boolean enabled,
            String sound,
            double volume,
            double pitch
    ) {
    }

    public record EvidenceConfig(
            boolean requireUrl
    ) {
    }

    public record LogsConfig(
            BufferConfig buffer,
            boolean persist,
            boolean rolling
    ) {
    }

    public record BufferConfig(
            int chatMaxMessages,
            int chatRetentionHours,
            int commandsMax,
            int commandsRetentionHours,
            int connectionsRetentionDays
    ) {
    }

    public record ProxyConfig(
            boolean enabled
    ) {
    }
}
