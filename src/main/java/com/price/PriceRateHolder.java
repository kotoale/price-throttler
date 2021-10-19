package com.price;

import java.util.concurrent.atomic.AtomicLong;

public class PriceRateHolder {
    private final AtomicLong bits = new AtomicLong(Double.doubleToLongBits(Double.NaN));

    public double getAndSetRate(double rate) {
        return Double.longBitsToDouble(bits.getAndSet(Double.doubleToLongBits(rate)));
    }
}
