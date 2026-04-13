package com.github.willrees23.reportx.paper.modules;

import com.github.willrees23.reportx.core.storage.AuditRepository;
import com.github.willrees23.reportx.core.storage.CaseRepository;
import com.github.willrees23.reportx.core.storage.EvidenceRepository;
import com.github.willrees23.reportx.core.storage.LogBufferRepository;
import com.github.willrees23.reportx.core.storage.NoteRepository;
import com.github.willrees23.reportx.core.storage.ReportRepository;
import com.github.willrees23.reportx.paper.storage.StorageBackendFactory;
import com.github.willrees23.reportx.storage.sqlite.SqliteStorage;
import com.github.willrees23.solo.module.Module;
import com.github.willrees23.solo.registry.ServiceRegistry;

import java.nio.file.Path;
import java.util.List;

public final class StorageModule extends Module {

    private SqliteStorage storage;

    @Override
    public String getName() {
        return "Storage";
    }

    @Override
    public List<String> getDependencies() {
        return List.of("Config");
    }

    @Override
    protected void onEnable() {
        ServiceRegistry registry = getContext().getServiceRegistry();
        ConfigModule config = registry.require(ConfigModule.class);

        Path dataDir = getContext().getPlugin().getDataFolder().toPath();
        this.storage = StorageBackendFactory.create(config.snapshot().storage(), dataDir);

        registry.register(ReportRepository.class, storage.reportRepository());
        registry.register(CaseRepository.class, storage.caseRepository());
        registry.register(EvidenceRepository.class, storage.evidenceRepository());
        registry.register(NoteRepository.class, storage.noteRepository());
        registry.register(AuditRepository.class, storage.auditRepository());
        registry.register(LogBufferRepository.class, storage.logBufferRepository());
        registry.register(StorageModule.class, this);
    }

    @Override
    protected void onDisable() {
        if (storage != null) {
            storage.close();
            storage = null;
        }
    }

    public SqliteStorage storage() {
        return storage;
    }
}
