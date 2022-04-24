package io.nessus.aries.test;

import java.util.concurrent.Executors;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.common.websocket.EventSubscriber;

public class ConcurrentFlowTest extends AbstractAriesTest {

    static class MyEvent {
        final int count;

        MyEvent(int count) {
            this.count = count;
        }
        
        @Override
        public String toString() {
            return String.format("Event[%02d]", count);
        }
    }
    
    @Test
    public void testWorkflow() throws Exception {
        
        SubmissionPublisher<MyEvent> publisher = new SubmissionPublisher<MyEvent>(Executors.newSingleThreadExecutor(), 10);
        
        Executors.newSingleThreadExecutor(r -> new Thread(r, "ThreadA")).submit(new Runnable() {
            AtomicInteger count = new AtomicInteger();
            @Override
            public void run() {
                String threadName = Thread.currentThread().getName();
                while (count.get() < 10) {
                    MyEvent event = new MyEvent(count.incrementAndGet());
                    log.info("{}: {}", threadName, event);
                    publisher.submit(event);
                    safeSleep(100);
                }
                publisher.close();
            }
        });

        EventSubscriber<MyEvent> subscriber = new EventSubscriber<MyEvent>();
        publisher.subscribe(subscriber);
        
        Assertions.assertTrue(subscriber.awaitComplete(10, TimeUnit.SECONDS), "Timeout awaiting subscriber complete");
    }
    
}
