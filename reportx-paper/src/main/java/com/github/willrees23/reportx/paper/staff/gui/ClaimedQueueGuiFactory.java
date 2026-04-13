package com.github.willrees23.reportx.paper.staff.gui;

import com.github.willrees23.reportx.core.config.GuiYaml;
import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.CaseStatus;
import com.github.willrees23.reportx.core.storage.CaseRepository;
import com.github.willrees23.reportx.core.storage.ReportRepository;
import com.github.willrees23.reportx.core.util.Ids;
import com.github.willrees23.reportx.paper.text.Items;
import com.github.willrees23.reportx.paper.text.Text;
import com.github.willrees23.solo.gui.Gui;
import com.github.willrees23.solo.gui.GuiButton;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class ClaimedQueueGuiFactory {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private static final int[] DEFAULT_CONTENT_SLOTS = new int[]{
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private ClaimedQueueGuiFactory() {
    }

    public static Gui build(GuiYaml guiConfig,
                            CaseRepository caseRepository,
                            ReportRepository reportRepository,
                            Consumer<Case> onClick) {
        Map<String, Object> spec = guiConfig.gui("claimed-queue")
                .orElseThrow(() -> new IllegalStateException("gui.yml missing claimed-queue"));

        List<Case> cases = caseRepository.findByStatus(CaseStatus.CLAIMED);

        Map<String, String> titlePlaceholders = new HashMap<>();
        titlePlaceholders.put("count", String.valueOf(cases.size()));
        String title = Text.legacy(
                GuiSpecReader.stringOr(spec, "title", "<dark_gray>Claimed Reports"),
                titlePlaceholders);

        int rows = GuiSpecReader.intOr(spec, "rows", 6);
        int[] contentSlots = toIntArray(GuiSpecReader.intList(spec.get("content-slots")), DEFAULT_CONTENT_SLOTS);

        Map<String, Object> caseItemSpec = GuiSpecReader.subMap(spec, "case-item");
        String caseMaterial = caseItemSpec == null ? "PLAYER_HEAD"
                : GuiSpecReader.stringOr(caseItemSpec, "material", "PLAYER_HEAD");
        String caseDisplay = caseItemSpec == null ? "<yellow>{target} <gray>— <white>{category}"
                : GuiSpecReader.stringOr(caseItemSpec, "display", "<yellow>{target} <gray>— <white>{category}");
        List<String> caseLore = caseItemSpec == null ? List.of()
                : GuiSpecReader.stringList(caseItemSpec.get("lore"));

        List<GuiButton> items = new ArrayList<>(cases.size());
        for (Case value : cases) {
            int reportCount = reportRepository.findByCase(value.id()).size();
            Map<String, String> placeholders = caseItemPlaceholders(value, reportCount);
            items.add(GuiButton.of(
                    Items.build(caseMaterial, caseDisplay, caseLore, placeholders),
                    event -> onClick.accept(value)));
        }

        Gui.Builder builder = Gui.builder()
                .title(title)
                .rows(rows)
                .paginated(items, contentSlots.length, contentSlots);

        applyNav(builder, spec);
        builder.filler(GuiSpecReader.fillerFromSpec(spec, "BLACK_STAINED_GLASS_PANE"));

        return builder.build();
    }

    private static void applyNav(Gui.Builder builder, Map<String, Object> spec) {
        Map<String, Object> nav = GuiSpecReader.subMap(spec, "nav");
        if (nav == null) return;
        Map<String, Object> previous = GuiSpecReader.subMap(nav, "previous");
        if (previous != null) {
            int slot = GuiSpecReader.intOr(previous, "slot", 48);
            String material = GuiSpecReader.stringOr(previous, "material", "ARROW");
            String display = GuiSpecReader.stringOr(previous, "display", "<yellow>Previous");
            builder.button(slot, GuiButton.previousPage(Items.build(material, display, List.of())));
        }
        Map<String, Object> next = GuiSpecReader.subMap(nav, "next");
        if (next != null) {
            int slot = GuiSpecReader.intOr(next, "slot", 50);
            String material = GuiSpecReader.stringOr(next, "material", "ARROW");
            String display = GuiSpecReader.stringOr(next, "display", "<yellow>Next");
            builder.button(slot, GuiButton.nextPage(Items.build(material, display, List.of())));
        }
    }

    private static Map<String, String> caseItemPlaceholders(Case value, int reportCount) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("target", lookupName(value.targetId()));
        placeholders.put("category", value.category());
        placeholders.put("report_count", String.valueOf(reportCount));
        placeholders.put("staff", value.claimedBy() == null ? "—" : lookupName(value.claimedBy()));
        placeholders.put("claimed_at", value.claimedAt() == null ? "—" : TIMESTAMP.format(value.claimedAt()));
        placeholders.put("id", Ids.shortCaseId(value.id()));
        return placeholders;
    }

    private static String lookupName(UUID id) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(id);
        String name = player.getName();
        return name == null ? id.toString().substring(0, 8) : name;
    }

    private static int[] toIntArray(List<Integer> list, int[] fallback) {
        if (list == null || list.isEmpty()) return fallback;
        int[] out = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = list.get(i);
        }
        return out;
    }
}
