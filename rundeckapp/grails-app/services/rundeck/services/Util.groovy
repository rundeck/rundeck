package rundeck.services

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import com.google.common.cache.Cache

/**
 * Created by greg on 7/30/15.
 */
class Util {

    static void addCacheMetrics(String name, MetricRegistry registry,Cache sourceCache) {
        registry?.register(
                MetricRegistry.name(name, "hitCount"),
                new Gauge<Long>() {
                    @Override
                    Long getValue() {
                        sourceCache.stats().hitCount()
                    }
                }
        )

        registry?.register(
                MetricRegistry.name(name, "evictionCount"),
                new Gauge<Long>() {
                    @Override
                    Long getValue() {
                        sourceCache.stats().evictionCount()
                    }
                }
        )
        registry?.register(
                MetricRegistry.name(name, "missCount"),
                new Gauge<Long>() {
                    @Override
                    Long getValue() {
                        sourceCache.stats().missCount()
                    }
                }
        )
        registry?.register(
                MetricRegistry.name(name, "loadExceptionCount"),
                new Gauge<Long>() {
                    @Override
                    Long getValue() {
                        sourceCache.stats().loadExceptionCount()
                    }
                }
        )
        registry?.register(
                MetricRegistry.name(name, "hitRate"),
                new Gauge<Double>() {
                    @Override
                    Double getValue() {
                        sourceCache.stats().hitRate()
                    }
                }
        )
    }
}
