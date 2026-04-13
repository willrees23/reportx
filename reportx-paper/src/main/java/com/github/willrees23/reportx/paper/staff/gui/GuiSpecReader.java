package com.github.willrees23.reportx.paper.staff.gui;

import com.github.willrees23.reportx.paper.text.Items;
import com.github.willrees23.solo.gui.GuiButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GuiSpecReader {

    private GuiSpecReader() {
    }

    public static String stringOr(Map<?, ?> map, String key, String fallback) {
        if (map == null) return fallback;
        Object value = map.get(key);
        return value == null ? fallback : Objects.toString(value);
    }

    public static int intOr(Map<?, ?> map, String key, int fallback) {
        if (map == null) return fallback;
        Object value = map.get(key);
        return value instanceof Number n ? n.intValue() : fallback;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> subMap(Map<?, ?> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    public static List<Integer> intList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<Integer> out = new ArrayList<>(list.size());
        for (Object element : list) {
            if (element instanceof Number n) {
                out.add(n.intValue());
            }
        }
        return out;
    }

    public static List<String> stringList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>(list.size());
        for (Object element : list) {
            if (element != null) {
                out.add(element.toString());
            }
        }
        return out;
    }

    public static GuiButton fillerFromSpec(Map<String, Object> spec, String fallbackMaterial) {
        Map<String, Object> fillerSpec = subMap(spec, "filler");
        if (fillerSpec == null) {
            return GuiButton.filler(Items.build(fallbackMaterial, " ", List.of()));
        }
        String material = stringOr(fillerSpec, "material", fallbackMaterial);
        String display = stringOr(fillerSpec, "display", " ");
        return GuiButton.filler(Items.build(material, display, List.of()));
    }

    public static GuiButton borderFromSpec(Map<String, Object> spec, String fallbackMaterial) {
        Map<String, Object> borderSpec = subMap(spec, "border");
        if (borderSpec == null) {
            return null;
        }
        String material = stringOr(borderSpec, "material", fallbackMaterial);
        String display = stringOr(borderSpec, "display", " ");
        return GuiButton.filler(Items.build(material, display, List.of()));
    }
}
