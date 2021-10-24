package com.price.processor;

import com.price.processor.wrapper.ProcessorWrapper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

public class CcyPairUpdatesProcessor {
    private final String ccyPair;
    private final long taskDurationThresholdInMillis;
    private final ConcurrentMap<PriceProcessor, ExecutorService> processorToExecutorService;
    private final ExecutorService slowProcessorsExecutorService;
    private final ExecutorService fastProcessorsExecutorService;
    private final ConcurrentHashMap<PriceProcessor, PriceEventHolder> processorToEventHolder = new ConcurrentHashMap<>();

    public CcyPairUpdatesProcessor(String ccyPair, ConcurrentMap<PriceProcessor, ExecutorService> processorToExecutorService,
                                   ExecutorService slowProcessorsExecutorService, ExecutorService fastProcessorsExecutorService,
                                   long taskDurationThresholdInMillis) {
        this.ccyPair = ccyPair;
        this.processorToExecutorService = processorToExecutorService;
        this.slowProcessorsExecutorService = slowProcessorsExecutorService;
        this.fastProcessorsExecutorService = fastProcessorsExecutorService;
        this.taskDurationThresholdInMillis = taskDurationThresholdInMillis;
    }

    public void onPrice(double rate) {
        processorToEventHolder.forEach((processor, priceEventHolder) -> {
            if (priceEventHolder.getAndSetRate(rate) == null) {
                final var executorServiceToBeUsed = executorServiceToBeUsed(processor);
                final var task = new ProcessorWrapper(ccyPair, processor, priceEventHolder,
                        () -> executorServiceToBeUsed(processor), (time) -> calculateExecutorService(processor, time));
                executorServiceToBeUsed.submit(task);
            }
        });
    }

    private ExecutorService executorServiceToBeUsed(PriceProcessor priceProcessor) {
        final var executorServiceToBeUsed = processorToExecutorService.get(priceProcessor);
        return executorServiceToBeUsed == null ? slowProcessorsExecutorService : executorServiceToBeUsed;
    }

    private void calculateExecutorService(PriceProcessor processor, long taskDurationInMillis) {
        processorToExecutorService.put(processor,
                taskDurationInMillis <= taskDurationThresholdInMillis
                        ? fastProcessorsExecutorService
                        : slowProcessorsExecutorService
        );
    }

    public void subscribe(PriceProcessor priceProcessor) {
        processorToEventHolder.computeIfAbsent(priceProcessor, key -> new PriceEventHolder());
    }

    public void unsubscribe(PriceProcessor priceProcessor) {
        processorToEventHolder.remove(priceProcessor);
    }
}
