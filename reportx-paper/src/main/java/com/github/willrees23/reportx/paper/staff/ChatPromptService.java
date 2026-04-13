package com.github.willrees23.reportx.paper.staff;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public final class ChatPromptService implements Listener {

    private static final String CANCEL_TOKEN = "cancel";

    private final Plugin plugin;
    private final ConcurrentMap<UUID, Pending> awaiting = new ConcurrentHashMap<>();

    public ChatPromptService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void prompt(Player player, Component message, Consumer<String> onResponse) {
        prompt(player, message, onResponse, () -> { });
    }

    public void prompt(Player player, Component message, Consumer<String> onResponse, Runnable onCancel) {
        player.sendMessage(message);
        awaiting.put(player.getUniqueId(), new Pending(onResponse, onCancel));
    }

    public void cancel(UUID playerId) {
        Pending pending = awaiting.remove(playerId);
        if (pending != null) {
            pending.onCancel.run();
        }
    }

    public boolean isAwaiting(UUID playerId) {
        return awaiting.containsKey(playerId);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Pending pending = awaiting.remove(id);
        if (pending == null) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase(CANCEL_TOKEN) || text.isEmpty()) {
                pending.onCancel.run();
            } else {
                pending.onResponse.accept(text);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Pending pending = awaiting.remove(event.getPlayer().getUniqueId());
        if (pending != null) {
            pending.onCancel.run();
        }
    }

    private record Pending(Consumer<String> onResponse, Runnable onCancel) {
    }
}
