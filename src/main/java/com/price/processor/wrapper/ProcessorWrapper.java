package com.price.processor.wrapper;

import com.price.PriceRateHolder;
import com.price.processor.PriceProcessor;

public class ProcessorWrapper implements Runnable {
    private final String ccyPair;
    private final PriceProcessor delegate;
    private final PriceRateHolder rateHolder;

    public ProcessorWrapper(String ccyPair, PriceProcessor delegate, PriceRateHolder rateHolder) {
        this.ccyPair = ccyPair;
        this.delegate = delegate;
        this.rateHolder = rateHolder;
    }

    @Override
    public void run() {
        delegate.onPrice(ccyPair, rateHolder.getAndSetRate(Double.NaN));
    }

    public PriceProcessor getDelegate() {
        return delegate;
    }
}
