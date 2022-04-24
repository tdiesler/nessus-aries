package io.nessus.aries.common.websocket;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Flow.Subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSubscriber<T> implements Flow.Subscriber<T> {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final CountDownLatch completeLatch = new CountDownLatch(1);
    protected Subscription subscription;
    
    public Subscription getSubscription() {
        return subscription;
    }

    public void cancelSubscription() {
        subscription.cancel();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        log.debug("onSubscribe");
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(T item) {
        log.debug("onNext: {}", item);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable th) {
        log.error("onError", th);
    }

    @Override
    public void onComplete() {
        log.debug("onComplete");
        completeLatch.countDown();
    }
    
    public boolean awaitComplete(long timeout, TimeUnit unit) throws InterruptedException {
        return completeLatch.await(timeout, unit);
    }
}