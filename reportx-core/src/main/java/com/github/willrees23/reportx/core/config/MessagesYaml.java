package com.github.willrees23.reportx.core.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record MessagesYaml(String prefix, Map<String, Object> tree) {

    public Optional<String> get(String dottedKey) {
        Objects.requireNonNull(dottedKey, "dottedKey");
        String[] parts = dottedKey.split("\\.");
        Object cursor = tree;
        for (String part : parts) {
            if (!(cursor instanceof Map<?, ?> map)) {
                return Optional.empty();
            }
            cursor = map.get(part);
            if (cursor == null) {
                return Optional.empty();
            }
        }
        return cursor instanceof String s ? Optional.of(s) : Optional.empty();
    }

    public static MessagesYaml empty() {
        return new MessagesYaml("", new LinkedHashMap<>());
    }
}
