package com.github.willrees23.reportx.paper.staff.commands;

import com.github.willrees23.solo.command.CommandArgument;
import com.github.willrees23.solo.command.CommandContext;
import com.github.willrees23.solo.command.CommandInfo;
import com.github.willrees23.solo.command.SoloCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@CommandInfo(
        name = "reporthandle",
        permission = "reportx.staff.handle",
        description = "Open the case file for the report you are currently handling, or pick a category.",
        aliases = {"rh", "handle"},
        playerOnly = true)
public final class ReportHandleCommand extends SoloCommand {

    public interface Handlers {

        void openDefault(Player viewer);

        void release(Player viewer);

        void handoff(Player viewer, String targetName);

        void reopen(Player viewer, String shortCaseId);

        void unknownSubcommand(Player viewer, String input);

        void usage(Player viewer, String key, String fallback);
    }

    private final Handlers handlers;

    public ReportHandleCommand(Handlers handlers) {
        this.handlers = handlers;
    }

    @Override
    public List<CommandArgument> getArguments() {
        return List.of(
                CommandArgument.optional("subcommand").tabComplete(ctx -> List.of("release", "handoff", "reopen")),
                CommandArgument.optional("argument").tabComplete(this::completeSubcommandArg));
    }

    @Override
    public void execute(CommandContext ctx) {
        Player viewer = ctx.getPlayer();
        String[] args = ctx.getArgs();
        if (args.length == 0) {
            handlers.openDefault(viewer);
            return;
        }
        switch (args[0].toLowerCase()) {
            case "release" -> handleRelease(viewer);
            case "handoff" -> handleHandoff(viewer, args);
            case "reopen" -> handleReopen(viewer, args);
            default -> handlers.unknownSubcommand(viewer, args[0]);
        }
    }

    private void handleRelease(Player viewer) {
        if (!viewer.hasPermission("reportx.staff.release")) {
            handlers.usage(viewer, "errors.no-permission", "<red>You don't have permission for that.");
            return;
        }
        handlers.release(viewer);
    }

    private void handleHandoff(Player viewer, String[] args) {
        if (!viewer.hasPermission("reportx.staff.handoff")) {
            handlers.usage(viewer, "errors.no-permission", "<red>You don't have permission for that.");
            return;
        }
        if (args.length < 2) {
            handlers.usage(viewer, "staff.handoff-usage", "<red>Usage: /rh handoff <staff>");
            return;
        }
        handlers.handoff(viewer, args[1]);
    }

    private void handleReopen(Player viewer, String[] args) {
        if (!viewer.hasPermission("reportx.admin.reopen")) {
            handlers.usage(viewer, "errors.no-permission", "<red>You don't have permission for that.");
            return;
        }
        if (args.length < 2) {
            handlers.usage(viewer, "staff.reopen-usage", "<red>Usage: /rh reopen <case-id>");
            return;
        }
        handlers.reopen(viewer, args[1]);
    }

    private List<String> completeSubcommandArg(CommandContext ctx) {
        String[] args = ctx.getArgs();
        if (args.length < 1) {
            return List.of();
        }
        if ("handoff".equalsIgnoreCase(args[0])) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return names;
        }
        return List.of();
    }
}
