package com.github.willrees23.reportx.paper.staff.commands;

import com.github.willrees23.solo.command.CommandContext;
import com.github.willrees23.solo.command.CommandInfo;
import com.github.willrees23.solo.command.SoloCommand;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

@CommandInfo(
        name = "claimedreports",
        permission = "reportx.staff.claimedreports",
        description = "Open the claimed reports queue (admin view).",
        playerOnly = true)
public final class ClaimedReportsCommand extends SoloCommand {

    private final Consumer<Player> openQueue;

    public ClaimedReportsCommand(Consumer<Player> openQueue) {
        this.openQueue = openQueue;
    }

    @Override
    public void execute(CommandContext ctx) {
        openQueue.accept(ctx.getPlayer());
    }
}
