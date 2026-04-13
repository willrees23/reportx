package com.github.willrees23.reportx.paper.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;

public final class Text {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMP = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private Text() {
    }

    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        String normalised = normaliseLegacy(raw);
        return MINI.deserialize(normalised);
    }

    public static Component parse(String raw, Map<String, String> placeholders) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        return parse(applyPlaceholders(raw, placeholders));
    }

    public static String legacy(Component component) {
        return LEGACY_SECTION.serialize(component);
    }

    public static String legacy(String raw) {
        return legacy(parse(raw));
    }

    public static String legacy(String raw, Map<String, String> placeholders) {
        return legacy(parse(raw, placeholders));
    }

    public static String applyPlaceholders(String raw, Map<String, String> placeholders) {
        if (raw == null || placeholders == null || placeholders.isEmpty()) {
            return raw;
        }
        String out = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            out = out.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return out;
    }

    private static String normaliseLegacy(String raw) {
        if (raw.indexOf('&') < 0 && raw.indexOf('\u00a7') < 0) {
            return raw;
        }
        // Convert legacy &-codes (and § codes) to MiniMessage tags by round-tripping
        // through Adventure: legacy → Component → MiniMessage string.
        Component fromLegacy = LEGACY_AMP.deserialize(raw.replace('\u00a7', '&'));
        return MINI.serialize(fromLegacy);
    }
}
