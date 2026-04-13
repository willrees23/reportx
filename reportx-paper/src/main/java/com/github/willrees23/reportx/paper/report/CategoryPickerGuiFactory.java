package com.github.willrees23.reportx.paper.report;

import com.github.willrees23.reportx.core.config.CategoriesYaml;
import com.github.willrees23.reportx.core.config.ConfigYaml;
import com.github.willrees23.reportx.core.config.GuiYaml;
import com.github.willrees23.reportx.paper.text.Items;
import com.github.willrees23.reportx.paper.text.Text;
import com.github.willrees23.solo.gui.Gui;
import com.github.willrees23.solo.gui.GuiButton;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class CategoryPickerGuiFactory {

    private CategoryPickerGuiFactory() {
    }

    public static Gui buildPlayerPicker(GuiYaml guiConfig,
                                        CategoriesYaml categories,
                                        ConfigYaml.ClickSoundConfig clickSound,
                                        Consumer<String> onCategoryClick) {
        Map<String, Object> spec = guiConfig.gui("category-picker-player")
                .orElseThrow(() -> new IllegalStateException("gui.yml missing category-picker-player"));

        String title = stringOr(spec, "title", "<dark_gray>Report a Player");
        int rows = intOr(spec, "rows", 3);
        List<Integer> categorySlots = readSlots(spec.get("category-slots"));

        Gui.Builder builder = Gui.builder()
                .title(Text.legacy(title))
                .rows(rows);

        List<CategoriesYaml.Category> categoryList = categories.categories();
        for (int i = 0; i < categoryList.size() && i < categorySlots.size(); i++) {
            CategoriesYaml.Category category = categoryList.get(i);
            int slot = categorySlots.get(i);
            builder.button(slot, GuiButton.of(
                    Items.build(category.icon(), category.display(), category.lore()),
                    event -> {
                        playClick(event.getWhoClicked(), clickSound);
                        event.getView().close();
                        onCategoryClick.accept(category.id());
                    }));
        }

        applyFiller(builder, spec.get("filler"));
        applyCloseButton(builder, spec.get("close-button"), clickSound);

        return builder.build();
    }

    private static void applyFiller(Gui.Builder builder, Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return;
        }
        String material = stringOr(map, "material", "GRAY_STAINED_GLASS_PANE");
        String display = stringOr(map, "display", " ");
        builder.filler(GuiButton.filler(Items.build(material, display, List.of())));
    }

    private static void applyCloseButton(Gui.Builder builder, Object raw, ConfigYaml.ClickSoundConfig clickSound) {
        if (!(raw instanceof Map<?, ?> map)) {
            return;
        }
        Object slotObj = map.get("slot");
        if (!(slotObj instanceof Number slot)) {
            return;
        }
        String material = stringOr(map, "material", "BARRIER");
        String display = stringOr(map, "display", "<red>Close");
        builder.button(slot.intValue(), GuiButton.of(
                Items.build(material, display, List.of()),
                event -> {
                    playClick(event.getWhoClicked(), clickSound);
                    event.getView().close();
                }));
    }

    private static void playClick(Object viewer, ConfigYaml.ClickSoundConfig clickSound) {
        if (clickSound == null || !clickSound.enabled() || !(viewer instanceof Player player)) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(clickSound.sound());
            player.playSound(player.getLocation(), sound,
                    (float) clickSound.volume(), (float) clickSound.pitch());
        } catch (IllegalArgumentException ignored) {
            // Unknown sound name in config; skip silently.
        }
    }

    private static List<Integer> readSlots(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Integer> out = new ArrayList<>(list.size());
        for (Object element : list) {
            if (element instanceof Number n) {
                out.add(n.intValue());
            }
        }
        return out;
    }

    private static String stringOr(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : Objects.toString(value);
    }

    private static int intOr(Map<?, ?> map, String key, int fallback) {
        Object value = map.get(key);
        return value instanceof Number n ? n.intValue() : fallback;
    }
}
