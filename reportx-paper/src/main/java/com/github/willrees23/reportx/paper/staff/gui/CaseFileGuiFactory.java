package com.github.willrees23.reportx.paper.staff.gui;

import com.github.willrees23.reportx.core.config.GuiYaml;
import com.github.willrees23.reportx.core.config.ReputationYaml;
import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.ReputationSnapshot;
import com.github.willrees23.reportx.core.util.Ids;
import com.github.willrees23.reportx.paper.text.Items;
import com.github.willrees23.reportx.paper.text.Text;
import com.github.willrees23.solo.gui.Gui;
import com.github.willrees23.solo.gui.GuiButton;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CaseFileGuiFactory {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public interface ActionHandler {
        void onTeleport();

        void onViewLogs();

        void onAttachEvidence();

        void onViewEvidence();

        void onAddNote();

        void onViewNotes();

        void onViewAuditLog();

        void onRelease();

        void onHandoff();

        void onResolveAccept();

        void onResolveDeny();
    }

    private CaseFileGuiFactory() {
    }

    public static Gui build(GuiYaml guiConfig,
                            Case caseValue,
                            int reportCount,
                            int evidenceCount,
                            int noteCount,
                            ReputationSnapshot reputation,
                            ReputationYaml reputationConfig,
                            ActionHandler handler) {
        Map<String, Object> spec = guiConfig.gui("case-file")
                .orElseThrow(() -> new IllegalStateException("gui.yml missing case-file"));

        Map<String, String> headerPlaceholders = headerPlaceholders(caseValue);
        String title = Text.legacy(GuiSpecReader.stringOr(spec, "title",
                "<dark_gray>Case #{id} <gray>— <yellow>{target}"), headerPlaceholders);
        int rows = GuiSpecReader.intOr(spec, "rows", 6);

        Gui.Builder builder = Gui.builder().title(title).rows(rows);

        applyPlayerInfo(builder, spec, caseValue, reputation, reputationConfig);
        applyReportsInfo(builder, spec, caseValue, reportCount);
        applyButtons(builder, spec, caseValue, evidenceCount, noteCount, handler);

        builder.filler(GuiSpecReader.fillerFromSpec(spec, "GRAY_STAINED_GLASS_PANE"));
        GuiButton border = GuiSpecReader.borderFromSpec(spec, "BLACK_STAINED_GLASS_PANE");
        if (border != null) {
            builder.border(border);
        }

        return builder.build();
    }

    private static void applyPlayerInfo(Gui.Builder builder,
                                        Map<String, Object> spec,
                                        Case caseValue,
                                        ReputationSnapshot reputation,
                                        ReputationYaml reputationConfig) {
        Map<String, Object> playerSpec = GuiSpecReader.subMap(spec, "player-info");
        if (playerSpec == null) return;

        OfflinePlayer target = Bukkit.getOfflinePlayer(caseValue.targetId());
        Player online = Bukkit.getPlayer(caseValue.targetId());

        Map<String, String> placeholders = new HashMap<>(headerPlaceholders(caseValue));
        placeholders.put("uuid", caseValue.targetId().toString());
        placeholders.put("server", online == null ? "offline" : online.getServer().getName());
        placeholders.put("world", online == null ? "—" : online.getWorld().getName());
        placeholders.put("x", online == null ? "—" : formatNum(online.getLocation().getX()));
        placeholders.put("y", online == null ? "—" : formatNum(online.getLocation().getY()));
        placeholders.put("z", online == null ? "—" : formatNum(online.getLocation().getZ()));
        placeholders.put("ping", online == null ? "—" : String.valueOf(online.getPing()));
        placeholders.put("playtime", online == null ? "—"
                : formatPlaytimeTicks(online.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE)));

        ReputationYaml.Tier tier = reputationConfig.tiers().stream()
                .filter(t -> t.id().equals(reputation.tierId()))
                .findFirst()
                .orElse(reputationConfig.tiers().get(0));
        placeholders.put("reputation_tier", tier.display());
        placeholders.put("reputation_description",
                tier.description().replace("{count}", String.valueOf(reputation.rawCount())));

        int slot = GuiSpecReader.intOr(playerSpec, "slot", 4);
        String material = GuiSpecReader.stringOr(playerSpec, "material", "PLAYER_HEAD");
        String display = GuiSpecReader.stringOr(playerSpec, "display", "<yellow>{target}");
        List<String> lore = GuiSpecReader.stringList(playerSpec.get("lore"));

        builder.button(slot, GuiButton.of(
                playerHead(material, target, display, lore, placeholders)));
    }

    private static void applyReportsInfo(Gui.Builder builder,
                                         Map<String, Object> spec,
                                         Case caseValue,
                                         int reportCount) {
        Map<String, Object> reportsSpec = GuiSpecReader.subMap(spec, "reports-info");
        if (reportsSpec == null) return;

        Map<String, String> placeholders = new HashMap<>(headerPlaceholders(caseValue));
        placeholders.put("report_count", String.valueOf(reportCount));
        placeholders.put("created", TIMESTAMP.format(caseValue.createdAt()));
        placeholders.put("staff", caseValue.claimedBy() == null ? "—" : caseValue.claimedBy().toString().substring(0, 8));

        int slot = GuiSpecReader.intOr(reportsSpec, "slot", 13);
        String material = GuiSpecReader.stringOr(reportsSpec, "material", "WRITABLE_BOOK");
        String display = GuiSpecReader.stringOr(reportsSpec, "display", "<yellow>Reports ({report_count})");
        List<String> lore = GuiSpecReader.stringList(reportsSpec.get("lore"));

        builder.button(slot, GuiButton.of(Items.build(material, display, lore, placeholders)));
    }

    private static void applyButtons(Gui.Builder builder,
                                     Map<String, Object> spec,
                                     Case caseValue,
                                     int evidenceCount,
                                     int noteCount,
                                     ActionHandler handler) {
        Map<String, Object> buttonsSpec = GuiSpecReader.subMap(spec, "buttons");
        if (buttonsSpec == null) return;

        Map<String, String> placeholders = new HashMap<>(headerPlaceholders(caseValue));
        placeholders.put("evidence_count", String.valueOf(evidenceCount));
        placeholders.put("note_count", String.valueOf(noteCount));

        addButton(builder, buttonsSpec, "teleport", placeholders, click -> handler.onTeleport());
        addButton(builder, buttonsSpec, "view-logs", placeholders, click -> handler.onViewLogs());
        addButton(builder, buttonsSpec, "attach-evidence", placeholders, click -> handler.onAttachEvidence());
        addButton(builder, buttonsSpec, "view-evidence", placeholders, click -> handler.onViewEvidence());
        addButton(builder, buttonsSpec, "add-note", placeholders, click -> handler.onAddNote());
        addButton(builder, buttonsSpec, "view-notes", placeholders, click -> handler.onViewNotes());
        addButton(builder, buttonsSpec, "view-audit-log", placeholders, click -> handler.onViewAuditLog());
        addButton(builder, buttonsSpec, "release", placeholders, click -> handler.onRelease());
        addButton(builder, buttonsSpec, "handoff", placeholders, click -> handler.onHandoff());
        addButton(builder, buttonsSpec, "resolve-accept", placeholders, click -> handler.onResolveAccept());
        addButton(builder, buttonsSpec, "resolve-deny", placeholders, click -> handler.onResolveDeny());
    }

    private static void addButton(Gui.Builder builder,
                                  Map<String, Object> buttonsSpec,
                                  String key,
                                  Map<String, String> placeholders,
                                  java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> handler) {
        Map<String, Object> buttonSpec = GuiSpecReader.subMap(buttonsSpec, key);
        if (buttonSpec == null) return;
        int slot = GuiSpecReader.intOr(buttonSpec, "slot", -1);
        if (slot < 0) return;
        String material = GuiSpecReader.stringOr(buttonSpec, "material", "STONE");
        String display = GuiSpecReader.stringOr(buttonSpec, "display", "<white>" + key);
        List<String> lore = GuiSpecReader.stringList(buttonSpec.get("lore"));
        builder.button(slot, GuiButton.of(Items.build(material, display, lore, placeholders), handler));
    }

    private static Map<String, String> headerPlaceholders(Case caseValue) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", Ids.shortCaseId(caseValue.id()));
        placeholders.put("category", caseValue.category());
        placeholders.put("status", caseValue.status().name());
        placeholders.put("target", lookupName(caseValue.targetId()));
        placeholders.put("staff", caseValue.claimedBy() == null ? "—" : lookupName(caseValue.claimedBy()));
        placeholders.put("created", TIMESTAMP.format(caseValue.createdAt()));
        placeholders.put("claimed_at", caseValue.claimedAt() == null ? "—" : TIMESTAMP.format(caseValue.claimedAt()));
        return placeholders;
    }

    private static String lookupName(UUID id) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(id);
        String name = player.getName();
        return name == null ? id.toString().substring(0, 8) : name;
    }

    private static org.bukkit.inventory.ItemStack playerHead(String fallbackMaterial,
                                                             OfflinePlayer target,
                                                             String display,
                                                             List<String> lore,
                                                             Map<String, String> placeholders) {
        org.bukkit.inventory.ItemStack base = Items.build(fallbackMaterial, display, lore, placeholders);
        if (base.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(target);
            base.setItemMeta(skullMeta);
        }
        return base;
    }

    private static String formatNum(double value) {
        return String.format("%.1f", value);
    }

    private static String formatPlaytimeTicks(int ticks) {
        long seconds = ticks / 20L;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }
}
