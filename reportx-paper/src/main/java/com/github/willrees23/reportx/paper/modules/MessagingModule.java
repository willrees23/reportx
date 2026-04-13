package com.github.willrees23.reportx.paper.modules;

import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.paper.messaging.MessagingFactory;
import com.github.willrees23.solo.module.Module;
import com.github.willrees23.solo.registry.ServiceRegistry;

import java.util.List;

public final class MessagingModule extends Module {

    private MessageBus bus;

    @Override
    public String getName() {
        return "Messaging";
    }

    @Override
    public List<String> getDependencies() {
        return List.of("Config");
    }

    @Override
    protected void onEnable() {
        ServiceRegistry registry = getContext().getServiceRegistry();
        ConfigModule config = registry.require(ConfigModule.class);

        this.bus = MessagingFactory.create(config.snapshot().messaging());
        registry.register(MessageBus.class, bus);
        registry.register(MessagingModule.class, this);
    }

    public MessageBus bus() {
        return bus;
    }
}
