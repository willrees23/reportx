package com.github.willrees23.reportx.core.messaging;

import com.github.willrees23.reportx.core.messaging.events.ReportXEvent;

import java.util.function.Consumer;

public interface MessageBus {

    void publish(ReportXEvent event);

    <E extends ReportXEvent> Subscription subscribe(Class<E> eventType, Consumer<E> handler);
}
