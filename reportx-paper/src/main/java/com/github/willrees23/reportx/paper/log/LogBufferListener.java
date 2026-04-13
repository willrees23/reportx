package com.github.willrees23.reportx.paper.log;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class LogBufferListener implements Listener {

    private final LogBufferRecorder recorder;
    private final Plugin plugin;

    public LogBufferListener(LogBufferRecorder recorder, Plugin plugin) {
        this.recorder = recorder;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        UUID id = player.getUniqueId();
        // Already async — write directly.
        safeRecord(() -> recorder.recordChat(id, text));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        String message = event.getMessage();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> safeRecord(() -> recorder.recordCommand(id, message)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> safeRecord(() -> recorder.recordConnection(id, ConnectionType.JOIN)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> safeRecord(() -> recorder.recordConnection(id, ConnectionType.QUIT)));
    }

    private void safeRecord(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Failed to record log buffer entry: " + ex.getMessage());
        }
    }
}
