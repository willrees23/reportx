package com.github.willrees23.reportx.paper.staff.gui;

import com.github.willrees23.reportx.core.config.CategoriesYaml;
import com.github.willrees23.reportx.core.config.GuiYaml;
import com.github.willrees23.reportx.paper.text.Items;
import com.github.willrees23.reportx.paper.text.Text;
import com.github.willrees23.solo.gui.Gui;
import com.github.willrees23.solo.gui.GuiButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class StaffCategoryPickerGuiFactory {

    private StaffCategoryPickerGuiFactory() {
    }

    public static Gui build(GuiYaml guiConfig,
                            CategoriesYaml categories,
                            Function<String, Integer> unhandledCountByCategory,
                            Consumer<String> onCategoryClick) {
        Map<String, Object> spec = guiConfig.gui("category-picker-staff")
                .orElseThrow(() -> new IllegalStateException("gui.yml missing category-picker-staff"));

        String title = Text.legacy(GuiSpecReader.stringOr(spec, "title", "<dark_gray>Handle a Report"));
        int rows = GuiSpecReader.intOr(spec, "rows", 3);
        List<Integer> slots = GuiSpecReader.intList(spec.get("category-slots"));
        List<String> appendLore = GuiSpecReader.stringList(spec.get("item-lore-append"));

        Gui.Builder builder = Gui.builder().title(title).rows(rows);

        List<CategoriesYaml.Category> categoryList = categories.categories();
        for (int i = 0; i < categoryList.size() && i < slots.size(); i++) {
            CategoriesYaml.Category category = categoryList.get(i);
            int slot = slots.get(i);
            int count = unhandledCountByCategory.apply(category.id());

            List<String> lore = new ArrayList<>(category.lore());
            for (String line : appendLore) {
                lore.add(line);
            }
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("category_id", category.id());
            placeholders.put("unhandled_count", String.valueOf(count));

            builder.button(slot, GuiButton.of(
                    Items.build(category.icon(), category.display(), lore, placeholders),
                    event -> {
                        event.getView().close();
                        onCategoryClick.accept(category.id());
                    }));
        }

        Map<String, Object> closeSpec = GuiSpecReader.subMap(spec, "close-button");
        if (closeSpec != null) {
            int slot = GuiSpecReader.intOr(closeSpec, "slot", -1);
            if (slot >= 0) {
                String material = GuiSpecReader.stringOr(closeSpec, "material", "BARRIER");
                String display = GuiSpecReader.stringOr(closeSpec, "display", "<red>Close");
                builder.button(slot, GuiButton.of(
                        Items.build(material, display, List.of()),
                        event -> event.getView().close()));
            }
        }

        builder.filler(GuiSpecReader.fillerFromSpec(spec, "BLACK_STAINED_GLASS_PANE"));
        return builder.build();
    }
}
