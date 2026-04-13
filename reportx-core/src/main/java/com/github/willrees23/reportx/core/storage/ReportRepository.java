package com.github.willrees23.reportx.core.storage;

import com.github.willrees23.reportx.core.model.Report;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository {

    void insert(Report report);

    Optional<Report> findById(UUID id);

    List<Report> findByCase(UUID caseId);

    List<Report> findByTargetSince(UUID targetId, Instant since);

    int countByTargetSince(UUID targetId, Instant since);
}
