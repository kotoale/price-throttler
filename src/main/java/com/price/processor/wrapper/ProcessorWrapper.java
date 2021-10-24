package com.price.processor.wrapper;

import com.price.processor.PriceEventHolder;
import com.price.processor.PriceProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProcessorWrapper implements Runnable {
    private final String ccyPair;
    private final PriceProcessor processor;
    private final PriceEventHolder rateHolder;
    private final Supplier<ExecutorService> executorServiceToBeUsed;
    private final Consumer<Long> processingTimeConsumer;

    public ProcessorWrapper(String ccyPair, PriceProcessor processor, PriceEventHolder rateHolder,
                            Supplier<ExecutorService> executorServiceToBeUsed, Consumer<Long> processingTimeConsumer) {
        this.ccyPair = ccyPair;
        this.processor = processor;
        this.rateHolder = rateHolder;
        this.executorServiceToBeUsed = executorServiceToBeUsed;
        this.processingTimeConsumer = processingTimeConsumer;
    }

    @Override
    public void run() {
        final var event = rateHolder.getEvent();
        Instant start = Instant.now();
        processor.onPrice(ccyPair, event.getRate());
        processingTimeConsumer.accept(ChronoUnit.MILLIS.between(start, Instant.now()));
        if (!rateHolder.compareAndSet(event, null)) {
            executorServiceToBeUsed.get().submit(this);
        }
    }
}
