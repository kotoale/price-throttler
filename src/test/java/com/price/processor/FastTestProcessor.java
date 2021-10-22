package com.price.processor;

import java.util.Objects;

public class FastTestProcessor extends AbstractTestProcessor {

    private static final long SHORT_PROCESSING_TIME = 10;

    private final int id;

    public FastTestProcessor(int id) {
        this.id = id;
    }

    @Override
    protected long getProcessingTimeInMillis() {
        return SHORT_PROCESSING_TIME;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FastTestProcessor that = (FastTestProcessor) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FastTestProcessor{" +
                "id=" + id +
                '}';
    }
}
