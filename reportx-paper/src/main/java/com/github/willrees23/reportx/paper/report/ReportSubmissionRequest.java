package com.github.willrees23.reportx.paper.report;

import com.github.willrees23.reportx.core.model.Coords;

import java.util.UUID;

public record ReportSubmissionRequest(
        UUID reporterId,
        UUID targetId,
        String category,
        String detail,
        String serverName,
        Coords reporterCoords
) {
}
