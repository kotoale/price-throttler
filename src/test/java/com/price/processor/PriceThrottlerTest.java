package com.price.processor;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;

class PriceThrottlerTest {
    private static final int CC_PAIRS_MAX_COUNT = 200;
    private static final int PROCESSORS_MAX_COUNT = 200;
    private static final Offset<Double> EPSILON = Offset.offset(0.000001d);

    private PriceThrottler priceThrottler;

    @BeforeEach
    void setup() {
        priceThrottler = new PriceThrottler(50);
    }

    @Test
    void onPrice_should_increase_currency_update_processors() throws Exception {
        ConcurrentMap<String, CcyPairUpdatesProcessor> currencyToUpdatesProcessor = priceThrottler.getCurrencyToUpdatesProcessor();
        assertThat(currencyToUpdatesProcessor).isEmpty();

        for (int i = 0; i < CC_PAIRS_MAX_COUNT; i++) {
            String ccyPair = "TEST_CC_PAIR_" + i;
            priceThrottler.onPrice(ccyPair, ThreadLocalRandom.current().nextDouble(1000.0));

            // sleep is required as long as onPrice processed in a different thread
            TimeUnit.MILLISECONDS.sleep(5);
            assertThat(currencyToUpdatesProcessor).hasSize(i + 1);
            assertThat(currencyToUpdatesProcessor).containsKey(ccyPair);
        }
    }

    @Test
    void onPrice_should_ignore_update() throws Exception {
        ConcurrentMap<String, CcyPairUpdatesProcessor> currencyToUpdatesProcessor = priceThrottler.getCurrencyToUpdatesProcessor();
        assertThat(currencyToUpdatesProcessor).isEmpty();

        IntStream.range(0, CC_PAIRS_MAX_COUNT).forEach((i) -> {
            String ccyPair = "TEST_CC_PAIR_" + i;
            priceThrottler.onPrice(ccyPair, ThreadLocalRandom.current().nextDouble(1000.0));
        });

        // sleep is required as long as onPrice processed in a different thread
        TimeUnit.MILLISECONDS.sleep(5);
        assertThat(currencyToUpdatesProcessor).hasSize(CC_PAIRS_MAX_COUNT);

        String newCcyPair = "TEST_CC_PAIR_" + CC_PAIRS_MAX_COUNT + 1;
        assertThat(currencyToUpdatesProcessor).doesNotContainKey(newCcyPair);
        priceThrottler.onPrice(newCcyPair, ThreadLocalRandom.current().nextDouble(1000.0));
        TimeUnit.MILLISECONDS.sleep(5);
        // check that size has not changed
        assertThat(currencyToUpdatesProcessor).hasSize(CC_PAIRS_MAX_COUNT);
        assertThat(currencyToUpdatesProcessor).doesNotContainKey(newCcyPair);
    }

    @Test
    @DisplayName("Complex test for onPrice() with subscribed processors")
    void onPrice() throws Exception {
        List<FastTestProcessor> fastProcessors = new ArrayList<>();
        List<SlowTestProcessor> slowProcessors = new ArrayList<>();
        IntStream.range(0, 10).forEach(i -> {
            FastTestProcessor priceProcessor = Mockito.spy(new FastTestProcessor(i));
            priceThrottler.subscribe(priceProcessor);
            fastProcessors.add(priceProcessor);
        });
        IntStream.range(0, 5).forEach(i -> {
            SlowTestProcessor priceProcessor = Mockito.spy(new SlowTestProcessor(i));
            priceThrottler.subscribe(priceProcessor);
            slowProcessors.add(priceProcessor);
        });

        assertThat(priceThrottler.getRegisteredProcessors()).hasSize(15);
        ConcurrentMap<PriceProcessor, ExecutorService> processorToExecutorService = priceThrottler.getProcessorToExecutorService();
        assertThat(processorToExecutorService).isEmpty();

        priceThrottler.onPrice("EURUSD", 1.16);

        TimeUnit.MILLISECONDS.sleep(3000);
        assertThat(processorToExecutorService).isNotEmpty();
        fastProcessors.forEach(it -> assertThat(processorToExecutorService).containsKey(it));

        // sleep is required as long as onPrice processed in a different threads
        TimeUnit.MILLISECONDS.sleep(5000);
        assertThat(processorToExecutorService).hasSize(15);
        fastProcessors.forEach(it -> assertThat(processorToExecutorService).containsKey(it));
        slowProcessors.forEach(it -> assertThat(processorToExecutorService).containsKey(it));

        // unsubscribe
        priceThrottler.unsubscribe(slowProcessors.get(0));

        IntStream.range(0, 10000).forEach((i) ->
                priceThrottler.onPrice("EURRUB", i));

        // sleep is required as long as onPrice processed in a different threads
        TimeUnit.MILLISECONDS.sleep(10000);

        fastProcessors.forEach(processor -> {
            ArgumentCaptor<Double> captor = ArgumentCaptor.forClass(Double.class);
            Mockito.verify(processor, Mockito.atLeastOnce()).onPrice(eq("EURRUB"), captor.capture());
            assertThat(captor.getAllValues().get(captor.getAllValues().size() - 1)).isEqualTo(9999.0, EPSILON);
        });

        // for unsubscribed processor
        Mockito.verify(slowProcessors.get(0), Mockito.never()).onPrice(eq("EURRUB"), anyDouble());

        slowProcessors.stream().skip(1).forEach(processor -> {
            ArgumentCaptor<Double> captor = ArgumentCaptor.forClass(Double.class);
            Mockito.verify(processor, Mockito.atLeastOnce()).onPrice(eq("EURRUB"), captor.capture());
            assertThat(captor.getAllValues().get(captor.getAllValues().size() - 1)).isEqualTo(9999.0, EPSILON);
        });
    }

    @Test
    void subscribe_should_add_processor_to_registered() {
        Set<PriceProcessor> registeredProcessors = priceThrottler.getRegisteredProcessors();
        assertThat(registeredProcessors).isEmpty();

        FastTestProcessor fastProcessor = new FastTestProcessor(0);
        priceThrottler.subscribe(fastProcessor);

        assertThat(registeredProcessors).hasSize(1);
        assertThat(registeredProcessors).contains(fastProcessor);

        SlowTestProcessor slowProcessor = new SlowTestProcessor(0);
        priceThrottler.subscribe(slowProcessor);
        FastTestProcessor fastProcessor1 = new FastTestProcessor(1);
        priceThrottler.subscribe(fastProcessor1);

        assertThat(registeredProcessors).hasSize(3);
        assertThat(registeredProcessors).contains(slowProcessor);
        assertThat(registeredProcessors).contains(fastProcessor1);
    }

    @Test
    void subscribe_should_throw_IllegalStateException() {
        Set<PriceProcessor> registeredProcessors = priceThrottler.getRegisteredProcessors();
        IntStream.range(0, PROCESSORS_MAX_COUNT).forEach((i) -> {
            FastTestProcessor processor = new FastTestProcessor(i);
            priceThrottler.subscribe(processor);
            assertThat(registeredProcessors).contains(processor);
            assertThat(registeredProcessors).hasSize(i + 1);
        });

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> priceThrottler.subscribe(new FastTestProcessor(PROCESSORS_MAX_COUNT)));
        assertThat(exception).hasMessage("Can't subscribe one more processor. The processors limit is reached: 200");
        assertThat(registeredProcessors).hasSize(PROCESSORS_MAX_COUNT);
    }

    @Test
    void subscribe_and_unsubscribe() {
        Set<PriceProcessor> registeredProcessors = priceThrottler.getRegisteredProcessors();
        assertThat(registeredProcessors).isEmpty();

        FastTestProcessor fastProcessor = new FastTestProcessor(0);
        // unsubscribe not subscribed
        priceThrottler.unsubscribe(fastProcessor);
        assertThat(registeredProcessors).isEmpty();

        priceThrottler.subscribe(fastProcessor);
        assertThat(registeredProcessors).hasSize(1);
        assertThat(registeredProcessors).contains(fastProcessor);

        SlowTestProcessor slowProcessor = new SlowTestProcessor(0);
        priceThrottler.subscribe(slowProcessor);
        priceThrottler.subscribe(new FastTestProcessor(1));
        assertThat(registeredProcessors).hasSize(3);
        assertThat(registeredProcessors).contains(slowProcessor);

        // unsubscribe
        priceThrottler.unsubscribe(slowProcessor);
        assertThat(registeredProcessors).hasSize(2);
        assertThat(registeredProcessors).doesNotContain(slowProcessor);

        // unsubscribe not subscribed
        priceThrottler.unsubscribe(slowProcessor);
        assertThat(registeredProcessors).hasSize(2);
        assertThat(registeredProcessors).doesNotContain(slowProcessor);

        // subscribe again
        priceThrottler.subscribe(slowProcessor);
        assertThat(registeredProcessors).hasSize(3);
        assertThat(registeredProcessors).contains(slowProcessor);

        // subscribe already subscribed
        priceThrottler.subscribe(fastProcessor);
        assertThat(registeredProcessors).hasSize(3);
    }
}