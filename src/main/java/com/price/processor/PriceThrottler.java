package com.price.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PriceThrottler implements PriceProcessor {
    private static final Logger log = LoggerFactory.getLogger(PriceThrottler.class);

    private static final int PROCESSORS_MAX_COUNT = 200;
    private static final int CC_PAIRS_MAX_COUNT = 200;

    private final ExecutorService priceTickHandler = Executors.newSingleThreadExecutor();
    private final Set<PriceProcessor> registeredProcessors = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, CcyPairUpdatesProcessor> currencyToUpdatesProcessor = new ConcurrentHashMap<>();
    private final ConcurrentMap<PriceProcessor, ExecutorService> processorToExecutorService = new ConcurrentHashMap<>();
    private final ExecutorService slowProcessorsExecutorService = Executors.newFixedThreadPool(12);
    private final ExecutorService fastProcessorsExecutorService = Executors.newFixedThreadPool(12);
    private final long taskDurationThresholdInMillis;

    public PriceThrottler(long taskDurationThresholdInMillis) {
        this.taskDurationThresholdInMillis = taskDurationThresholdInMillis;
    }

    @Override
    public void onPrice(String ccyPair, double rate) {
        priceTickHandler.execute(() -> {
            final var updatesProcessor = currencyToUpdatesProcessor.computeIfAbsent(
                    ccyPair,
                    (key) -> {
                        if (currencyToUpdatesProcessor.size() >= CC_PAIRS_MAX_COUNT) {
                            log.error("Skip update for [ccPair, rate]: [{}, {}]. The ccPairs limit is reached: {}",
                                    key, rate, CC_PAIRS_MAX_COUNT);
                            return null;
                        }

                        final var processor = new CcyPairUpdatesProcessor(key, processorToExecutorService,
                                slowProcessorsExecutorService, fastProcessorsExecutorService,
                                taskDurationThresholdInMillis);
                        registeredProcessors.forEach(processor::subscribe);
                        return processor;
                    }
            );
            if (updatesProcessor != null) {
                updatesProcessor.onPrice(rate);
            }
        });
    }

    @Override
    public void subscribe(PriceProcessor priceProcessor) {
        if (registeredProcessors.size() >= PROCESSORS_MAX_COUNT) {
            String errorMsg = "Can't subscribe one more processor. The processors limit is reached: " + PROCESSORS_MAX_COUNT;
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        registeredProcessors.add(priceProcessor);
        currencyToUpdatesProcessor.forEach((currency, updatesProcessor) -> updatesProcessor.subscribe(priceProcessor));
    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {
        registeredProcessors.remove(priceProcessor);
        currencyToUpdatesProcessor.forEach((currency, updatesProcessor) -> updatesProcessor.unsubscribe(priceProcessor));
        processorToExecutorService.remove(priceProcessor);
    }

    //Visible for testing
    Set<PriceProcessor> getRegisteredProcessors() {
        return registeredProcessors;
    }

    //Visible for testing
    ConcurrentMap<String, CcyPairUpdatesProcessor> getCurrencyToUpdatesProcessor() {
        return currencyToUpdatesProcessor;
    }

    //Visible for testing
    ConcurrentMap<PriceProcessor, ExecutorService> getProcessorToExecutorService() {
        return processorToExecutorService;
    }
}
