package com.github.willrees23.reportx.core.storage;

import com.github.willrees23.reportx.core.model.AuditEntry;

import java.util.List;
import java.util.UUID;

public interface AuditRepository {

    void insert(AuditEntry entry);

    List<AuditEntry> findByCase(UUID caseId);
}
