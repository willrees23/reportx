package com.github.willrees23.reportx.paper.staff.commands;

import com.github.willrees23.solo.command.CommandContext;
import com.github.willrees23.solo.command.CommandInfo;
import com.github.willrees23.solo.command.SoloCommand;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

@CommandInfo(
        name = "reporthandle",
        permission = "reportx.staff.handle",
        description = "Open the case file for the report you are currently handling.",
        aliases = {"rh", "handle"},
        playerOnly = true)
public final class ReportHandleCommand extends SoloCommand {

    private final Consumer<Player> openCurrent;

    public ReportHandleCommand(Consumer<Player> openCurrent) {
        this.openCurrent = openCurrent;
    }

    @Override
    public void execute(CommandContext ctx) {
        openCurrent.accept(ctx.getPlayer());
    }
}
