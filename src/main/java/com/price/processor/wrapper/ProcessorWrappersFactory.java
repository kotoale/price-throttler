package com.price.processor.wrapper;

import com.price.PriceRateHolder;
import com.price.processor.PriceProcessor;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

public class ProcessorWrappersFactory {
    public static Runnable buildProcessorWrapper(String ccyPair, PriceProcessor processor,
                                                 PriceRateHolder rateHolder, long taskDurationThresholdInMillis,
                                                 boolean executorServiceNotSpecifiedForProcessor,
                                                 ConcurrentMap<PriceProcessor, ExecutorService> processorToExecutorService,
                                                 ExecutorService slowProcessorsExecutorService,
                                                 ExecutorService fastProcessorsExecutorService) {
        final var wrapper = new ProcessorWrapper(ccyPair, processor, rateHolder);
        return executorServiceNotSpecifiedForProcessor
                ? new ProcessorWrapperWithClassifier(wrapper, processorToExecutorService, slowProcessorsExecutorService,
                fastProcessorsExecutorService, taskDurationThresholdInMillis)
                : wrapper;
    }
}
