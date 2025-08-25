// src/main/java/com/platform/util/LogContainerResultCallback.java
package com.platform.util;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LogContainerResultCallback implements ResultCallback<Frame> {
    
    private final List<String> logs = new ArrayList<>();
    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void onStart(Closeable closeable) {
        // Implementation can be empty
    }

    @Override
    public void onNext(Frame frame) {
        if (frame != null && frame.getPayload() != null) {
            String log = new String(frame.getPayload()).trim();
            if (!log.isEmpty()) {
                logs.add(log);
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        latch.countDown();
    }

    @Override
    public void onComplete() {
        latch.countDown();
    }

    @Override
    public void close() throws IOException {
        // Clean up if needed
    }

    public LogContainerResultCallback awaitCompletion() throws InterruptedException {
        latch.await();
        return this;
    }

    public List<String> getLogs() {
        return new ArrayList<>(logs);
    }
    
    @Override
    public String toString() {
        return String.join("\n", logs);
    }
}