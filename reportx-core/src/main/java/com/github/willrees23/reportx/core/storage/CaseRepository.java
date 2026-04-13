package com.github.willrees23.reportx.core.storage;

import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaseRepository {

    void insert(Case value);

    void update(Case value);

    Optional<Case> findById(UUID id);

    Optional<Case> findOpenDedupCandidate(UUID targetId, String category, Instant notBefore);

    Optional<Case> findOpenDedupCandidateAnyCategory(UUID targetId, Instant notBefore);

    List<Case> findByStatus(CaseStatus status);

    Optional<Case> findOldestUnclaimedByCategory(String category);

    List<Case> findClaimedBy(UUID staffId);
}
