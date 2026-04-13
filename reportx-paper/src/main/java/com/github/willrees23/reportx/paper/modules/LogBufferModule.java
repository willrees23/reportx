package com.github.willrees23.reportx.paper.modules;

import com.github.willrees23.reportx.core.config.ConfigYaml;
import com.github.willrees23.reportx.core.storage.LogBufferRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.reportx.paper.log.LogBufferListener;
import com.github.willrees23.reportx.paper.log.LogBufferPruner;
import com.github.willrees23.reportx.paper.log.LogBufferRecorder;
import com.github.willrees23.solo.module.Module;
import com.github.willrees23.solo.registry.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public final class LogBufferModule extends Module {

    private static final long TICKS_PER_HOUR = 20L * 60L * 60L;
    // TODO: surface server identity via config when network mode lands; "default" suffices for single-server.
    private static final String DEFAULT_SERVER_NAME = "default";

    private LogBufferRecorder recorder;
    private LogBufferPruner pruner;
    private LogBufferListener listener;
    private BukkitTask pruneTask;
    private ConfigYaml.BufferConfig bufferConfig;

    @Override
    public String getName() {
        return "LogBuffer";
    }

    @Override
    public List<String> getDependencies() {
        return List.of("Config", "Storage");
    }

    @Override
    protected void onEnable() {
        ServiceRegistry registry = getContext().getServiceRegistry();
        LogBufferRepository repo = registry.require(LogBufferRepository.class);
        ConfigModule config = registry.require(ConfigModule.class);
        this.bufferConfig = config.snapshot().config().logs().buffer();

        Clock clock = Clock.system();
        this.recorder = new LogBufferRecorder(repo, clock, DEFAULT_SERVER_NAME);
        this.pruner = new LogBufferPruner(repo, clock);

        this.listener = new LogBufferListener(recorder, getContext().getPlugin());
        registerListener(listener);

        this.pruneTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                getContext().getPlugin(),
                this::runPrune,
                TICKS_PER_HOUR,
                TICKS_PER_HOUR);

        registry.register(LogBufferModule.class, this);
        registry.register(LogBufferRecorder.class, recorder);
    }

    @Override
    protected void onDisable() {
        if (pruneTask != null) {
            pruneTask.cancel();
            pruneTask = null;
        }
    }

    private void runPrune() {
        try {
            LogBufferPruner.PruneSummary summary = pruner.prune(bufferConfig);
            if (summary.total() > 0) {
                getLogger().fine("Pruned " + summary.total() + " stale log buffer entries.");
            }
        } catch (RuntimeException ex) {
            getLogger().warning("Log buffer prune failed: " + ex.getMessage());
        }
    }

    public LogBufferRecorder recorder() {
        return recorder;
    }
}
