package com.price.processor;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class PriceEventHolder {
    private final AtomicReference<Event> event = new AtomicReference<>();
    private final AtomicLong counter = new AtomicLong(0);

    public Event getEvent() {
        return event.get();
    }

    public Event getAndSetRate(double rate) {
        return event.getAndSet(new Event(counter.incrementAndGet(), rate));
    }

    public boolean compareAndSet(Event expectedValue, Event newValue) {
        return event.compareAndSet(expectedValue, newValue);
    }

    public static class Event {
        private final long id;
        private final double rate;

        public Event(long id, double rate) {
            this.id = id;
            this.rate = rate;
        }

        public double getRate() {
            return rate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Event event = (Event) o;
            return id == event.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
