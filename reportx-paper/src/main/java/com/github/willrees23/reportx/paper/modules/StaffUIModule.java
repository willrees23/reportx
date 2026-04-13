package com.github.willrees23.reportx.paper.modules;

import com.github.willrees23.reportx.core.config.MessagesYaml;
import com.github.willrees23.reportx.core.config.ReportXConfig;
import com.github.willrees23.reportx.core.model.AuditEntry;
import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import com.github.willrees23.reportx.core.model.Evidence;
import com.github.willrees23.reportx.core.model.LogBufferEntry;
import com.github.willrees23.reportx.core.model.Note;
import com.github.willrees23.reportx.core.model.ReputationSnapshot;
import com.github.willrees23.reportx.core.storage.AuditRepository;
import com.github.willrees23.reportx.core.storage.CaseRepository;
import com.github.willrees23.reportx.core.storage.LogBufferRepository;
import com.github.willrees23.reportx.core.storage.ReportRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.reportx.core.util.Ids;
import com.github.willrees23.reportx.paper.cases.CaseLifecycleOutcome;
import com.github.willrees23.reportx.paper.cases.CaseService;
import com.github.willrees23.reportx.paper.evidence.EvidenceOutcome;
import com.github.willrees23.reportx.paper.evidence.EvidenceService;
import com.github.willrees23.reportx.paper.notes.NoteOutcome;
import com.github.willrees23.reportx.paper.notes.NoteService;
import com.github.willrees23.reportx.paper.reputation.ReputationService;
import com.github.willrees23.reportx.paper.staff.ChatPromptService;
import com.github.willrees23.reportx.paper.staff.StaffSession;
import com.github.willrees23.reportx.paper.staff.StaffSessionRegistry;
import com.github.willrees23.reportx.paper.staff.commands.ClaimedReportsCommand;
import com.github.willrees23.reportx.paper.staff.commands.ReportHandleCommand;
import com.github.willrees23.reportx.paper.staff.commands.ReportsCommand;
import com.github.willrees23.reportx.paper.staff.gui.CaseFileGuiFactory;
import com.github.willrees23.reportx.paper.staff.gui.ClaimedQueueGuiFactory;
import com.github.willrees23.reportx.paper.staff.gui.StaffCategoryPickerGuiFactory;
import com.github.willrees23.reportx.paper.staff.gui.UnclaimedQueueGuiFactory;
import com.github.willrees23.reportx.paper.text.Text;
import com.github.willrees23.solo.gui.Gui;
import com.github.willrees23.solo.module.Module;
import com.github.willrees23.solo.registry.ServiceRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class StaffUIModule extends Module {

    private static final DateTimeFormatter LOG_TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final Duration LOG_WINDOW = Duration.ofHours(24);

    private ConfigModule configModule;
    private CaseRepository caseRepository;
    private ReportRepository reportRepository;
    private LogBufferRepository logBufferRepository;
    private AuditRepository auditRepository;
    private CaseService caseService;
    private EvidenceService evidenceService;
    private NoteService noteService;
    private ReputationService reputationService;
    private StaffSessionRegistry sessionRegistry;
    private ChatPromptService chatPromptService;

    @Override
    public String getName() {
        return "StaffUI";
    }

    @Override
    public List<String> getDependencies() {
        return List.of("Config", "Storage", "Case", "Evidence", "Note", "Reputation");
    }

    @Override
    protected void onEnable() {
        ServiceRegistry registry = getContext().getServiceRegistry();
        this.configModule = registry.require(ConfigModule.class);
        this.caseRepository = registry.require(CaseRepository.class);
        this.reportRepository = registry.require(ReportRepository.class);
        this.logBufferRepository = registry.require(LogBufferRepository.class);
        this.auditRepository = registry.require(AuditRepository.class);
        this.caseService = registry.require(CaseService.class);
        this.evidenceService = registry.require(EvidenceService.class);
        this.noteService = registry.require(NoteService.class);
        this.reputationService = registry.require(ReputationService.class);

        this.sessionRegistry = new StaffSessionRegistry(Clock.system());
        this.chatPromptService = new ChatPromptService(getContext().getPlugin());

        registerListener(chatPromptService);
        registerCommand(new ReportsCommand(this::openQueue));
        registerCommand(new ClaimedReportsCommand(this::openClaimedQueue));
        registerCommand(new ReportHandleCommand(new HandleHandlersImpl()));

        registry.register(StaffSessionRegistry.class, sessionRegistry);
        registry.register(ChatPromptService.class, chatPromptService);
        registry.register(StaffUIModule.class, this);
    }

    private void openQueue(Player viewer) {
        ReportXConfig snapshot = configModule.snapshot();
        Gui gui = UnclaimedQueueGuiFactory.build(
                snapshot.gui(),
                caseRepository,
                reportRepository,
                Instant.now(),
                value -> handleQueueClick(viewer, value));
        openGui(viewer, gui);
    }

    private void handleQueueClick(Player viewer, Case value) {
        viewer.closeInventory();
        if (sessionRegistry.isHandling(viewer.getUniqueId())) {
            UUID currentCase = sessionRegistry.currentFor(viewer.getUniqueId()).map(StaffSession::caseId).orElseThrow();
            send(viewer, "errors.already-handling", Map.of("id", Ids.shortCaseId(currentCase)),
                    "<red>You are already handling case <gray>#{id}</gray>. Resolve or release it first.");
            return;
        }
        CaseLifecycleOutcome outcome = caseService.claim(
                value.id(), viewer.getUniqueId(), viewer.getServer().getName());
        switch (outcome) {
            case CaseLifecycleOutcome.Success success -> {
                sessionRegistry.start(viewer.getUniqueId(), success.caseValue().id());
                send(viewer, "staff.case-claimed",
                        Map.of("id", Ids.shortCaseId(success.caseValue().id()),
                                "staff", viewer.getName()),
                        "<aqua>{staff}</aqua> claimed case <gray>#{id}</gray>.");
                openCaseFile(viewer, success.caseValue());
            }
            case CaseLifecycleOutcome.AlreadyClaimed already ->
                    send(viewer, "errors.case-already-claimed",
                            Map.of("id", Ids.shortCaseId(value.id()),
                                    "claimer", already.currentClaimer().toString().substring(0, 8)),
                            "<red>That case is already claimed by <white>{claimer}</white>.");
            case CaseLifecycleOutcome.AlreadyResolved ignored ->
                    send(viewer, "errors.case-already-resolved", Map.of(),
                            "<red>That case has already been resolved.");
            case CaseLifecycleOutcome.NotFound ignored ->
                    send(viewer, "errors.case-not-found", Map.of(),
                            "<red>That case no longer exists.");
            case CaseLifecycleOutcome.NotClaimed ignored ->
                    send(viewer, "errors.unexpected", Map.of(),
                            "<red>Unexpected case state — try refreshing the queue.");
            case CaseLifecycleOutcome.NotClaimer ignored ->
                    send(viewer, "errors.unexpected", Map.of(),
                            "<red>Unexpected case state — try refreshing the queue.");
            case CaseLifecycleOutcome.NotResolved ignored ->
                    send(viewer, "errors.unexpected", Map.of(),
                            "<red>Unexpected case state — try refreshing the queue.");
        }
    }

    private void openCurrent(Player viewer) {
        Optional<StaffSession> session = sessionRegistry.currentFor(viewer.getUniqueId());
        if (session.isEmpty()) {
            send(viewer, "errors.not-handling", Map.of(),
                    "<red>You aren't currently handling a case.");
            return;
        }
        Optional<Case> caseValue = caseRepository.findById(session.get().caseId());
        if (caseValue.isEmpty()) {
            sessionRegistry.end(viewer.getUniqueId());
            send(viewer, "errors.case-not-found", Map.of(),
                    "<red>That case no longer exists.");
            return;
        }
        openCaseFile(viewer, caseValue.get());
    }

    private void openClaimedQueue(Player viewer) {
        ReportXConfig snapshot = configModule.snapshot();
        Gui gui = ClaimedQueueGuiFactory.build(
                snapshot.gui(),
                caseRepository,
                reportRepository,
                value -> handleClaimedQueueClick(viewer, value));
        openGui(viewer, gui);
    }

    private void handleClaimedQueueClick(Player viewer, Case caseValue) {
        viewer.closeInventory();
        sendAuditDump(viewer, caseValue);
        openCaseFile(viewer, caseValue);
    }

    private void sendAuditDump(Player viewer, Case caseValue) {
        List<AuditEntry> entries = auditRepository.findByCase(caseValue.id());
        if (entries.isEmpty()) {
            send(viewer, "staff.audit-empty", Map.of("id", Ids.shortCaseId(caseValue.id())),
                    "<gray>No audit entries for case <white>#{id}</white>.");
            return;
        }
        sendRaw(viewer, "<gold>=== Audit Log — case #" + Ids.shortCaseId(caseValue.id()) + " ===");
        for (AuditEntry entry : entries) {
            String actor = entry.actorId() == null ? "system" : lookupName(entry.actorId());
            sendRaw(viewer, "<gray>[" + LOG_TIME.format(entry.createdAt()) + "] <yellow>"
                    + entry.eventType() + " <gray>by <white>" + actor);
        }
    }

    private void openStaffCategoryPicker(Player viewer) {
        ReportXConfig snapshot = configModule.snapshot();
        Gui gui = StaffCategoryPickerGuiFactory.build(
                snapshot.gui(),
                snapshot.categories(),
                category -> caseRepository.findByStatus(CaseStatus.UNCLAIMED).stream()
                        .filter(c -> c.category().equalsIgnoreCase(category))
                        .toList()
                        .size(),
                category -> handlePickerClick(viewer, category));
        openGui(viewer, gui);
    }

    private void handlePickerClick(Player viewer, String categoryId) {
        Optional<Case> oldest = caseRepository.findOldestUnclaimedByCategory(categoryId);
        if (oldest.isEmpty()) {
            send(viewer, "staff.no-reports-in-category",
                    Map.of("category", categoryId),
                    "<red>No reports available in <white>{category}</white>.");
            return;
        }
        handleQueueClick(viewer, oldest.get());
    }

    private final class HandleHandlersImpl implements ReportHandleCommand.Handlers {

        @Override
        public void openDefault(Player viewer) {
            if (sessionRegistry.isHandling(viewer.getUniqueId())) {
                openCurrent(viewer);
            } else {
                openStaffCategoryPicker(viewer);
            }
        }

        @Override
        public void release(Player viewer) {
            Optional<StaffSession> session = sessionRegistry.currentFor(viewer.getUniqueId());
            if (session.isEmpty()) {
                send(viewer, "errors.not-handling", Map.of(),
                        "<red>You aren't currently handling a case.");
                return;
            }
            CaseLifecycleOutcome outcome = caseService.release(session.get().caseId(), viewer.getUniqueId());
            if (outcome instanceof CaseLifecycleOutcome.Success success) {
                sessionRegistry.end(viewer.getUniqueId());
                send(viewer, "staff.case-released",
                        Map.of("id", Ids.shortCaseId(success.caseValue().id()),
                                "staff", viewer.getName()),
                        "<aqua>{staff}</aqua> released case <gray>#{id}</gray>.");
            } else {
                send(viewer, "errors.unexpected", Map.of(),
                        "<red>Could not release that case.");
            }
        }

        @Override
        public void handoff(Player viewer, String targetName) {
            Optional<StaffSession> session = sessionRegistry.currentFor(viewer.getUniqueId());
            if (session.isEmpty()) {
                send(viewer, "errors.not-handling", Map.of(),
                        "<red>You aren't currently handling a case.");
                return;
            }
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                send(viewer, "errors.target-offline", Map.of("name", targetName),
                        "<red>That staff member is not online.");
                return;
            }
            UUID caseId = session.get().caseId();
            CaseLifecycleOutcome outcome = caseService.handoff(caseId, viewer.getUniqueId(), target.getUniqueId());
            if (outcome instanceof CaseLifecycleOutcome.Success success) {
                sessionRegistry.end(viewer.getUniqueId());
                sessionRegistry.start(target.getUniqueId(), success.caseValue().id());
                send(viewer, "staff.handoff-sent",
                        Map.of("to", target.getName(), "id", Ids.shortCaseId(caseId)),
                        "<aqua>Handed off case <gray>#{id}</gray> to <white>{to}</white>.");
                send(target, "staff.handoff-received",
                        Map.of("from", viewer.getName(), "id", Ids.shortCaseId(caseId)),
                        "<aqua>{from}</aqua> handed case <gray>#{id}</gray> to you.");
            } else {
                send(viewer, "errors.unexpected", Map.of(),
                        "<red>Could not hand off that case.");
            }
        }

        @Override
        public void reopen(Player viewer, String shortCaseId) {
            Optional<Case> match = findCaseByShortId(shortCaseId);
            if (match.isEmpty()) {
                send(viewer, "errors.case-not-found",
                        Map.of("id", shortCaseId),
                        "<red>No case matches <white>#{id}</white>.");
                return;
            }
            CaseLifecycleOutcome outcome = caseService.reopen(
                    match.get().id(), viewer.getUniqueId(), "reopened via /rh reopen");
            if (outcome instanceof CaseLifecycleOutcome.Success success) {
                send(viewer, "staff.reopened",
                        Map.of("id", Ids.shortCaseId(success.caseValue().id()),
                                "staff", viewer.getName()),
                        "<aqua>{staff}</aqua> reopened case <gray>#{id}</gray>.");
            } else if (outcome instanceof CaseLifecycleOutcome.NotResolved) {
                send(viewer, "errors.case-not-resolved",
                        Map.of("id", shortCaseId),
                        "<red>Case <white>#{id}</white> isn't resolved, so it can't be reopened.");
            } else {
                send(viewer, "errors.unexpected", Map.of(),
                        "<red>Could not reopen that case.");
            }
        }

        @Override
        public void unknownSubcommand(Player viewer, String input) {
            send(viewer, "staff.handle-unknown-subcommand",
                    Map.of("input", input),
                    "<red>Unknown subcommand: <white>{input}</white>. Try /rh release | handoff <staff> | reopen <case-id>.");
        }

        @Override
        public void usage(Player viewer, String key, String fallback) {
            send(viewer, key, Map.of(), fallback);
        }
    }

    private Optional<Case> findCaseByShortId(String shortId) {
        if (shortId == null || shortId.length() < 4) {
            return Optional.empty();
        }
        String prefix = shortId.toLowerCase();
        for (CaseStatus status : new CaseStatus[]{CaseStatus.RESOLVED_ACCEPTED, CaseStatus.RESOLVED_DENIED}) {
            for (Case candidate : caseRepository.findByStatus(status)) {
                if (Ids.shortCaseId(candidate.id()).toLowerCase().startsWith(prefix)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    private void openCaseFile(Player viewer, Case caseValue) {
        ReportXConfig snapshot = configModule.snapshot();
        int reportCount = reportRepository.findByCase(caseValue.id()).size();
        int evidenceCount = evidenceService.listForCase(caseValue.id()).size();
        int noteCount = noteService.listForCase(caseValue.id()).size();
        ReputationSnapshot reputation = reputationService.compute(caseValue.targetId(), snapshot.reputation());

        Gui gui = CaseFileGuiFactory.build(
                snapshot.gui(),
                caseValue, reportCount, evidenceCount, noteCount,
                reputation, snapshot.reputation(),
                new ActionHandlerImpl(viewer, caseValue));
        openGui(viewer, gui);
    }

    private final class ActionHandlerImpl implements CaseFileGuiFactory.ActionHandler {

        private final Player viewer;
        private final Case caseValue;

        private ActionHandlerImpl(Player viewer, Case caseValue) {
            this.viewer = viewer;
            this.caseValue = caseValue;
        }

        @Override
        public void onTeleport() {
            Player target = Bukkit.getPlayer(caseValue.targetId());
            if (target == null) {
                send(viewer, "errors.target-offline", Map.of(),
                        "<red>That player is not online to teleport to.");
                return;
            }
            viewer.closeInventory();
            viewer.teleport(target.getLocation());
            send(viewer, "staff.teleported", Map.of("target", target.getName()),
                    "<green>Teleported to <white>{target}</white>.");
        }

        @Override
        public void onViewLogs() {
            viewer.closeInventory();
            List<LogBufferEntry> logs = logBufferRepository.findByPlayerSince(
                    caseValue.targetId(), Instant.now().minus(LOG_WINDOW));
            if (logs.isEmpty()) {
                send(viewer, "staff.logs-empty", Map.of(),
                        "<gray>No buffered logs for this player in the last 24 hours.");
                return;
            }
            sendRaw(viewer, "<gold>=== Logs (last 24h) ===");
            for (LogBufferEntry entry : logs) {
                sendRaw(viewer, "<gray>[" + LOG_TIME.format(entry.createdAt()) + "] <yellow>"
                        + entry.type() + "<gray>: <white>" + entry.content());
            }
        }

        @Override
        public void onAttachEvidence() {
            viewer.closeInventory();
            chatPromptService.prompt(viewer,
                    Text.parse("<yellow>Type evidence as <gray>label | content<yellow> (or 'cancel'):"),
                    response -> handleEvidenceInput(response),
                    () -> send(viewer, "staff.cancelled", Map.of(),
                            "<gray>Cancelled."));
        }

        private void handleEvidenceInput(String response) {
            String label;
            String content;
            int separator = response.indexOf('|');
            if (separator < 0) {
                label = "Evidence";
                content = response.trim();
            } else {
                label = response.substring(0, separator).trim();
                content = response.substring(separator + 1).trim();
            }
            EvidenceOutcome outcome = evidenceService.attach(
                    caseValue.id(), label, content, viewer.getUniqueId(),
                    configModule.snapshot().config().evidence().requireUrl());
            switch (outcome) {
                case EvidenceOutcome.Success success ->
                        send(viewer, "staff.evidence-attached",
                                Map.of("label", success.evidence().label()),
                                "<green>Evidence attached: <white>{label}</white>");
                case EvidenceOutcome.UrlRequired ignored ->
                        send(viewer, "staff.evidence-url-required", Map.of(),
                                "<red>Evidence content must contain a URL.");
                case EvidenceOutcome.EmptyContent ignored ->
                        send(viewer, "staff.evidence-empty", Map.of(),
                                "<red>Evidence content cannot be empty.");
                case EvidenceOutcome.NotAuthor ignored ->
                        send(viewer, "errors.unexpected", Map.of(),
                                "<red>Unexpected evidence error.");
                case EvidenceOutcome.NotFound ignored ->
                        send(viewer, "errors.unexpected", Map.of(),
                                "<red>Unexpected evidence error.");
            }
        }

        @Override
        public void onViewEvidence() {
            viewer.closeInventory();
            List<Evidence> evidence = evidenceService.listForCase(caseValue.id());
            if (evidence.isEmpty()) {
                send(viewer, "staff.evidence-empty-list", Map.of(),
                        "<gray>No evidence attached yet.");
                return;
            }
            sendRaw(viewer, "<gold>=== Evidence ===");
            for (Evidence e : evidence) {
                sendRaw(viewer, "<yellow>" + e.label() + "<gray>: <white>" + e.content());
            }
        }

        @Override
        public void onAddNote() {
            viewer.closeInventory();
            chatPromptService.prompt(viewer,
                    Text.parse("<yellow>Type your note in chat (or 'cancel'):"),
                    response -> {
                        NoteOutcome outcome = noteService.add(caseValue.id(), response, viewer.getUniqueId());
                        switch (outcome) {
                            case NoteOutcome.Success ignored ->
                                    send(viewer, "staff.note-added", Map.of(),
                                            "<green>Note added.");
                            case NoteOutcome.EmptyBody ignored ->
                                    send(viewer, "staff.note-empty", Map.of(),
                                            "<red>Note cannot be empty.");
                            case NoteOutcome.NotAuthor ignored ->
                                    send(viewer, "errors.unexpected", Map.of(),
                                            "<red>Unexpected note error.");
                            case NoteOutcome.NotFound ignored ->
                                    send(viewer, "errors.unexpected", Map.of(),
                                            "<red>Unexpected note error.");
                        }
                    },
                    () -> send(viewer, "staff.cancelled", Map.of(), "<gray>Cancelled."));
        }

        @Override
        public void onViewNotes() {
            viewer.closeInventory();
            List<Note> notes = noteService.listForCase(caseValue.id());
            if (notes.isEmpty()) {
                send(viewer, "staff.notes-empty-list", Map.of(),
                        "<gray>No notes on this case yet.");
                return;
            }
            sendRaw(viewer, "<gold>=== Notes ===");
            for (Note n : notes) {
                String author = lookupName(n.authorId());
                sendRaw(viewer, "<yellow>" + author + "<gray>: <white>" + n.body());
            }
        }

        @Override
        public void onRelease() {
            viewer.closeInventory();
            CaseLifecycleOutcome outcome = caseService.release(caseValue.id(), viewer.getUniqueId());
            handleLifecycleOutcome(outcome,
                    "staff.case-released",
                    "<aqua>{staff}</aqua> released case <gray>#{id}</gray>.",
                    true);
        }

        @Override
        public void onHandoff() {
            viewer.closeInventory();
            chatPromptService.prompt(viewer,
                    Text.parse("<yellow>Type the staff member to hand the case off to (or 'cancel'):"),
                    response -> {
                        Player target = Bukkit.getPlayerExact(response);
                        if (target == null) {
                            send(viewer, "errors.target-offline",
                                    Map.of("name", response),
                                    "<red>That staff member is not online.");
                            return;
                        }
                        CaseLifecycleOutcome outcome = caseService.handoff(
                                caseValue.id(), viewer.getUniqueId(), target.getUniqueId());
                        if (outcome instanceof CaseLifecycleOutcome.Success success) {
                            sessionRegistry.end(viewer.getUniqueId());
                            sessionRegistry.start(target.getUniqueId(), success.caseValue().id());
                            send(viewer, "staff.handoff-sent",
                                    Map.of("to", target.getName(), "id", Ids.shortCaseId(caseValue.id())),
                                    "<aqua>Handed off case <gray>#{id}</gray> to <white>{to}</white>.");
                            send(target, "staff.handoff-received",
                                    Map.of("from", viewer.getName(), "id", Ids.shortCaseId(caseValue.id())),
                                    "<aqua>{from}</aqua> handed case <gray>#{id}</gray> to you.");
                        } else {
                            handleLifecycleOutcome(outcome,
                                    "staff.handoff-sent",
                                    "<aqua>Handoff complete.",
                                    false);
                        }
                    },
                    () -> send(viewer, "staff.cancelled", Map.of(), "<gray>Cancelled."));
        }

        @Override
        public void onResolveAccept() {
            viewer.closeInventory();
            promptResolve(CaseStatus.RESOLVED_ACCEPTED, "staff.resolve-accepted",
                    "<green>Case <gray>#{id}</gray> resolved as accepted.");
        }

        @Override
        public void onResolveDeny() {
            viewer.closeInventory();
            promptResolve(CaseStatus.RESOLVED_DENIED, "staff.resolve-denied",
                    "<red>Case <gray>#{id}</gray> resolved as denied.");
        }

        private void promptResolve(CaseStatus outcome, String successKey, String fallback) {
            chatPromptService.prompt(viewer,
                    Text.parse("<yellow>Type a reason for the resolution (or 'cancel'):"),
                    response -> {
                        CaseLifecycleOutcome result = caseService.resolve(
                                caseValue.id(), viewer.getUniqueId(), outcome, response);
                        handleLifecycleOutcome(result, successKey, fallback, true);
                    },
                    () -> send(viewer, "staff.cancelled", Map.of(), "<gray>Cancelled."));
        }

        private void handleLifecycleOutcome(CaseLifecycleOutcome outcome,
                                            String successKey, String fallback,
                                            boolean endsSession) {
            switch (outcome) {
                case CaseLifecycleOutcome.Success success -> {
                    if (endsSession) {
                        sessionRegistry.end(viewer.getUniqueId());
                    }
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("id", Ids.shortCaseId(success.caseValue().id()));
                    placeholders.put("staff", viewer.getName());
                    send(viewer, successKey, placeholders, fallback);
                }
                case CaseLifecycleOutcome.NotClaimer notClaimer ->
                        send(viewer, "errors.not-claimer",
                                Map.of("claimer", notClaimer.currentClaimer().toString().substring(0, 8)),
                                "<red>Case is claimed by <white>{claimer}</white>, not you.");
                case CaseLifecycleOutcome.NotClaimed ignored ->
                        send(viewer, "errors.not-claimed", Map.of(),
                                "<red>That case is not currently claimed.");
                case CaseLifecycleOutcome.AlreadyClaimed already ->
                        send(viewer, "errors.case-already-claimed",
                                Map.of("claimer", already.currentClaimer().toString().substring(0, 8)),
                                "<red>That case is already claimed by <white>{claimer}</white>.");
                case CaseLifecycleOutcome.AlreadyResolved ignored ->
                        send(viewer, "errors.case-already-resolved", Map.of(),
                                "<red>That case is already resolved.");
                case CaseLifecycleOutcome.NotResolved ignored ->
                        send(viewer, "errors.case-not-resolved", Map.of(),
                                "<red>That case has not been resolved.");
                case CaseLifecycleOutcome.NotFound ignored ->
                        send(viewer, "errors.case-not-found", Map.of(),
                                "<red>That case no longer exists.");
            }
        }
    }

    private void send(Player player, String key, Map<String, String> placeholders, String fallback) {
        MessagesYaml messages = configModule.snapshot().messages();
        String raw = messages == null ? fallback : messages.get(key).orElse(fallback);
        String prefix = messages == null ? "" : (messages.prefix() == null ? "" : messages.prefix());
        Component component = Text.parse(prefix + raw, placeholders);
        player.sendMessage(component);
    }

    private void sendRaw(Player player, String mini) {
        player.sendMessage(Text.parse(mini));
    }

    private static String lookupName(UUID id) {
        org.bukkit.OfflinePlayer player = Bukkit.getOfflinePlayer(id);
        String name = player.getName();
        return name == null ? id.toString().substring(0, 8) : name;
    }
}
