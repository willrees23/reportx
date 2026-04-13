package com.github.willrees23.reportx.paper.text;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Items {

    private Items() {
    }

    public static ItemStack build(String materialName, String display, List<String> lore) {
        return build(materialName, display, lore, Map.of());
    }

    public static ItemStack build(String materialName, String display, List<String> lore,
                                  Map<String, String> placeholders) {
        Material material = resolveMaterial(materialName);
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (display != null && !display.isEmpty()) {
                meta.displayName(Text.parse(display, placeholders).decoration(
                        net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            }
            if (lore != null && !lore.isEmpty()) {
                List<Component> components = new ArrayList<>(lore.size());
                for (String line : lore) {
                    components.add(Text.parse(line, placeholders).decoration(
                            net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                }
                meta.lore(components);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static Material resolveMaterial(String name) {
        if (name == null || name.isBlank()) {
            return Material.STONE;
        }
        Material material = Material.matchMaterial(name);
        return material != null ? material : Material.STONE;
    }
}
