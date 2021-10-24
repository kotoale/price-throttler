package com.price.processor;

import java.util.concurrent.atomic.AtomicReference;

public class PriceRateHolder {
    private final AtomicReference<Double> rate = new AtomicReference<>();

    public boolean setAndCheckIfItWasNull(double newValue) {
        return rate.getAndSet(newValue) == null;
    }

    public Double getRate() {
        return rate.get();
    }

    public boolean compareAndSetNull(Double expectedValue) {
        return rate.compareAndSet(expectedValue, null);
    }
}
