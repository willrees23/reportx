package com.github.willrees23.reportx.paper.messaging;

import com.github.willrees23.reportx.core.config.MessagingYaml;
import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.messaging.local.LocalMessageBus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessagingFactoryTest {

    @Test
    void create_localTransportReturnsLocalBus() {
        MessagingYaml config = new MessagingYaml("local", null);

        MessageBus bus = MessagingFactory.create(config);

        assertThat(bus).isInstanceOf(LocalMessageBus.class);
    }

    @Test
    void create_localTransportIsCaseInsensitive() {
        MessagingYaml config = new MessagingYaml("LOCAL", null);

        assertThat(MessagingFactory.create(config)).isInstanceOf(LocalMessageBus.class);
    }

    @Test
    void create_redisTransportNotYetSupported() {
        MessagingYaml config = new MessagingYaml("redis", null);

        assertThatThrownBy(() -> MessagingFactory.create(config))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("milestone 3");
    }

    @Test
    void create_unknownTransportIsRejected() {
        MessagingYaml config = new MessagingYaml("nosuch", null);

        assertThatThrownBy(() -> MessagingFactory.create(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown messaging transport");
    }

    @Test
    void create_missingTransportKeyIsRejected() {
        MessagingYaml config = new MessagingYaml(null, null);

        assertThatThrownBy(() -> MessagingFactory.create(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transport");
    }
}
