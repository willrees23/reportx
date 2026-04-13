package com.github.willrees23.reportx.paper.modules;

import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.core.storage.NoteRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.reportx.paper.notes.NoteService;
import com.github.willrees23.solo.module.Module;
import com.github.willrees23.solo.registry.ServiceRegistry;

import java.util.List;

public final class NoteModule extends Module {

    private NoteService service;

    @Override
    public String getName() {
        return "Note";
    }

    @Override
    public List<String> getDependencies() {
        return List.of("Storage", "Messaging");
    }

    @Override
    protected void onEnable() {
        ServiceRegistry registry = getContext().getServiceRegistry();
        NoteRepository repo = registry.require(NoteRepository.class);
        MessageBus bus = registry.require(MessageBus.class);

        this.service = new NoteService(repo, bus, Clock.system());

        registry.register(NoteService.class, service);
        registry.register(NoteModule.class, this);
    }

    public NoteService service() {
        return service;
    }
}
