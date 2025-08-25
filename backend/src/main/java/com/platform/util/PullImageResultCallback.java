package com.platform.util;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.PullResponseItem;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class PullImageResultCallback implements ResultCallback<PullResponseItem> {
    
    private CountDownLatch latch = new CountDownLatch(1);
    private boolean completed = false;
    private Throwable error;

    @Override
    public void onStart(Closeable closeable) {
        // Implementation can be empty
    }

    @Override
    public void onNext(PullResponseItem item) {
        // Log progress if needed
        if (item.getStatus() != null) {
            System.out.println("Pull progress: " + item.getStatus());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        this.error = throwable;
        completed = true;
        latch.countDown();
    }

    @Override
    public void onComplete() {
        completed = true;
        latch.countDown();
    }

    @Override
    public void close() throws IOException {
        // Clean up if needed
    }

    public PullImageResultCallback awaitCompletion() throws InterruptedException {
        latch.await();
        if (error != null) {
            throw new RuntimeException("Image pull failed", error);
        }
        return this;
    }

    public boolean isCompleted() {
        return completed;
    }
}