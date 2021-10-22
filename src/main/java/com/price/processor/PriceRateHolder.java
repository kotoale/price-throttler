package com.price.processor;

import java.util.concurrent.atomic.AtomicLong;

public class PriceRateHolder {
    private final AtomicLong bits = new AtomicLong(Double.doubleToLongBits(Double.NaN));

    public double getRate() {
        return Double.longBitsToDouble(bits.get());
    }

    public double getAndSetRate(double rate) {
        return Double.longBitsToDouble(bits.getAndSet(Double.doubleToLongBits(rate)));
    }

    public boolean compareAndSet(double expectedValue, double newValue) {
        return bits.compareAndSet(Double.doubleToLongBits(expectedValue), Double.doubleToLongBits(newValue));
    }
}
