package com.github.willrees23.reportx.paper.staff.gui;

import com.github.willrees23.reportx.core.config.GuiYaml;
import com.github.willrees23.reportx.core.model.Note;
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

public final class NotesListGuiFactory {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private static final int[] DEFAULT_CONTENT_SLOTS = new int[]{
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private NotesListGuiFactory() {
    }

    public static Gui build(GuiYaml guiConfig,
                            UUID caseId,
                            List<Note> entries,
                            Consumer<Note> onClickEdit,
                            Consumer<Note> onShiftClickDelete,
                            Runnable onBack) {
        Map<String, Object> spec = guiConfig.gui("notes-list")
                .orElseThrow(() -> new IllegalStateException("gui.yml missing notes-list"));

        Map<String, String> titlePlaceholders = new HashMap<>();
        titlePlaceholders.put("id", Ids.shortCaseId(caseId));
        String title = Text.legacy(GuiSpecReader.stringOr(spec, "title",
                "<dark_gray>Notes <gray>— <yellow>Case #{id}"), titlePlaceholders);
        int rows = GuiSpecReader.intOr(spec, "rows", 6);
        int[] contentSlots = AuditLogGuiFactory.toIntArray(
                GuiSpecReader.intList(spec.get("content-slots")), DEFAULT_CONTENT_SLOTS);

        Map<String, Object> entrySpec = GuiSpecReader.subMap(spec, "entry");
        String entryMaterial = entrySpec == null ? "PAPER"
                : GuiSpecReader.stringOr(entrySpec, "material", "PAPER");
        String entryDisplay = entrySpec == null ? "<yellow>Note by {author}"
                : GuiSpecReader.stringOr(entrySpec, "display", "<yellow>Note by {author}");
        List<String> entryLore = entrySpec == null ? List.of()
                : GuiSpecReader.stringList(entrySpec.get("lore"));

        List<GuiButton> items = new ArrayList<>(entries.size());
        for (Note note : entries) {
            Map<String, String> placeholders = entryPlaceholders(note);
            items.add(GuiButton.of(
                    Items.build(entryMaterial, entryDisplay, entryLore, placeholders),
                    event -> {
                        if (event.isShiftClick()) {
                            if (onShiftClickDelete != null) {
                                onShiftClickDelete.accept(note);
                            }
                        } else if (onClickEdit != null) {
                            onClickEdit.accept(note);
                        }
                    }));
        }

        Gui.Builder builder = Gui.builder()
                .title(title)
                .rows(rows)
                .paginated(items, contentSlots.length, contentSlots);

        AuditLogGuiFactory.applyBackButton(builder, spec, onBack);
        builder.filler(GuiSpecReader.fillerFromSpec(spec, "BLACK_STAINED_GLASS_PANE"));
        return builder.build();
    }

    private static Map<String, String> entryPlaceholders(Note note) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("author", lookupName(note.authorId()));
        placeholders.put("body", note.body() == null ? "" : note.body());
        placeholders.put("created", TIMESTAMP.format(note.createdAt()));
        placeholders.put("edited", note.editedAt() == null ? "—" : TIMESTAMP.format(note.editedAt()));
        return placeholders;
    }

    private static String lookupName(UUID id) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(id);
        String name = player.getName();
        return name == null ? id.toString().substring(0, 8) : name;
    }
}
