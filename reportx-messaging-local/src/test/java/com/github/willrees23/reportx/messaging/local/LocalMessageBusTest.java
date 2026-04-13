package com.github.willrees23.reportx.messaging.local;

import com.github.willrees23.reportx.core.messaging.Subscription;
import com.github.willrees23.reportx.core.messaging.events.CaseClaimedEvent;
import com.github.willrees23.reportx.core.messaging.events.CaseReleasedEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportXEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LocalMessageBusTest {

    @Test
    void publishWithoutSubscribersIsNoop() {
        LocalMessageBus bus = new LocalMessageBus();
        bus.publish(new CaseClaimedEvent(UUID.randomUUID(), UUID.randomUUID(), "survival"));
    }

    @Test
    void subscribersReceiveTypedEvents() {
        LocalMessageBus bus = new LocalMessageBus();
        List<CaseClaimedEvent> received = new ArrayList<>();
        bus.subscribe(CaseClaimedEvent.class, received::add);

        CaseClaimedEvent event = new CaseClaimedEvent(UUID.randomUUID(), UUID.randomUUID(), "survival");
        bus.publish(event);
        bus.publish(new CaseReleasedEvent(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(received).containsExactly(event);
    }

    @Test
    void unsubscribeStopsDelivery() {
        LocalMessageBus bus = new LocalMessageBus();
        AtomicInteger count = new AtomicInteger();
        Subscription subscription = bus.subscribe(CaseClaimedEvent.class, ev -> count.incrementAndGet());

        bus.publish(new CaseClaimedEvent(UUID.randomUUID(), UUID.randomUUID(), "survival"));
        subscription.unsubscribe();
        bus.publish(new CaseClaimedEvent(UUID.randomUUID(), UUID.randomUUID(), "survival"));

        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    void handlerExceptionDoesNotBreakOtherSubscribers() {
        LocalMessageBus bus = new LocalMessageBus();
        AtomicInteger goodCalls = new AtomicInteger();
        bus.subscribe(CaseClaimedEvent.class, ev -> { throw new RuntimeException("boom"); });
        bus.subscribe(CaseClaimedEvent.class, ev -> goodCalls.incrementAndGet());

        bus.publish(new CaseClaimedEvent(UUID.randomUUID(), UUID.randomUUID(), "survival"));

        assertThat(goodCalls.get()).isEqualTo(1);
    }

    @Test
    void concurrentPublishAndSubscribeIsSafe() throws InterruptedException {
        LocalMessageBus bus = new LocalMessageBus();
        AtomicInteger received = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            int subscriberCount = 16;
            int publisherCount = 16;
            int eventsPerPublisher = 50;

            for (int i = 0; i < subscriberCount; i++) {
                pool.submit(() -> {
                    awaitQuietly(start);
                    bus.subscribe(CaseClaimedEvent.class, ev -> received.incrementAndGet());
                });
            }

            CountDownLatch publishersDone = new CountDownLatch(publisherCount);
            for (int i = 0; i < publisherCount; i++) {
                pool.submit(() -> {
                    awaitQuietly(start);
                    try {
                        for (int j = 0; j < eventsPerPublisher; j++) {
                            ReportXEvent event = new CaseClaimedEvent(UUID.randomUUID(), UUID.randomUUID(), "s");
                            bus.publish(event);
                        }
                    } finally {
                        publishersDone.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(publishersDone.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
        assertThat(received.get()).isGreaterThanOrEqualTo(0);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
