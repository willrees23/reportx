package com.github.willrees23.reportx.paper.staff;

import java.time.Instant;
import java.util.UUID;

public record StaffSession(UUID staffId, UUID caseId, Instant claimedAt) {
}
