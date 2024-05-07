package org.rundeck.tests.functional.api.system

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class MetricsSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
    }

    def "Test metrics list"() {
        when:
            def responseMetrics = get("/metrics", Map)
        then:
            verifyAll {
                responseMetrics._links.size() == 4
                responseMetrics._links.metrics.href == client.baseUrl + "/api/${client.apiVersion}" + "/metrics/metrics"
                responseMetrics._links.ping.href == client.baseUrl + "/api/${client.apiVersion}" + "/metrics/ping"
                responseMetrics._links.threads.href == client.baseUrl + "/api/${client.apiVersion}" + "/metrics/threads"
                responseMetrics._links.healthcheck.href == client.baseUrl + "/api/${client.apiVersion}" + "/metrics/healthcheck"
            }
    }

    def "Test metric metrics"() {
        when:
            def responseMetricsMetrics = get("/metrics/metrics", Map)
        then:
            verifyAll {
                responseMetricsMetrics.containsKey("gauges")
                responseMetricsMetrics.gauges.size() > 0
                responseMetricsMetrics.containsKey("counters")
                responseMetricsMetrics.counters.size() > 0
                responseMetricsMetrics.containsKey("meters")
                responseMetricsMetrics.meters.size() > 0
                responseMetricsMetrics.containsKey("timers")
                responseMetricsMetrics.timers.size() > 0
            }
    }

    def "Test metrics ping-pong"() {
        when:
            def responseMetricsPing = client.doGetAcceptAll("/metrics/ping")
        then:
            verifyAll {
                responseMetricsPing.code() == 200
                def aux = responseMetricsPing.body().string()
                aux.contains("pong")
                aux.count("pong") == 1
            }
    }

    def "Test metrics threads"() {
        when:
            def responseMetricsThreads = client.doGetAcceptAll("/metrics/threads")
        then:
            verifyAll {
                responseMetricsThreads.code() == 200
                def aux = responseMetricsThreads.body().string()
                aux.readLines().size() > 10
            }
    }

    def "Test metrics healthcheck"() {
        when:
            def responseMetricsHealthcheck = get("/metrics/healthcheck", Map)
        then:
            verifyAll {
                responseMetricsHealthcheck.size() == 2
                responseMetricsHealthcheck["dataSource.connection.time"].healthy == true
                responseMetricsHealthcheck["quartz.scheduler.threadPool"].healthy == true
            }
    }

}
