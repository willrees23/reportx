package com.github.willrees23.reportx.paper.staff.commands;

import com.github.willrees23.solo.command.CommandContext;
import com.github.willrees23.solo.command.CommandInfo;
import com.github.willrees23.solo.command.SoloCommand;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

@CommandInfo(
        name = "reports",
        permission = "reportx.staff.reports",
        description = "Open the unclaimed reports queue.",
        aliases = {"unclaimedreports"},
        playerOnly = true)
public final class ReportsCommand extends SoloCommand {

    private final Consumer<Player> openQueue;

    public ReportsCommand(Consumer<Player> openQueue) {
        this.openQueue = openQueue;
    }

    @Override
    public void execute(CommandContext ctx) {
        openQueue.accept(ctx.getPlayer());
    }
}
