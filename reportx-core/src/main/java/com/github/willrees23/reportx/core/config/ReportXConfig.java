package com.github.willrees23.reportx.core.config;

public record ReportXConfig(
        ConfigYaml config,
        StorageYaml storage,
        MessagingYaml messaging,
        CategoriesYaml categories,
        ReputationYaml reputation,
        GuiYaml gui,
        MessagesYaml messages
) {
}
