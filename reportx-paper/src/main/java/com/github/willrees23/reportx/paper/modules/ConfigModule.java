package com.github.willrees23.reportx.paper.modules;

import com.github.willrees23.reportx.core.config.ReportXConfig;
import com.github.willrees23.reportx.paper.admin.ReportXCommand;
import com.github.willrees23.reportx.paper.config.ConfigLoader;
import com.github.willrees23.solo.module.Module;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public final class ConfigModule extends Module {

    private final AtomicReference<ReportXConfig> current = new AtomicReference<>();
    private ConfigLoader loader;

    @Override
    public String getName() {
        return "Config";
    }

    @Override
    protected void onLoad() {
        Path dataDir = getContext().getPlugin().getDataFolder().toPath();
        this.loader = new ConfigLoader(dataDir, getClass().getClassLoader());
        reload();
    }

    @Override
    protected void onEnable() {
        getContext().getServiceRegistry().register(ConfigModule.class, this);
        registerCommand(new ReportXCommand(getContext().getPlugin(), this));
    }

    public ReportXConfig snapshot() {
        ReportXConfig value = current.get();
        if (value == null) {
            throw new IllegalStateException("ReportX config not loaded yet");
        }
        return value;
    }

    public void reload() {
        current.set(loader.load());
    }
}
