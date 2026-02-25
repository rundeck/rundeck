package org.rundeck.tests.functional.api.system

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

/**
 * Functional tests for Metrics endpoints (Rundeck 6.0 / Grails 7)
 * 
 * Tests dual-mode metrics:
 * - Legacy Dropwizard endpoints: /metrics/* (disabled by default, opt-in with rundeck.metrics.legacy.enabled=true)
 * - Modern Spring Boot Actuator endpoints: /monitoring/* (enabled by default)
 * 
 * Pattern #52-54: Metrics Servlet Endpoints Migration
 * See: GRAILS7_HANDOFF/METRICS_SERVLET_ENDPOINTS.md
 */
@APITest
class MetricsSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
    }

    // ========================================================================
    // MODERN ENDPOINTS (Spring Boot Actuator) - Always available by default
    // ========================================================================

    def "Test modern Prometheus endpoint"() {
        when:
            def response = client.doGetAcceptAll("/monitoring/prometheus")
        then:
            verifyAll {
                response.code() == 200
                def body = response.body().string()
                body.contains("# HELP")
                body.contains("# TYPE")
                // Prometheus text format
                body.contains("application_started_time_seconds")
            }
    }

    def "Test modern metrics list endpoint"() {
        when:
            def response = client.doGetAcceptAll("/monitoring/metrics")
        then:
            verifyAll {
                response.code() == 200
                def body = response.body().string()
                body.contains('"names"')
                // Should have Micrometer metrics
                body.contains("jvm.memory.used")
            }
    }

    def "Test modern health endpoint"() {
        when:
            def response = client.doGetAcceptAll("/monitoring/health")
        then:
            verifyAll {
                response.code() == 200
                def body = response.body().string()
                body.contains('"status"')
                body.contains('UP')
            }
    }

    def "Test modern threaddump endpoint"() {
        when:
            def response = client.doGetAcceptAll("/monitoring/threaddump")
        then:
            verifyAll {
                response.code() == 200
                def body = response.body().string()
                body.readLines().size() > 10
                // Thread dump should contain thread info
                body.contains("Thread")
            }
    }

    // ========================================================================
    // LEGACY ENDPOINTS (Dropwizard) - Disabled by default in Rundeck 6.0
    // ========================================================================

    def "Test legacy healthcheck disabled by default"() {
        when:
            def response = client.doGetAcceptAll("/metrics/healthcheck")
        then:
            response.code() == 404
    }

    def "Test legacy threads disabled by default"() {
        when:
            def response = client.doGetAcceptAll("/metrics/threads")
        then:
            response.code() == 404
    }

    // ========================================================================
    // API ENDPOINTS - Forward to legacy (disabled by default)
    // ========================================================================

    def "Test API metrics list with legacy disabled"() {
        when:
            def responseMetrics = get("/metrics", Map)
        then:
            verifyAll {
                // When legacy is disabled, API returns empty links
                responseMetrics._links != null
                responseMetrics._links.size() == 0
            }
    }

    // ========================================================================
    // BACKWARD COMPATIBILITY - Tests for when legacy is enabled
    // NOTE: These tests require rundeck.metrics.legacy.enabled=true
    // They are informational and will pass if legacy is disabled (404)
    // ========================================================================

    def "Test legacy metrics format when enabled (backward compatibility)"() {
        when:
            def response = client.doGetAcceptAll("/metrics/metrics")
        then:
            if (response.code() == 200) {
                // Legacy enabled - verify Dropwizard 4.0.0 format
                def body = response.body().string()
                verifyAll {
                    body.contains('"version":"4.0.0"')
                    body.contains('"gauges"')
                    body.contains('"counters"')
                    body.contains('"meters"')
                    body.contains('"timers"')
                    body.contains('"histograms"')
                }
            } else {
                // Legacy disabled (default) - expect 404
                assert response.code() == 404
            }
    }

    def "Test legacy ping-pong when enabled (backward compatibility)"() {
        when:
            def response = client.doGetAcceptAll("/metrics/ping")
        then:
            if (response.code() == 200) {
                // Legacy enabled - verify pong response
                def body = response.body().string()
                assert body.contains("pong")
                assert body.count("pong") == 1
            } else {
                // Legacy disabled (default) - expect 404
                assert response.code() == 404
            }
    }

    def "Test API metrics list when legacy enabled (backward compatibility)"() {
        when:
            def responseMetrics = get("/metrics", Map)
        then:
            if (responseMetrics._links.size() == 4) {
                // Legacy enabled - all links present
                verifyAll {
                    responseMetrics._links.metrics.href == client.baseUrl + "/api/${client.apiVersion}" + "/metrics/metrics"
                    responseMetrics._links.ping.href == client.baseUrl + "/api/${client.apiVersion}" + "/metrics/ping"
                    responseMetrics._links.threads.href == client.baseUrl + "/api/${client.apiVersion}" + "/metrics/threads"
                    responseMetrics._links.healthcheck.href == client.baseUrl + "/api/${client.apiVersion}" + "/metrics/healthcheck"
                }
            } else {
                // Legacy disabled (default) - no links
                assert responseMetrics._links.size() == 0
            }
    }

}
