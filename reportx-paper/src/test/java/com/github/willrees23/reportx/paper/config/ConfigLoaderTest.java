package com.github.willrees23.reportx.paper.config;

import com.github.willrees23.reportx.core.config.ReportXConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigLoaderTest {

    @TempDir
    Path dataDir;

    private ConfigLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ConfigLoader(dataDir, getClass().getClassLoader());
    }

    @Test
    void load_writesDefaultsOnFirstRunAndParsesEverything() {
        ReportXConfig snapshot = loader.load();

        for (String fileName : List.of(
                "config.yml",
                "storage.yml",
                "messaging.yml",
                "categories.yml",
                "reputation.yml",
                "gui.yml",
                "messages.yml")) {
            assertThat(dataDir.resolve(fileName)).exists();
        }

        assertThat(snapshot.config().dedup().enabled()).isTrue();
        assertThat(snapshot.config().dedup().windowSeconds()).isEqualTo(300);
        assertThat(snapshot.config().reports().cooldownSeconds()).isEqualTo(60);
        assertThat(snapshot.config().gui().clickSound().enabled()).isTrue();
        assertThat(snapshot.config().gui().clickSound().sound()).isEqualTo("UI_BUTTON_CLICK");
        assertThat(snapshot.config().handle().emptyQueueRevertSeconds()).isEqualTo(2);
        assertThat(snapshot.config().logs().buffer().chatMaxMessages()).isEqualTo(200);

        assertThat(snapshot.storage().backend()).isEqualTo("sqlite");
        assertThat(snapshot.storage().sqlite().file()).isEqualTo("reports.db");

        assertThat(snapshot.messaging().transport()).isEqualTo("local");

        assertThat(snapshot.categories().categories()).hasSize(3);
        assertThat(snapshot.categories().categories().get(0).id()).isEqualTo("hacking");
        assertThat(snapshot.categories().emptySlot().sound()).isEqualTo("BLOCK_NOTE_BLOCK_BASS");

        assertThat(snapshot.reputation().tiers()).hasSize(5);
        assertThat(snapshot.reputation().tiers().get(0).id()).isEqualTo("outstanding");
        assertThat(snapshot.reputation().decay().halfLifeDays()).isEqualTo(30);

        assertThat(snapshot.gui().gui("category-picker-player")).isPresent();
        assertThat(snapshot.gui().gui("case-file")).isPresent();

        assertThat(snapshot.messages().prefix()).isEqualTo("<gray>[<aqua>ReportX</aqua>]</gray> ");
        assertThat(snapshot.messages().get("report.submitted"))
                .contains("<green>Your report has been submitted. Thank you.");
        assertThat(snapshot.messages().get("staff.case-claimed")).isPresent();
        assertThat(snapshot.messages().get("does.not.exist")).isEmpty();
    }

    @Test
    void load_preservesUserEdits() throws Exception {
        loader.load(); // write defaults

        Path configFile = dataDir.resolve("config.yml");
        String original = Files.readString(configFile);
        String edited = original.replace("cooldown-seconds: 60", "cooldown-seconds: 999");
        Files.writeString(configFile, edited);

        ReportXConfig snapshot = loader.load();
        assertThat(snapshot.config().reports().cooldownSeconds()).isEqualTo(999);
    }

    @Test
    void load_failsClearlyOnInvalidYaml() throws Exception {
        loader.load();
        Path categoriesFile = dataDir.resolve("categories.yml");
        Files.writeString(categoriesFile, "categories: [not-a-mapping");

        try {
            loader.load();
        } catch (ConfigLoadException expected) {
            assertThat(expected).hasMessageContaining("ReportX configuration");
            return;
        }
        org.junit.jupiter.api.Assertions.fail("Expected ConfigLoadException for malformed YAML");
    }
}
