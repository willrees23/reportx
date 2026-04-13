package com.github.willrees23.reportx.paper;

import com.github.willrees23.reportx.paper.modules.AuditModule;
import com.github.willrees23.reportx.paper.modules.CaseModule;
import com.github.willrees23.reportx.paper.modules.ConfigModule;
import com.github.willrees23.reportx.paper.modules.EvidenceModule;
import com.github.willrees23.reportx.paper.modules.LogBufferModule;
import com.github.willrees23.reportx.paper.modules.MessagingModule;
import com.github.willrees23.reportx.paper.modules.NoteModule;
import com.github.willrees23.reportx.paper.modules.ReportModule;
import com.github.willrees23.reportx.paper.modules.ReputationModule;
import com.github.willrees23.reportx.paper.modules.StorageModule;
import com.github.willrees23.solo.SoloPlugin;
import com.github.willrees23.solo.module.ModuleManager;

public final class ReportXPlugin extends SoloPlugin {

    @Override
    protected void registerModules(ModuleManager manager) {
        manager.register(new ConfigModule());
        manager.register(new StorageModule());
        manager.register(new MessagingModule());
        manager.register(new AuditModule());
        manager.register(new LogBufferModule());
        manager.register(new CaseModule());
        manager.register(new ReputationModule());
        manager.register(new EvidenceModule());
        manager.register(new NoteModule());
        manager.register(new ReportModule());
    }
}
