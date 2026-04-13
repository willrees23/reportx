package com.github.willrees23.reportx.paper.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.willrees23.reportx.core.config.CategoriesYaml;
import com.github.willrees23.reportx.core.config.ConfigYaml;
import com.github.willrees23.reportx.core.config.GuiYaml;
import com.github.willrees23.reportx.core.config.MessagesYaml;
import com.github.willrees23.reportx.core.config.MessagingYaml;
import com.github.willrees23.reportx.core.config.ReportXConfig;
import com.github.willrees23.reportx.core.config.ReputationYaml;
import com.github.willrees23.reportx.core.config.StorageYaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigLoader {

    private static final String DEFAULTS_RESOURCE_PREFIX = "defaults/";

    private final Path dataDirectory;
    private final ObjectMapper yaml;
    private final ClassLoader resourceLoader;

    public ConfigLoader(Path dataDirectory, ClassLoader resourceLoader) {
        this.dataDirectory = dataDirectory;
        this.resourceLoader = resourceLoader;
        this.yaml = new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public ReportXConfig load() {
        try {
            ensureDataDirectory();
            return new ReportXConfig(
                    readTyped("config.yml", ConfigYaml.class),
                    readTyped("storage.yml", StorageYaml.class),
                    readTyped("messaging.yml", MessagingYaml.class),
                    readTyped("categories.yml", CategoriesYaml.class),
                    readTyped("reputation.yml", ReputationYaml.class),
                    readGui("gui.yml"),
                    readMessages("messages.yml")
            );
        } catch (IOException ex) {
            throw new ConfigLoadException("Failed to load ReportX configuration", ex);
        }
    }

    private void ensureDataDirectory() throws IOException {
        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory);
        }
    }

    private <T> T readTyped(String fileName, Class<T> type) throws IOException {
        Path file = ensureFile(fileName);
        return yaml.readValue(file.toFile(), type);
    }

    private GuiYaml readGui(String fileName) throws IOException {
        Path file = ensureFile(fileName);
        Map<String, Map<String, Object>> map = yaml.readValue(
                file.toFile(),
                yaml.getTypeFactory().constructMapType(LinkedHashMap.class,
                        yaml.getTypeFactory().constructType(String.class),
                        yaml.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class)));
        return new GuiYaml(map);
    }

    private MessagesYaml readMessages(String fileName) throws IOException {
        Path file = ensureFile(fileName);
        Map<String, Object> map = yaml.readValue(
                file.toFile(),
                yaml.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
        Object prefix = map.remove("prefix");
        return new MessagesYaml(prefix == null ? "" : prefix.toString(), map);
    }

    private Path ensureFile(String fileName) throws IOException {
        Path target = dataDirectory.resolve(fileName);
        if (Files.exists(target)) {
            return target;
        }
        try (InputStream resource = resourceLoader.getResourceAsStream(DEFAULTS_RESOURCE_PREFIX + fileName)) {
            if (resource == null) {
                throw new ConfigLoadException("Bundled default missing for " + fileName);
            }
            Files.copy(resource, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    public Path dataDirectory() {
        return dataDirectory;
    }
}
