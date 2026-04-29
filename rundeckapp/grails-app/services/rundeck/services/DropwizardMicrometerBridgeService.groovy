package rundeck.services

import com.codahale.metrics.Counter
import com.codahale.metrics.Histogram
import com.codahale.metrics.Meter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.MetricRegistryListener
import com.codahale.metrics.Snapshot
import com.codahale.metrics.Timer
import grails.core.GrailsApplication
import grails.events.annotation.Subscriber
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry

import java.util.concurrent.ConcurrentHashMap
import java.util.function.ToDoubleFunction
/**
 * Bridges Dropwizard MetricRegistry to Micrometer MeterRegistry.
 *
 * This allows business metrics registered via Dropwizard (throughout Rundeck codebase)
 * to be exposed through Spring Boot Actuator endpoints like /monitoring/prometheus.
 */
@Slf4j
@CompileStatic
class DropwizardMicrometerBridgeService {

    MetricRegistry metricRegistry
    ConfigurationService configurationService
    GrailsApplication grailsApplication

    // Cache snapshots to avoid recomputing during Prometheus scrapes
    // Key: metric object identity, Value: cached snapshot with timestamp
    private static final ConcurrentHashMap<Object, CachedSnapshot> snapshotCache = new ConcurrentHashMap<>()
    private static final long SNAPSHOT_CACHE_TTL_MS = 1000L // 1 second

    /**
     * Wrapper to cache Timer/Histogram snapshots to avoid redundant computations during scrapes.
     * Each timer/histogram registers 4-5 gauges (mean, p50, p95, p99, etc). Without caching,
     * Prometheus scrape triggers 4-5 snapshot computations per metric. With caching, only 1 per second.
     */
    private static class CachedSnapshot {
        final Snapshot snapshot
        final long timestamp

        CachedSnapshot(Snapshot snapshot, long timestamp) {
            this.snapshot = snapshot
            this.timestamp = timestamp
        }

        boolean isExpired(long now) {
            return (now - timestamp) > SNAPSHOT_CACHE_TTL_MS
        }
    }

    private static Snapshot getCachedSnapshot(Object metricObject, Closure<Snapshot> snapshotSupplier) {
        long now = System.currentTimeMillis()
        CachedSnapshot cached = snapshotCache.get(metricObject)

        if (cached != null && !cached.isExpired(now)) {
            return cached.snapshot
        }

        Snapshot freshSnapshot = snapshotSupplier.call()
        snapshotCache.put(metricObject, new CachedSnapshot(freshSnapshot, now))
        return freshSnapshot
    }

    // Helper methods to create ToDoubleFunction for each metric type
    private static ToDoubleFunction<com.codahale.metrics.Gauge> gaugeValueExtractor() {
        return new ToDoubleFunction<com.codahale.metrics.Gauge>() {
            @Override
            double applyAsDouble(com.codahale.metrics.Gauge g) {
                Object value = g.value
                if (value instanceof Number) {
                    return ((Number) value).doubleValue()
                }
                return 0.0d
            }
        }
    }

    private static ToDoubleFunction<Counter> counterValueExtractor() {
        return new ToDoubleFunction<Counter>() {
            @Override
            double applyAsDouble(Counter c) {
                return c.count as double
            }
        }
    }

    private static ToDoubleFunction<Meter> meterCountExtractor() {
        return new ToDoubleFunction<Meter>() {
            @Override
            double applyAsDouble(Meter m) {
                return m.count as double
            }
        }
    }

    private static ToDoubleFunction<Meter> meterRateExtractor() {
        return new ToDoubleFunction<Meter>() {
            @Override
            double applyAsDouble(Meter m) {
                return m.meanRate
            }
        }
    }

    private static ToDoubleFunction<Timer> timerCountExtractor() {
        return new ToDoubleFunction<Timer>() {
            @Override
            double applyAsDouble(Timer t) {
                return t.count as double
            }
        }
    }

    private static ToDoubleFunction<Timer> timerMeanExtractor() {
        return new ToDoubleFunction<Timer>() {
            @Override
            double applyAsDouble(Timer t) {
                Snapshot snapshot = getCachedSnapshot(t, { -> t.snapshot })
                return snapshot.mean / 1_000_000.0 // ns to ms
            }
        }
    }

    private static ToDoubleFunction<Timer> timerP50Extractor() {
        return new ToDoubleFunction<Timer>() {
            @Override
            double applyAsDouble(Timer t) {
                Snapshot snapshot = getCachedSnapshot(t, { -> t.snapshot })
                return snapshot.median / 1_000_000.0 // ns to ms
            }
        }
    }

    private static ToDoubleFunction<Timer> timerP95Extractor() {
        return new ToDoubleFunction<Timer>() {
            @Override
            double applyAsDouble(Timer t) {
                Snapshot snapshot = getCachedSnapshot(t, { -> t.snapshot })
                return snapshot.get95thPercentile() / 1_000_000.0 // ns to ms
            }
        }
    }

    private static ToDoubleFunction<Timer> timerP99Extractor() {
        return new ToDoubleFunction<Timer>() {
            @Override
            double applyAsDouble(Timer t) {
                Snapshot snapshot = getCachedSnapshot(t, { -> t.snapshot })
                return snapshot.get99thPercentile() / 1_000_000.0 // ns to ms
            }
        }
    }

    private static ToDoubleFunction<Histogram> histogramCountExtractor() {
        return new ToDoubleFunction<Histogram>() {
            @Override
            double applyAsDouble(Histogram h) {
                return h.count as double
            }
        }
    }

    private static ToDoubleFunction<Histogram> histogramMeanExtractor() {
        return new ToDoubleFunction<Histogram>() {
            @Override
            double applyAsDouble(Histogram h) {
                Snapshot snapshot = getCachedSnapshot(h, { -> h.snapshot })
                return snapshot.mean
            }
        }
    }

    private static ToDoubleFunction<Histogram> histogramP50Extractor() {
        return new ToDoubleFunction<Histogram>() {
            @Override
            double applyAsDouble(Histogram h) {
                Snapshot snapshot = getCachedSnapshot(h, { -> h.snapshot })
                return snapshot.median
            }
        }
    }

    private static ToDoubleFunction<Histogram> histogramP95Extractor() {
        return new ToDoubleFunction<Histogram>() {
            @Override
            double applyAsDouble(Histogram h) {
                Snapshot snapshot = getCachedSnapshot(h, { -> h.snapshot })
                return snapshot.get95thPercentile()
            }
        }
    }

    private static ToDoubleFunction<Histogram> histogramP99Extractor() {
        return new ToDoubleFunction<Histogram>() {
            @Override
            double applyAsDouble(Histogram h) {
                Snapshot snapshot = getCachedSnapshot(h, { -> h.snapshot })
                return snapshot.get99thPercentile()
            }
        }
    }

    @Subscriber("rundeck.bootstrap")
    void initialize() {
        boolean enabled = configurationService.getBoolean(
            'metrics.micrometer.bridge.enabled',
            true
        )

        if (!enabled) {
            log.info("Dropwizard->Micrometer bridge disabled")
            return
        }

        log.info("=== Initializing Dropwizard->Micrometer metrics bridge ===")
        log.info("MetricRegistry: ${metricRegistry?.class?.name}")

        if (!metricRegistry) {
            log.warn("Cannot initialize bridge: metricRegistry is null")
            return
        }

        // Get the composite MeterRegistry from Spring Boot's auto-configuration
        MeterRegistry meterRegistry = null
        try {
            meterRegistry = grailsApplication.mainContext.getBean(MeterRegistry)
            log.info("Found MeterRegistry: ${meterRegistry?.class?.name}")
        } catch (Exception e) {
            log.error("Cannot find MeterRegistry bean", e)
            return
        }

        if (!meterRegistry) {
            log.warn("Cannot initialize bridge: meterRegistry is null")
            return
        }

        // Count existing Dropwizard metrics
        int gaugeCount = metricRegistry.gauges.size()
        int counterCount = metricRegistry.counters.size()
        int meterCount = metricRegistry.meters.size()
        int timerCount = metricRegistry.timers.size()
        int histogramCount = metricRegistry.histograms.size()
        int totalMetrics = gaugeCount + counterCount + meterCount + timerCount + histogramCount

        log.info("Dropwizard metrics found: ${totalMetrics} total")
        log.info("  - ${gaugeCount} gauges")
        log.info("  - ${counterCount} counters")
        log.info("  - ${meterCount} meters")
        log.info("  - ${timerCount} timers")
        log.info("  - ${histogramCount} histograms")

        // Bridge Dropwizard metrics to Micrometer by registering them directly
        int bridged = 0

        // Register Dropwizard Gauges as Micrometer Gauges
        metricRegistry.gauges.each { String name, com.codahale.metrics.Gauge gauge ->
            try {
                Gauge.builder(name, gauge, gaugeValueExtractor()).register(meterRegistry)
                bridged++
            } catch (Exception e) {
                log.debug("Failed to bridge gauge ${name}: ${e.message}")
            }
        }

        // Register Dropwizard Counters as Micrometer Gauges (Dropwizard counters are not monotonic)
        metricRegistry.counters.each { String name, Counter counter ->
            try {
                Gauge.builder(name, counter, counterValueExtractor()).register(meterRegistry)
                bridged++
            } catch (Exception e) {
                log.debug("Failed to bridge counter ${name}: ${e.message}")
            }
        }

        // Register Dropwizard Meters as Micrometer Gauges (count and rates)
        metricRegistry.meters.each { String name, Meter meter ->
            try {
                Gauge.builder("${name}.count", meter, meterCountExtractor()).register(meterRegistry)
                Gauge.builder("${name}.rate.mean", meter, meterRateExtractor()).register(meterRegistry)
                if (name.contains('execution')) {
                    log.info("✅ Bridged execution meter: ${name} → ${name}.count (${meter.count})")
                }
                bridged += 2
            } catch (Exception e) {
                log.warn("❌ Failed to bridge meter ${name}: ${e.message}", e)
            }
        }

        // Register Dropwizard Timers as Micrometer Gauges
        metricRegistry.timers.each { String name, Timer timer ->
            try {
                Gauge.builder("${name}.count", timer, timerCountExtractor()).register(meterRegistry)
                Gauge.builder("${name}.mean", timer, timerMeanExtractor()).register(meterRegistry)
                Gauge.builder("${name}.50thpercentile", timer, timerP50Extractor()).register(meterRegistry)
                Gauge.builder("${name}.95thpercentile", timer, timerP95Extractor()).register(meterRegistry)
                Gauge.builder("${name}.99thpercentile", timer, timerP99Extractor()).register(meterRegistry)
                bridged += 5
            } catch (Exception e) {
                log.debug("Failed to bridge timer ${name}: ${e.message}")
            }
        }

        // Register Dropwizard Histograms as Micrometer Gauges
        metricRegistry.histograms.each { String name, Histogram histogram ->
            try {
                Gauge.builder("${name}.count", histogram, histogramCountExtractor()).register(meterRegistry)
                Gauge.builder("${name}.mean", histogram, histogramMeanExtractor()).register(meterRegistry)
                Gauge.builder("${name}.50thpercentile", histogram, histogramP50Extractor()).register(meterRegistry)
                Gauge.builder("${name}.95thpercentile", histogram, histogramP95Extractor()).register(meterRegistry)
                Gauge.builder("${name}.99thpercentile", histogram, histogramP99Extractor()).register(meterRegistry)
                bridged += 5
            } catch (Exception e) {
                log.debug("Failed to bridge histogram ${name}: ${e.message}")
            }
        }

        // Add listener to bridge new metrics dynamically
        metricRegistry.addListener(new MetricRegistryListener() {
            @Override
            void onGaugeAdded(String name, com.codahale.metrics.Gauge<?> gauge) {
                try {
                    Gauge.builder(name, gauge, gaugeValueExtractor()).register(meterRegistry)
                    log.debug("Dynamically bridged gauge: ${name}")
                } catch (Exception e) {
                    log.debug("Failed to bridge gauge ${name}: ${e.message}")
                }
            }

            @Override
            void onGaugeRemoved(String name) {
                log.debug("Gauge removed from Dropwizard: ${name}")
            }

            @Override
            void onCounterAdded(String name, Counter counter) {
                try {
                    Gauge.builder(name, counter, counterValueExtractor()).register(meterRegistry)
                    log.debug("Dynamically bridged counter: ${name}")
                } catch (Exception e) {
                    log.debug("Failed to bridge counter ${name}: ${e.message}")
                }
            }

            @Override
            void onCounterRemoved(String name) {
                log.debug("Counter removed from Dropwizard: ${name}")
            }

            @Override
            void onHistogramAdded(String name, Histogram histogram) {
                try {
                    Gauge.builder("${name}.count", histogram, histogramCountExtractor()).register(meterRegistry)
                    Gauge.builder("${name}.mean", histogram, histogramMeanExtractor()).register(meterRegistry)
                    Gauge.builder("${name}.50thpercentile", histogram, histogramP50Extractor()).register(meterRegistry)
                    Gauge.builder("${name}.95thpercentile", histogram, histogramP95Extractor()).register(meterRegistry)
                    Gauge.builder("${name}.99thpercentile", histogram, histogramP99Extractor()).register(meterRegistry)
                    log.debug("Dynamically bridged histogram: ${name} with percentiles")
                } catch (Exception e) {
                    log.debug("Failed to bridge histogram ${name}: ${e.message}")
                }
            }

            @Override
            void onHistogramRemoved(String name) {
                log.debug("Histogram removed from Dropwizard: ${name}")
            }

            @Override
            void onMeterAdded(String name, Meter meter) {
                try {
                    // Try to register count
                    try {
                        Gauge.builder("${name}.count", meter, meterCountExtractor()).register(meterRegistry)
                    } catch (IllegalArgumentException iae) {
                        // Already registered, that's okay
                        log.debug("Meter ${name}.count already registered")
                    }

                    // Try to register rate
                    try {
                        Gauge.builder("${name}.rate.mean", meter, meterRateExtractor()).register(meterRegistry)
                    } catch (IllegalArgumentException iae) {
                        // Already registered, that's okay
                        log.debug("Meter ${name}.rate.mean already registered")
                    }

                    log.info("✅ Dynamically bridged meter: ${name} → ${name}.count, ${name}.rate.mean")
                } catch (Exception e) {
                    log.warn("❌ Failed to bridge meter ${name}: ${e.message}", e)
                }
            }

            @Override
            void onMeterRemoved(String name) {
                log.debug("Meter removed from Dropwizard: ${name}")
            }

            @Override
            void onTimerAdded(String name, Timer timer) {
                try {
                    Gauge.builder("${name}.count", timer, timerCountExtractor()).register(meterRegistry)
                    Gauge.builder("${name}.mean", timer, timerMeanExtractor()).register(meterRegistry)
                    Gauge.builder("${name}.50thpercentile", timer, timerP50Extractor()).register(meterRegistry)
                    Gauge.builder("${name}.95thpercentile", timer, timerP95Extractor()).register(meterRegistry)
                    Gauge.builder("${name}.99thpercentile", timer, timerP99Extractor()).register(meterRegistry)
                    log.info("Dynamically bridged timer: ${name} with percentiles")
                } catch (Exception e) {
                    log.debug("Failed to bridge timer ${name}: ${e.message}")
                }
            }

            @Override
            void onTimerRemoved(String name) {
                log.debug("Timer removed from Dropwizard: ${name}")
            }
        })

        // Log execution-related meters for debugging
        def executionMeters = metricRegistry.meters.keySet().findAll { it.contains('execution') }
        if (executionMeters) {
            log.info("📊 Found ${executionMeters.size()} execution meters:")
            executionMeters.each { name ->
                log.info("   - ${name}")
            }
        } else {
            log.warn("⚠️  No execution meters found in MetricRegistry yet!")
            log.warn("   Execution meters will be bridged dynamically when jobs run")
        }

        log.info("=== Dropwizard->Micrometer bridge initialized successfully ===")
        log.info("Bridged ${bridged} existing metrics to Micrometer")
        log.info("Listener registered to bridge new metrics dynamically")
        log.info("Metrics will be available at /monitoring/prometheus")
    }
}
