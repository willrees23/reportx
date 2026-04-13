package com.github.willrees23.reportx.core.storage;

import com.github.willrees23.reportx.core.model.Evidence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceRepository {

    void insert(Evidence evidence);

    void update(Evidence evidence);

    void delete(UUID id);

    Optional<Evidence> findById(UUID id);

    List<Evidence> findByCase(UUID caseId);
}
