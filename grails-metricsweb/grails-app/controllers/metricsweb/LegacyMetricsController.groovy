package metricsweb
/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.codahale.metrics.Counter
import com.codahale.metrics.Gauge
import com.codahale.metrics.Histogram
import com.codahale.metrics.Meter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import grails.converters.JSON
import org.springframework.beans.factory.annotation.Autowired

/**
 * Legacy Metrics Endpoint Wrapper (Deprecated)
 * 
 * Provides backward compatibility for Dropwizard Metrics 4.x format.
 * Translates data from MetricRegistry to legacy JSON structure.
 * 
 * DEPRECATED: This endpoint will be removed in Rundeck 7.0.
 * Customers should migrate to /monitoring/prometheus.
 * 
 * Timeline:
 * - Rundeck 6.x: Available via opt-in (deprecated)
 * - Rundeck 7.0: Removed
 */
class LegacyMetricsController {
    
    @Autowired
    MetricRegistry metricRegistry
    
    def metrics() {
        // Check if legacy mode is explicitly enabled (OFF by default)
        def legacyEnabled = grailsApplication.config.getProperty(
            'rundeck.metrics.legacy.enabled', 
            Boolean, 
            false  // Default to FALSE in Rundeck 6.0 - opt-in only
        )
        
        if (!legacyEnabled) {
            response.status = 404  // Not Found
            render([
                error: 'Legacy metrics endpoint is disabled by default',
                enable: 'Set rundeck.metrics.legacy.enabled=true to re-enable',
                migrate: 'Recommended: Use /monitoring/prometheus or /monitoring/metrics',
                docs: 'https://docs.rundeck.com/docs/upgrading/upgrading-to-6.0.html'
            ] as JSON)
            return
        }
        
        // Log deprecation warning when legacy is enabled
        log.warn("DEPRECATED: Legacy /metrics endpoint enabled and accessed. " +
                 "This endpoint will be removed in Rundeck 7.0. " +
                 "Please migrate to /monitoring/prometheus (recommended) or /monitoring/metrics")
        
        // Build Dropwizard-style response
        def output = [
            version: "4.0.0",
            gauges: buildGauges(),
            counters: buildCounters(),
            histograms: buildHistograms(),
            meters: buildMeters(),
            timers: buildTimers()
        ]
        
        render output as JSON
    }
    
    def ping() {
        def legacyEnabled = grailsApplication.config.getProperty(
            'rundeck.metrics.legacy.enabled', 
            Boolean, 
            false  // Default to FALSE - opt-in only
        )
        
        if (!legacyEnabled) {
            response.status = 404  // Not Found
            response.contentType = 'text/plain'
            render "Legacy /metrics/ping endpoint disabled. Enable with rundeck.metrics.legacy.enabled=true or use /monitoring/health"
            return
        }
        
        log.warn("DEPRECATED: Legacy /metrics/ping endpoint accessed. " +
                 "This endpoint will be removed in Rundeck 7.0. " +
                 "Migrate to /monitoring/health")
        
        response.contentType = 'text/plain'
        render "pong"
    }

    def healthcheck() {
        def legacyEnabled = grailsApplication.config.getProperty(
            'rundeck.metrics.legacy.enabled',
            Boolean,
            false
        )

        if (!legacyEnabled) {
            response.status = 404
            render([
                error: 'Legacy /metrics/healthcheck endpoint disabled',
                enable: 'Set rundeck.metrics.legacy.enabled=true to re-enable',
                migrate: 'Recommended: Use /monitoring/health'
            ] as JSON)
            return
        }

        log.warn("DEPRECATED: Legacy /metrics/healthcheck endpoint accessed. " +
                 "This endpoint will be removed in Rundeck 7.0. " +
                 "Migrate to /monitoring/health")

        // Return Dropwizard-style healthcheck
        def output = [:]
        metricRegistry.gauges.each { name, gauge ->
            if (name.contains("healthcheck") || name.contains("health")) {
                output[name] = [healthy: true]
            }
        }
        
        // If no health checks found, return a simple healthy status
        if (output.isEmpty()) {
            output["rundeck"] = [healthy: true]
        }

        render output as JSON
    }

    def threads() {
        def legacyEnabled = grailsApplication.config.getProperty(
            'rundeck.metrics.legacy.enabled',
            Boolean,
            false
        )

        if (!legacyEnabled) {
            response.status = 404
            render([
                error: 'Legacy /metrics/threads endpoint disabled',
                enable: 'Set rundeck.metrics.legacy.enabled=true to re-enable',
                migrate: 'Recommended: Use /monitoring/threaddump'
            ] as JSON)
            return
        }

        log.warn("DEPRECATED: Legacy /metrics/threads endpoint accessed. " +
                 "This endpoint will be removed in Rundeck 7.0. " +
                 "Migrate to /monitoring/threaddump")

        // Return thread dump in Dropwizard format
        def threadMXBean = java.lang.management.ManagementFactory.getThreadMXBean()
        def threads = threadMXBean.dumpAllThreads(true, true)
        
        def output = threads.collect { threadInfo ->
            [
                name: threadInfo.threadName,
                id: threadInfo.threadId,
                state: threadInfo.threadState.toString(),
                blockedCount: threadInfo.blockedCount,
                blockedTime: threadInfo.blockedTime,
                waitedCount: threadInfo.waitedCount,
                waitedTime: threadInfo.waitedTime,
                lockName: threadInfo.lockName,
                lockOwnerId: threadInfo.lockOwnerId,
                lockOwnerName: threadInfo.lockOwnerName,
                inNative: threadInfo.inNative,
                suspended: threadInfo.suspended,
                stackTrace: threadInfo.stackTrace.collect { it.toString() }
            ]
        }

        render output as JSON
    }
    
    private Map buildGauges() {
        metricRegistry.gauges.collectEntries { name, gauge ->
            [(name): [value: gauge.value]]
        }
    }
    
    private Map buildCounters() {
        metricRegistry.counters.collectEntries { name, counter ->
            [(name): [count: counter.count]]
        }
    }
    
    private Map buildHistograms() {
        metricRegistry.histograms.collectEntries { name, histogram ->
            def snapshot = histogram.snapshot
            [(name): [
                count: histogram.count,
                min: snapshot.min,
                max: snapshot.max,
                mean: snapshot.mean,
                stddev: snapshot.stdDev,
                median: snapshot.median,
                p75: snapshot.get75thPercentile(),
                p95: snapshot.get95thPercentile(),
                p98: snapshot.get98thPercentile(),
                p99: snapshot.get99thPercentile(),
                p999: snapshot.get999thPercentile()
            ]]
        }
    }
    
    private Map buildMeters() {
        metricRegistry.meters.collectEntries { name, meter ->
            [(name): [
                count: meter.count,
                m15_rate: meter.fifteenMinuteRate,
                m1_rate: meter.oneMinuteRate,
                m5_rate: meter.fiveMinuteRate,
                mean_rate: meter.meanRate,
                units: "events/second"
            ]]
        }
    }
    
    private Map buildTimers() {
        metricRegistry.timers.collectEntries { name, timer ->
            def snapshot = timer.snapshot
            [(name): [
                count: timer.count,
                max: snapshot.max,
                mean: snapshot.mean,
                min: snapshot.min,
                p50: snapshot.median,
                p75: snapshot.get75thPercentile(),
                p95: snapshot.get95thPercentile(),
                p98: snapshot.get98thPercentile(),
                p99: snapshot.get99thPercentile(),
                p999: snapshot.get999thPercentile(),
                stddev: snapshot.stdDev,
                m15_rate: timer.fifteenMinuteRate,
                m1_rate: timer.oneMinuteRate,
                m5_rate: timer.fiveMinuteRate,
                mean_rate: timer.meanRate,
                duration_units: "nanoseconds",
                rate_units: "calls/second"
            ]]
        }
    }
}

