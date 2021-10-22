package com.price.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public abstract class AbstractTestProcessor implements PriceProcessor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void onPrice(String ccyPair, double rate) {
        try {
            TimeUnit.MILLISECONDS.sleep(getProcessingTimeInMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("onPrice [ccyPair, rate]: [{}, {}] has been interrupted for {}", ccyPair, rate, this, ie);
        }
        log.info("Price update [ccyPair, rate]: [{}, {}] successfully handled by {}", ccyPair, rate, this);
    }

    @Override
    public void subscribe(PriceProcessor priceProcessor) {
    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {
    }

    protected abstract long getProcessingTimeInMillis();

}
