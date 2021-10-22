package com.price.processor;

import java.util.Objects;

public class SlowTestProcessor extends AbstractTestProcessor {

    private static final long LONG_PROCESSING_TIME = 3000;

    private final int id;

    public SlowTestProcessor(int id) {
        this.id = id;
    }

    @Override
    protected long getProcessingTimeInMillis() {
        return LONG_PROCESSING_TIME;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlowTestProcessor that = (SlowTestProcessor) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SlowTestProcessor{" +
                "id=" + id +
                '}';
    }
}
