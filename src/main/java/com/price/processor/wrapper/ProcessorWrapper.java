package com.price.processor.wrapper;

import com.price.processor.PriceProcessor;
import com.price.processor.PriceRateHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProcessorWrapper implements Runnable {
    private final String ccyPair;
    private final PriceProcessor processor;
    private final PriceRateHolder rateHolder;
    private final Supplier<ExecutorService> executorServiceToBeUsed;
    private final Consumer<Long> processingTimeConsumer;

    public ProcessorWrapper(String ccyPair, PriceProcessor processor, PriceRateHolder rateHolder,
                            Supplier<ExecutorService> executorServiceToBeUsed, Consumer<Long> processingTimeConsumer) {
        this.ccyPair = ccyPair;
        this.processor = processor;
        this.rateHolder = rateHolder;
        this.executorServiceToBeUsed = executorServiceToBeUsed;
        this.processingTimeConsumer = processingTimeConsumer;
    }

    @Override
    public void run() {
        double rate = rateHolder.getRate();
        Instant start = Instant.now();
        processor.onPrice(ccyPair, rate);
        processingTimeConsumer.accept(ChronoUnit.MILLIS.between(start, Instant.now()));
        if (!rateHolder.compareAndSet(rate, Double.NaN)) {
            executorServiceToBeUsed.get().submit(this);
        }
    }
}
