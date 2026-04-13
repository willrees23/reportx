package com.github.willrees23.reportx.paper.modules;

import com.github.willrees23.reportx.core.audit.AuditEventTranslator;
import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.core.messaging.Subscription;
import com.github.willrees23.reportx.core.messaging.events.ReportXEvent;
import com.github.willrees23.reportx.core.storage.AuditRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.solo.module.Module;
import com.github.willrees23.solo.registry.ServiceRegistry;

import java.util.List;

public final class AuditModule extends Module {

    private Subscription subscription;

    @Override
    public String getName() {
        return "Audit";
    }

    @Override
    public List<String> getDependencies() {
        return List.of("Storage", "Messaging");
    }

    @Override
    protected void onEnable() {
        ServiceRegistry registry = getContext().getServiceRegistry();
        AuditRepository repo = registry.require(AuditRepository.class);
        MessageBus bus = registry.require(MessageBus.class);

        AuditEventTranslator translator = new AuditEventTranslator(Clock.system());

        this.subscription = bus.subscribe(ReportXEvent.class, event ->
                translator.translate(event).ifPresent(entry -> {
                    try {
                        repo.insert(entry);
                    } catch (RuntimeException ex) {
                        getLogger().warning("Failed to write audit entry for "
                                + event.getClass().getSimpleName() + ": " + ex.getMessage());
                    }
                }));

        registry.register(AuditModule.class, this);
    }

    @Override
    protected void onDisable() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }
}
