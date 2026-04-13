package com.github.willrees23.reportx.paper.text;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TextTest {

    @Test
    void parseMiniMessageProducesComponent() {
        Component component = Text.parse("<red>hello");

        assertThat(component).isNotNull();
        assertThat(Text.legacy(component)).contains("hello");
    }

    @Test
    void parseLegacyAmpersandIsConvertedToMiniMessage() {
        Component component = Text.parse("&chello &lworld");

        // Round-trip through legacy serializer to confirm formatting survived.
        String legacy = Text.legacy(component);
        assertThat(legacy).contains("hello");
        assertThat(legacy).contains("world");
        assertThat(legacy).contains("\u00a7");
    }

    @Test
    void applyPlaceholders_substitutesAllOccurrences() {
        String result = Text.applyPlaceholders("Welcome {player}, your id is {id}",
                Map.of("player", "Notch", "id", "abc123"));

        assertThat(result).isEqualTo("Welcome Notch, your id is abc123");
    }

    @Test
    void parseWithPlaceholders_appliesBeforeFormatting() {
        Component component = Text.parse("<yellow>Welcome {player}</yellow>",
                Map.of("player", "Notch"));

        assertThat(Text.legacy(component)).contains("Welcome Notch");
    }

    @Test
    void parseEmptyOrNullProducesEmptyComponent() {
        assertThat(Text.parse(null)).isEqualTo(Component.empty());
        assertThat(Text.parse("")).isEqualTo(Component.empty());
    }

    @Test
    void applyPlaceholders_nullValueSubstitutesEmptyString() {
        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("name", null);

        String result = Text.applyPlaceholders("hi {name}", placeholders);

        assertThat(result).isEqualTo("hi ");
    }
}
