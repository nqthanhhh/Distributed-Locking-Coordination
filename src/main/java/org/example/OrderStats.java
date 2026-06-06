package org.example;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderStats {

    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failCount = new AtomicInteger();

    public void incrementSuccess() {
        successCount.incrementAndGet();
    }

    public void incrementFail() {
        failCount.incrementAndGet();
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public int getFailCount() {
        return failCount.get();
    }
}
