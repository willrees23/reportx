package com.github.willrees23.reportx.paper.staff.gui;

import com.github.willrees23.reportx.core.config.GuiYaml;
import com.github.willrees23.reportx.core.model.AuditEntry;
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

public final class AuditLogGuiFactory {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final int[] DEFAULT_CONTENT_SLOTS = new int[]{
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private AuditLogGuiFactory() {
    }

    public static Gui build(GuiYaml guiConfig,
                            UUID caseId,
                            List<AuditEntry> entries,
                            Runnable onBack) {
        Map<String, Object> spec = guiConfig.gui("audit-log")
                .orElseThrow(() -> new IllegalStateException("gui.yml missing audit-log"));

        Map<String, String> titlePlaceholders = new HashMap<>();
        titlePlaceholders.put("id", Ids.shortCaseId(caseId));
        String title = Text.legacy(GuiSpecReader.stringOr(spec, "title",
                "<dark_gray>Audit Log <gray>— <yellow>Case #{id}"), titlePlaceholders);
        int rows = GuiSpecReader.intOr(spec, "rows", 6);
        int[] contentSlots = toIntArray(GuiSpecReader.intList(spec.get("content-slots")), DEFAULT_CONTENT_SLOTS);

        Map<String, Object> entrySpec = GuiSpecReader.subMap(spec, "entry");
        String entryMaterial = entrySpec == null ? "KNOWLEDGE_BOOK"
                : GuiSpecReader.stringOr(entrySpec, "material", "KNOWLEDGE_BOOK");
        String entryDisplay = entrySpec == null ? "<yellow>{event_type}"
                : GuiSpecReader.stringOr(entrySpec, "display", "<yellow>{event_type}");
        List<String> entryLore = entrySpec == null ? List.of()
                : GuiSpecReader.stringList(entrySpec.get("lore"));

        List<GuiButton> items = new ArrayList<>(entries.size());
        for (AuditEntry entry : entries) {
            Map<String, String> placeholders = entryPlaceholders(entry);
            items.add(GuiButton.of(Items.build(entryMaterial, entryDisplay, entryLore, placeholders)));
        }

        Gui.Builder builder = Gui.builder()
                .title(title)
                .rows(rows)
                .paginated(items, contentSlots.length, contentSlots);

        applyBackButton(builder, spec, onBack);
        builder.filler(GuiSpecReader.fillerFromSpec(spec, "BLACK_STAINED_GLASS_PANE"));
        return builder.build();
    }

    static void applyBackButton(Gui.Builder builder, Map<String, Object> spec, Runnable onBack) {
        Map<String, Object> back = GuiSpecReader.subMap(spec, "back-button");
        if (back == null || onBack == null) {
            return;
        }
        int slot = GuiSpecReader.intOr(back, "slot", 49);
        String material = GuiSpecReader.stringOr(back, "material", "ARROW");
        String display = GuiSpecReader.stringOr(back, "display", "<yellow>Back");
        builder.button(slot, GuiButton.of(
                Items.build(material, display, List.of()),
                event -> {
                    event.getView().close();
                    onBack.run();
                }));
    }

    static int[] toIntArray(List<Integer> list, int[] fallback) {
        if (list == null || list.isEmpty()) return fallback;
        int[] out = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    private static Map<String, String> entryPlaceholders(AuditEntry entry) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("event_type", entry.eventType().name());
        placeholders.put("actor", entry.actorId() == null ? "system" : lookupName(entry.actorId()));
        placeholders.put("created", TIMESTAMP.format(entry.createdAt()));
        placeholders.put("summary", summarisePayload(entry));
        return placeholders;
    }

    private static String summarisePayload(AuditEntry entry) {
        if (entry.payload() == null || entry.payload().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> e : entry.payload().entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    private static String lookupName(UUID id) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(id);
        String name = player.getName();
        return name == null ? id.toString().substring(0, 8) : name;
    }
}
