package com.github.willrees23.reportx.paper.report;

import com.github.willrees23.reportx.core.config.MessagesYaml;
import com.github.willrees23.reportx.core.config.ReportXConfig;
import com.github.willrees23.reportx.core.model.Coords;
import com.github.willrees23.reportx.paper.modules.ConfigModule;
import com.github.willrees23.reportx.paper.text.Text;
import com.github.willrees23.solo.command.CommandArgument;
import com.github.willrees23.solo.command.CommandContext;
import com.github.willrees23.solo.command.CommandInfo;
import com.github.willrees23.solo.command.SoloCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@CommandInfo(
        name = "report",
        permission = "reportx.report",
        description = "Report a player.",
        usage = "/report <player> [reason]",
        playerOnly = true)
public final class ReportCommand extends SoloCommand {

    private final ConfigModule configModule;
    private final BiConsumer<Player, ReportContext> onReport;

    public ReportCommand(ConfigModule configModule, BiConsumer<Player, ReportContext> onReport) {
        this.configModule = configModule;
        this.onReport = onReport;
    }

    @Override
    public List<CommandArgument> getArguments() {
        return List.of(
                CommandArgument.required("player").tabComplete(ctx -> onlinePlayerNames()));
    }

    @Override
    public void execute(CommandContext ctx) {
        Player player = ctx.getPlayer();
        ReportXConfig snapshot = configModule.snapshot();
        MessagesYaml messages = snapshot.messages();

        String[] args = ctx.getArgs();
        if (args.length == 0) {
            sendMessage(player, messages, "errors.usage", Map.of(), "<red>Usage: /report <player> [reason]");
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sendMessage(player, messages, "report.invalid-target",
                    Map.of("player", targetName),
                    "<red>That player could not be found.");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            sendMessage(player, messages, "report.self-report", Map.of(),
                    "<red>You cannot report yourself.");
            return;
        }

        String reason = args.length > 1 ? joinFrom(args, 1) : null;
        ReportContext context = new ReportContext(target, reason, locationCoords(player), serverName(player));
        onReport.accept(player, context);
    }

    private static List<String> onlinePlayerNames() {
        List<String> out = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            out.add(online.getName());
        }
        return out;
    }

    private static String joinFrom(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private static Coords locationCoords(Player player) {
        return new Coords(
                player.getWorld().getName(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ());
    }

    private static String serverName(Player player) {
        return player.getServer().getName();
    }

    private static void sendMessage(Player player, MessagesYaml messages, String key,
                                    Map<String, String> placeholders, String fallback) {
        String raw = messages == null ? fallback : messages.get(key).orElse(fallback);
        Component component = Text.parse(prefixed(messages, raw), placeholders);
        player.sendMessage(component);
    }

    private static String prefixed(MessagesYaml messages, String body) {
        if (messages == null) {
            return body;
        }
        String prefix = messages.prefix() == null ? "" : messages.prefix();
        return prefix + body;
    }

    public record ReportContext(Player target, String reason, Coords reporterCoords, String serverName) {
    }
}
