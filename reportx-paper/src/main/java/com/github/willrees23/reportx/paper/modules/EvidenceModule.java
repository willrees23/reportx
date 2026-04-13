package com.github.willrees23.reportx.paper.modules;

import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.core.storage.EvidenceRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.reportx.paper.evidence.EvidenceService;
import com.github.willrees23.solo.module.Module;
import com.github.willrees23.solo.registry.ServiceRegistry;

import java.util.List;

public final class EvidenceModule extends Module {

    private EvidenceService service;

    @Override
    public String getName() {
        return "Evidence";
    }

    @Override
    public List<String> getDependencies() {
        return List.of("Storage", "Messaging");
    }

    @Override
    protected void onEnable() {
        ServiceRegistry registry = getContext().getServiceRegistry();
        EvidenceRepository repo = registry.require(EvidenceRepository.class);
        MessageBus bus = registry.require(MessageBus.class);

        this.service = new EvidenceService(repo, bus, Clock.system());

        registry.register(EvidenceService.class, service);
        registry.register(EvidenceModule.class, this);
    }

    public EvidenceService service() {
        return service;
    }
}
