package rundeck.services

import com.codahale.metrics.Counter
import com.codahale.metrics.Histogram
import com.codahale.metrics.Meter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class DropwizardMicrometerBridgeServiceSpec extends Specification {

    DropwizardMicrometerBridgeService service
    MetricRegistry metricRegistry
    MeterRegistry meterRegistry
    ConfigurationService configurationService

    def setup() {
        metricRegistry = new MetricRegistry()
        meterRegistry = new SimpleMeterRegistry()
        configurationService = Mock(ConfigurationService)

        service = new DropwizardMicrometerBridgeService()
        service.metricRegistry = metricRegistry
        service.meterRegistry = meterRegistry
        service.configurationService = configurationService
    }

    void setupBridgeEnabled() {
        configurationService.getBoolean('metrics.micrometer.bridge.enabled', true) >> true
    }

    def "bridge is disabled when configuration is false"() {
        given:
        configurationService.getBoolean('metrics.micrometer.bridge.enabled', true) >> false

        when:
        service.initialize()

        then:
        meterRegistry.meters.size() == 0
    }

    def "bridge registers existing Dropwizard Gauge to Micrometer"() {
        given:
        setupBridgeEnabled()

        // Register a Dropwizard gauge before initializing the bridge
        metricRegistry.register("test.gauge", new com.codahale.metrics.Gauge<Integer>() {
            @Override
            Integer getValue() {
                return 42
            }
        })

        when:
        service.initialize()

        then:
        def micrometerGauge = meterRegistry.get("test.gauge").gauge()
        micrometerGauge.value() == 42.0d
    }

    def "bridge registers existing Dropwizard Counter to Micrometer"() {
        given:
        setupBridgeEnabled()

        Counter counter = metricRegistry.counter("test.counter")
        counter.inc(10)

        when:
        service.initialize()

        then:
        def micrometerGauge = meterRegistry.get("test.counter").gauge()
        micrometerGauge.value() == 10.0d
    }

    def "bridge registers existing Dropwizard Meter with count and rate"() {
        given:
        setupBridgeEnabled()

        Meter meter = metricRegistry.meter("test.meter")
        meter.mark(5)

        when:
        service.initialize()

        then:
        def countGauge = meterRegistry.get("test.meter.count").gauge()
        countGauge.value() == 5.0d

        and:
        def rateGauge = meterRegistry.get("test.meter.rate.mean").gauge()
        rateGauge.value() >= 0.0d  // Rate will be positive
    }

    def "bridge registers existing Dropwizard Timer with count, mean, and percentiles"() {
        given:
        setupBridgeEnabled()

        Timer timer = metricRegistry.timer("test.timer")
        timer.update(100, TimeUnit.MILLISECONDS)
        timer.update(200, TimeUnit.MILLISECONDS)

        when:
        service.initialize()

        then:
        def countGauge = meterRegistry.get("test.timer.count").gauge()
        countGauge.value() == 2.0d

        and:
        def meanGauge = meterRegistry.get("test.timer.mean").gauge()
        meanGauge.value() > 0.0d

        and:
        meterRegistry.get("test.timer.50thpercentile").gauge()
        meterRegistry.get("test.timer.95thpercentile").gauge()
        meterRegistry.get("test.timer.99thpercentile").gauge()
    }

    def "bridge registers existing Dropwizard Histogram with count, mean, and percentiles"() {
        given:
        setupBridgeEnabled()

        Histogram histogram = metricRegistry.histogram("test.histogram")
        histogram.update(50)
        histogram.update(100)
        histogram.update(150)

        when:
        service.initialize()

        then:
        def countGauge = meterRegistry.get("test.histogram.count").gauge()
        countGauge.value() == 3.0d

        and:
        def meanGauge = meterRegistry.get("test.histogram.mean").gauge()
        meanGauge.value() == 100.0d

        and:
        meterRegistry.get("test.histogram.50thpercentile").gauge()
        meterRegistry.get("test.histogram.95thpercentile").gauge()
        meterRegistry.get("test.histogram.99thpercentile").gauge()
    }

    def "bridge dynamically registers new Dropwizard Gauge added after initialization"() {
        given:
        setupBridgeEnabled()

        when:
        service.initialize()

        and: "register a new gauge after the bridge is initialized"
        metricRegistry.register("dynamic.gauge", new com.codahale.metrics.Gauge<Integer>() {
            @Override
            Integer getValue() {
                return 99
            }
        })

        then:
        def micrometerGauge = meterRegistry.get("dynamic.gauge").gauge()
        micrometerGauge.value() == 99.0d
    }

    def "bridge dynamically registers new Dropwizard Meter added after initialization"() {
        given:
        setupBridgeEnabled()

        when:
        service.initialize()

        and: "register a new meter after the bridge is initialized"
        Meter meter = metricRegistry.meter("dynamic.meter")
        meter.mark(7)

        then:
        def countGauge = meterRegistry.get("dynamic.meter.count").gauge()
        countGauge.value() == 7.0d

        and:
        meterRegistry.get("dynamic.meter.rate.mean").gauge()
    }

    def "bridge dynamically registers new Dropwizard Timer added after initialization"() {
        given:
        setupBridgeEnabled()

        when:
        service.initialize()

        and: "register a new timer after the bridge is initialized"
        Timer timer = metricRegistry.timer("dynamic.timer")
        timer.update(50, TimeUnit.MILLISECONDS)

        then:
        def countGauge = meterRegistry.get("dynamic.timer.count").gauge()
        countGauge.value() == 1.0d

        and:
        meterRegistry.get("dynamic.timer.mean").gauge()
        meterRegistry.get("dynamic.timer.50thpercentile").gauge()
        meterRegistry.get("dynamic.timer.95thpercentile").gauge()
        meterRegistry.get("dynamic.timer.99thpercentile").gauge()
    }

    def "bridge handles Gauge with non-numeric value gracefully"() {
        given:
        setupBridgeEnabled()

        metricRegistry.register("test.string.gauge", new com.codahale.metrics.Gauge<String>() {
            @Override
            String getValue() {
                return "not a number"
            }
        })

        when:
        service.initialize()

        then:
        def micrometerGauge = meterRegistry.get("test.string.gauge").gauge()
        micrometerGauge.value() == 0.0d  // Should default to 0.0 for non-numeric values
    }

    def "bridge registers multiple metric types in a single initialization"() {
        given:
        setupBridgeEnabled()

        // Register one of each type
        metricRegistry.register("test.gauge", new com.codahale.metrics.Gauge<Integer>() {
            @Override
            Integer getValue() { return 1 }
        })
        metricRegistry.counter("test.counter").inc()
        metricRegistry.meter("test.meter").mark()
        metricRegistry.timer("test.timer").update(10, TimeUnit.MILLISECONDS)
        metricRegistry.histogram("test.histogram").update(5)

        when:
        service.initialize()

        then:
        meterRegistry.get("test.gauge").gauge()
        meterRegistry.get("test.counter").gauge()
        meterRegistry.get("test.meter.count").gauge()
        meterRegistry.get("test.timer.count").gauge()
        meterRegistry.get("test.histogram.count").gauge()

        and: "should have bridged multiple metrics per type"
        meterRegistry.meters.size() > 5  // At least 5 base metrics + percentiles
    }
}
