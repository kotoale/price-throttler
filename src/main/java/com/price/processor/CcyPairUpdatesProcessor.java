package com.price.processor;

import com.price.PriceRateHolder;
import com.price.processor.wrapper.ProcessorWrappersFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

public class CcyPairUpdatesProcessor implements PriceProcessor {
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

    @Override
    public void onPrice(String ccyPair, double rate) {
        onPrice(rate);
    }

    public void onPrice(double rate) {
        processorToRateHolder.forEach((processor, priceRateHolder) -> {
            if (Double.isNaN(priceRateHolder.getAndSetRate(rate))) {
                var executorServiceToBeUsed = processorToExecutorService.get(processor);
                final boolean executorServiceNotSpecifiedForProcessor = executorServiceToBeUsed == null;
                if (executorServiceNotSpecifiedForProcessor) {
                    executorServiceToBeUsed = slowProcessorsExecutorService;
                }
                final var task = ProcessorWrappersFactory.buildProcessorWrapper(ccyPair, processor,
                        priceRateHolder, taskDurationThresholdInMillis, executorServiceNotSpecifiedForProcessor,
                        processorToExecutorService, slowProcessorsExecutorService, fastProcessorsExecutorService);
                executorServiceToBeUsed.submit(task);
            }
        });
    }

    @Override
    public void subscribe(PriceProcessor priceProcessor) {
        processorToRateHolder.computeIfAbsent(priceProcessor, key -> new PriceRateHolder());
    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {
        processorToRateHolder.remove(priceProcessor);
    }
}
