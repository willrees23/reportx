package com.github.willrees23.reportx.messaging.local;

import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.core.messaging.Subscription;
import com.github.willrees23.reportx.core.messaging.events.ReportXEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class LocalMessageBus implements MessageBus {

    private static final Logger LOG = LoggerFactory.getLogger(LocalMessageBus.class);

    private final Map<Class<? extends ReportXEvent>, CopyOnWriteArrayList<Consumer<? extends ReportXEvent>>> subscribers
            = new ConcurrentHashMap<>();

    @Override
    public void publish(ReportXEvent event) {
        if (event == null) {
            return;
        }
        for (Map.Entry<Class<? extends ReportXEvent>, CopyOnWriteArrayList<Consumer<? extends ReportXEvent>>> entry
                : subscribers.entrySet()) {
            if (!entry.getKey().isInstance(event)) {
                continue;
            }
            for (Consumer<? extends ReportXEvent> handler : entry.getValue()) {
                dispatch(handler, event);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends ReportXEvent> void dispatch(Consumer<? extends ReportXEvent> handler, ReportXEvent event) {
        try {
            ((Consumer<E>) handler).accept((E) event);
        } catch (RuntimeException ex) {
            LOG.warn("Subscriber threw while handling {}", event.getClass().getSimpleName(), ex);
        }
    }

    @Override
    public <E extends ReportXEvent> Subscription subscribe(Class<E> eventType, Consumer<E> handler) {
        CopyOnWriteArrayList<Consumer<? extends ReportXEvent>> handlers
                = subscribers.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>());
        handlers.add(handler);
        return () -> {
            List<Consumer<? extends ReportXEvent>> existing = subscribers.get(eventType);
            if (existing != null) {
                existing.remove(handler);
            }
        };
    }
}
