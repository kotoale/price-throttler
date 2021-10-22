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
    private final ConcurrentHashMap<PriceProcessor, PriceRateHolder> processorToRateHolder = new ConcurrentHashMap<>();

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
        processorToRateHolder.forEach((processor, priceRateHolder) -> {
            if (Double.isNaN(priceRateHolder.getAndSetRate(rate))) {
                final var executorServiceToBeUsed = executorServiceToBeUsed(processor);
                final var task = new ProcessorWrapper(ccyPair, processor, priceRateHolder,
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
        processorToRateHolder.computeIfAbsent(priceProcessor, key -> new PriceRateHolder());
    }

    public void unsubscribe(PriceProcessor priceProcessor) {
        processorToRateHolder.remove(priceProcessor);
    }
}
