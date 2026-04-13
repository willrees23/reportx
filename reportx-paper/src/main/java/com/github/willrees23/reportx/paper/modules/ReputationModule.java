package com.github.willrees23.reportx.paper.modules;

import com.github.willrees23.reportx.core.storage.ReportRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.reportx.paper.reputation.ReputationService;
import com.github.willrees23.solo.module.Module;
import com.github.willrees23.solo.registry.ServiceRegistry;

import java.util.List;

public final class ReputationModule extends Module {

    private ReputationService service;

    @Override
    public String getName() {
        return "Reputation";
    }

    @Override
    public List<String> getDependencies() {
        return List.of("Storage");
    }

    @Override
    protected void onEnable() {
        ServiceRegistry registry = getContext().getServiceRegistry();
        ReportRepository reports = registry.require(ReportRepository.class);
        this.service = new ReputationService(reports, Clock.system());

        registry.register(ReputationService.class, service);
        registry.register(ReputationModule.class, this);
    }

    public ReputationService service() {
        return service;
    }
}
