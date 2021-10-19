package com.price.processor.wrapper;

import com.price.processor.PriceProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

public class ProcessorWrapperWithClassifier implements Runnable {
    private final ProcessorWrapper delegate;
    private final ConcurrentMap<PriceProcessor, ExecutorService> processorToExecutorService;
    private final ExecutorService slowProcessorsExecutorService;
    private final ExecutorService fastProcessorsExecutorService;
    private final long taskDurationThresholdInMillis;

    public ProcessorWrapperWithClassifier(ProcessorWrapper delegate,
                                          ConcurrentMap<PriceProcessor, ExecutorService> processorToExecutorService,
                                          ExecutorService slowProcessorsExecutorService,
                                          ExecutorService fastProcessorsExecutorService,
                                          long taskDurationThresholdInMillis) {
        this.delegate = delegate;
        this.processorToExecutorService = processorToExecutorService;
        this.slowProcessorsExecutorService = slowProcessorsExecutorService;
        this.fastProcessorsExecutorService = fastProcessorsExecutorService;
        this.taskDurationThresholdInMillis = taskDurationThresholdInMillis;
    }

    public void run() {
        final var start = Instant.now();
        delegate.run();
        if (ChronoUnit.MILLIS.between(start, Instant.now()) <= taskDurationThresholdInMillis) {
            processorToExecutorService.put(delegate.getDelegate(), fastProcessorsExecutorService);
        } else {
            processorToExecutorService.put(delegate.getDelegate(), slowProcessorsExecutorService);
        }
    }
}
