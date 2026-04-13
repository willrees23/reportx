package com.github.willrees23.reportx.core.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record GuiYaml(Map<String, Map<String, Object>> guis) {

    public Optional<Map<String, Object>> gui(String id) {
        return Optional.ofNullable(guis.get(id));
    }

    public static GuiYaml empty() {
        return new GuiYaml(new LinkedHashMap<>());
    }
}
