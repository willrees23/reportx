package com.github.willrees23.reportx.paper;

import com.github.willrees23.reportx.paper.modules.ConfigModule;
import com.github.willrees23.solo.SoloPlugin;
import com.github.willrees23.solo.module.ModuleManager;

public final class ReportXPlugin extends SoloPlugin {

    @Override
    protected void registerModules(ModuleManager manager) {
        manager.register(new ConfigModule());
    }
}
