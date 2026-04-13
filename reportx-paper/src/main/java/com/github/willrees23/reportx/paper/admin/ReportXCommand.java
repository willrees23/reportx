package com.github.willrees23.reportx.paper.admin;

import com.github.willrees23.reportx.core.config.MessagesYaml;
import com.github.willrees23.reportx.paper.modules.ConfigModule;
import com.github.willrees23.reportx.paper.text.Text;
import com.github.willrees23.solo.command.CommandArgument;
import com.github.willrees23.solo.command.CommandContext;
import com.github.willrees23.solo.command.CommandInfo;
import com.github.willrees23.solo.command.SoloCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandInfo(
        name = "reportx",
        description = "ReportX info and admin commands.",
        usage = "/reportx [reload]")
public final class ReportXCommand extends SoloCommand {

    private static final String RELOAD_PERMISSION = "reportx.admin.reload";

    private final Plugin plugin;
    private final ConfigModule configModule;

    public ReportXCommand(Plugin plugin, ConfigModule configModule) {
        this.plugin = plugin;
        this.configModule = configModule;
    }

    @Override
    public List<CommandArgument> getArguments() {
        return List.of(
                CommandArgument.optional("subcommand").tabComplete(ctx -> List.of("reload")));
    }

    @Override
    public void execute(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        String[] args = ctx.getArgs();

        if (args.length == 0) {
            sendAbout(sender);
            return;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            handleReload(sender);
            return;
        }

        send(sender, "reportx.unknown-subcommand",
                Map.of("input", args[0]),
                "<red>Unknown subcommand: <white>{input}</white>. Try /reportx [reload].");
    }

    private void sendAbout(CommandSender sender) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", plugin.getName());
        placeholders.put("version", plugin.getPluginMeta().getVersion());
        placeholders.put("author", String.join(", ", plugin.getPluginMeta().getAuthors()));
        placeholders.put("description", nullToEmpty(plugin.getPluginMeta().getDescription()));
        placeholders.put("website", nullToEmpty(plugin.getPluginMeta().getWebsite()));

        sendRaw(sender, "<dark_gray><strikethrough>--------------------</strikethrough>");
        send(sender, "reportx.about-name",
                placeholders,
                "<aqua><bold>{name}</bold> <gray>v<white>{version}</white>");
        send(sender, "reportx.about-author",
                placeholders,
                "<gray>by <white>{author}");
        send(sender, "reportx.about-description",
                placeholders,
                "<gray>{description}");
        send(sender, "reportx.about-website",
                placeholders,
                "<gray>{website}");
        send(sender, "reportx.about-hint",
                Map.of(),
                "<gray>Run <white>/reportx reload</white> to re-read configs.");
        sendRaw(sender, "<dark_gray><strikethrough>--------------------</strikethrough>");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            send(sender, "errors.no-permission", Map.of(),
                    "<red>You don't have permission for that.");
            return;
        }
        long started = System.currentTimeMillis();
        try {
            configModule.reload();
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Failed to reload ReportX configuration: " + ex.getMessage());
            send(sender, "reportx.reload-failed",
                    Map.of("error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()),
                    "<red>Reload failed: <white>{error}</white>. Check the server log.");
            return;
        }
        long elapsed = System.currentTimeMillis() - started;
        send(sender, "reportx.reload-ok",
                Map.of("ms", String.valueOf(elapsed)),
                "<green>ReportX configuration reloaded in <white>{ms}ms</white>.");
    }

    private void send(CommandSender sender, String key, Map<String, String> placeholders, String fallback) {
        MessagesYaml messages = configModule.snapshot().messages();
        String raw = messages == null ? fallback : messages.get(key).orElse(fallback);
        String prefix = messages == null ? "" : (messages.prefix() == null ? "" : messages.prefix());
        Component component = Text.parse(prefix + raw, placeholders);
        sender.sendMessage(component);
    }

    private void sendRaw(CommandSender sender, String mini) {
        sender.sendMessage(Text.parse(mini));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
