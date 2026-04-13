package com.github.willrees23.reportx.paper.modules;

import com.github.willrees23.reportx.core.config.MessagesYaml;
import com.github.willrees23.reportx.core.config.ReportXConfig;
import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.core.storage.ReportRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.reportx.paper.cases.CaseService;
import com.github.willrees23.reportx.paper.report.CategoryPickerGuiFactory;
import com.github.willrees23.reportx.paper.report.ReportCommand;
import com.github.willrees23.reportx.paper.report.ReportSubmissionRequest;
import com.github.willrees23.reportx.paper.report.ReportSubmissionResult;
import com.github.willrees23.reportx.paper.report.ReportSubmissionService;
import com.github.willrees23.reportx.paper.text.Text;
import com.github.willrees23.solo.gui.Gui;
import com.github.willrees23.solo.module.Module;
import com.github.willrees23.solo.registry.CooldownRegistry;
import com.github.willrees23.solo.registry.ServiceRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReportModule extends Module {

    private static final String PREFIX_KEY_FALLBACK = "";

    private ReportSubmissionService submissionService;
    private ConfigModule configModule;
    private CooldownRegistry cooldowns;

    @Override
    public String getName() {
        return "Report";
    }

    @Override
    public List<String> getDependencies() {
        return List.of("Config", "Storage", "Messaging", "Case");
    }

    @Override
    protected void onEnable() {
        ServiceRegistry registry = getContext().getServiceRegistry();
        ReportRepository reports = registry.require(ReportRepository.class);
        MessageBus bus = registry.require(MessageBus.class);
        CaseService cases = registry.require(CaseService.class);
        this.configModule = registry.require(ConfigModule.class);

        this.cooldowns = registry.get(CooldownRegistry.class).orElseGet(() -> {
            CooldownRegistry created = new CooldownRegistry();
            registry.register(CooldownRegistry.class, created);
            return created;
        });

        this.submissionService = new ReportSubmissionService(
                reports, cases, bus, cooldowns, Clock.system());

        registerCommand(new ReportCommand(configModule, this::openCategoryPicker));
        registry.register(ReportSubmissionService.class, submissionService);
        registry.register(ReportModule.class, this);
    }

    private void openCategoryPicker(Player reporter, ReportCommand.ReportContext context) {
        ReportXConfig snapshot = configModule.snapshot();
        Gui gui = CategoryPickerGuiFactory.buildPlayerPicker(
                snapshot.gui(),
                snapshot.categories(),
                snapshot.config().gui().clickSound(),
                category -> handleCategoryChosen(reporter, context, category));
        openGui(reporter, gui);
    }

    private void handleCategoryChosen(Player reporter, ReportCommand.ReportContext context, String category) {
        ReportXConfig snapshot = configModule.snapshot();
        ReportSubmissionRequest request = new ReportSubmissionRequest(
                reporter.getUniqueId(),
                context.target().getUniqueId(),
                category,
                context.reason(),
                context.serverName(),
                context.reporterCoords());

        ReportSubmissionResult result = submissionService.submit(
                request, snapshot.config(), snapshot.categories());

        sendResult(reporter, snapshot.messages(), result, context, category);
    }

    private void sendResult(Player reporter, MessagesYaml messages,
                            ReportSubmissionResult result,
                            ReportCommand.ReportContext context,
                            String category) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", context.target().getName());
        placeholders.put("target", context.target().getName());
        placeholders.put("category", category);

        switch (result) {
            case ReportSubmissionResult.Success ignored ->
                    send(reporter, messages, "report.submitted", placeholders,
                            "<green>Your report has been submitted. Thank you.");
            case ReportSubmissionResult.OnCooldown onCooldown -> {
                placeholders.put("seconds", String.valueOf(onCooldown.remainingSeconds()));
                send(reporter, messages, "report.on-cooldown", placeholders,
                        "<red>Please wait {seconds}s before reporting again.");
            }
            case ReportSubmissionResult.SelfReport ignored ->
                    send(reporter, messages, "report.self-report", placeholders,
                            "<red>You cannot report yourself.");
            case ReportSubmissionResult.UnknownCategory unknown -> {
                placeholders.put("category", unknown.category());
                send(reporter, messages, "report.unknown-category", placeholders,
                        "<red>Unknown category: {category}");
            }
            case ReportSubmissionResult.ReasonRequired ignored ->
                    send(reporter, messages, "report.reason-required", placeholders,
                            "<red>This server requires a reason. Use /report <player> <reason>.");
        }
    }

    private static void send(Player player, MessagesYaml messages, String key,
                             Map<String, String> placeholders, String fallback) {
        String raw = messages == null ? fallback : messages.get(key).orElse(fallback);
        String prefix = messages == null ? PREFIX_KEY_FALLBACK : (messages.prefix() == null ? "" : messages.prefix());
        Component component = Text.parse(prefix + raw, placeholders);
        player.sendMessage(component);
    }
}
