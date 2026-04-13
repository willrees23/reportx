package com.github.willrees23.reportx.core.config;

import java.util.List;

public record CategoriesYaml(
        List<Category> categories,
        EmptySlot emptySlot
) {

    public record Category(
            String id,
            String display,
            String icon,
            List<String> lore,
            int priority,
            int evidenceWindowSeconds
    ) {
    }

    public record EmptySlot(
            String icon,
            String display,
            List<String> lore,
            String sound
    ) {
    }
}
