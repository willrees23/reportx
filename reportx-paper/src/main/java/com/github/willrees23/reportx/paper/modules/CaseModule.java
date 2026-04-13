package com.github.willrees23.reportx.paper.modules;

import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.core.storage.CaseRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.reportx.paper.cases.CaseService;
import com.github.willrees23.solo.module.Module;
import com.github.willrees23.solo.registry.ServiceRegistry;

import java.util.List;

public final class CaseModule extends Module {

    private CaseService service;

    @Override
    public String getName() {
        return "Case";
    }

    @Override
    public List<String> getDependencies() {
        return List.of("Storage", "Messaging");
    }

    @Override
    protected void onEnable() {
        ServiceRegistry registry = getContext().getServiceRegistry();
        CaseRepository repo = registry.require(CaseRepository.class);
        MessageBus bus = registry.require(MessageBus.class);

        this.service = new CaseService(repo, bus, Clock.system());

        registry.register(CaseService.class, service);
        registry.register(CaseModule.class, this);
    }

    public CaseService service() {
        return service;
    }
}
