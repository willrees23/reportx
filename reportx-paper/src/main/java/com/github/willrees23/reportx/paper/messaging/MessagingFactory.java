package com.github.willrees23.reportx.paper.messaging;

import com.github.willrees23.reportx.core.config.MessagingYaml;
import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.messaging.local.LocalMessageBus;

import java.util.Locale;

public final class MessagingFactory {

    private MessagingFactory() {
    }

    public static MessageBus create(MessagingYaml config) {
        if (config == null || config.transport() == null) {
            throw new IllegalArgumentException("messaging.yml is missing the 'transport' setting");
        }
        String transport = config.transport().toLowerCase(Locale.ROOT);
        return switch (transport) {
            case "local" -> new LocalMessageBus();
            case "redis" -> throw new UnsupportedOperationException(
                    "Messaging transport 'redis' is not implemented yet (planned for milestone 3)");
            default -> throw new IllegalArgumentException("Unknown messaging transport: " + config.transport());
        };
    }
}
