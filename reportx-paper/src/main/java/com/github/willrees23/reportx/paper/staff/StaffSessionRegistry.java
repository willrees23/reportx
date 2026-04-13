package com.github.willrees23.reportx.paper.staff;

import com.github.willrees23.reportx.core.util.Clock;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class StaffSessionRegistry {

    private final Clock clock;
    private final ConcurrentMap<UUID, StaffSession> byStaff = new ConcurrentHashMap<>();

    public StaffSessionRegistry(Clock clock) {
        this.clock = clock;
    }

    public Optional<StaffSession> currentFor(UUID staffId) {
        return Optional.ofNullable(byStaff.get(staffId));
    }

    public boolean isHandling(UUID staffId) {
        return byStaff.containsKey(staffId);
    }

    public StaffSession start(UUID staffId, UUID caseId) {
        StaffSession session = new StaffSession(staffId, caseId, clock.now());
        byStaff.put(staffId, session);
        return session;
    }

    public Optional<StaffSession> end(UUID staffId) {
        return Optional.ofNullable(byStaff.remove(staffId));
    }

    public Optional<StaffSession> findByCase(UUID caseId) {
        return byStaff.values().stream()
                .filter(s -> s.caseId().equals(caseId))
                .findFirst();
    }

    public int size() {
        return byStaff.size();
    }
}
