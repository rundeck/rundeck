/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

package rundeck.services

import grails.compiler.GrailsCompileStatic
import grails.core.GrailsApplication
import groovy.json.JsonOutput
import groovy.transform.CompileDynamic
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.ReadinessState
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo

/**
 * Builds the bodies for {@code /monitoring/*} and {@code /api/{version}/monitoring/*}.
 *
 * Callers enforce authentication. This service only reads metrics and formats the response.
 */
@GrailsCompileStatic
class MonitoringEndpointService implements ApplicationContextAware {

    static transactional = false

    static final String JSON = 'application/json'
    static final String PROMETHEUS = 'text/plain; version=0.0.4'
    static final String TEXT = 'text/plain'

    private static final List<String> ENDPOINTS = ['prometheus', 'metrics', 'health', 'info', 'threaddump']

    ApplicationContext applicationContext
    GrailsApplication grailsApplication
    ConfigurationService configurationService

    /**
     * Spring context used to look up Micrometer beans.
     * Grails does not inject {@code applicationContext} unless the service implements
     * {@link ApplicationContextAware}. If that still did not run, use the context Grails
     * already attached to {@code grailsApplication}, which is what {@code /monitoring/info} uses.
     */
    private ApplicationContext beanContext() {
        if (applicationContext != null) {
            return applicationContext
        }
        grailsApplication.mainContext
    }

    /**
     * Whether {@code rundeck.metrics.monitoring.enabled} is on. Defaults to true.
     */
    boolean isMonitoringEnabled() {
        configurationService.getBoolean('metrics.monitoring.enabled', true)
    }

    /**
     * @param endpoint one of prometheus, metrics, health, info, threaddump
     * @return formatted body for that endpoint
     */
    Payload payloadFor(String endpoint) {
        switch (endpoint) {
            case 'metrics':
                return metricsPayload()
            case 'prometheus':
                return prometheusPayload()
            case 'health':
                return healthPayload()
            case 'info':
                return infoPayload()
            case 'threaddump':
                return threadDumpPayload()
            default:
                return jsonPayload(404, [error: 'Unknown monitoring endpoint'])
        }
    }

    /**
     * Names exposed by the Micrometer registry.
     */
    Payload metricsPayload() {
        try {
            MeterRegistry meterRegistry = beanContext().getBean(MeterRegistry)
            List<String> metricNames = new ArrayList<String>()
            for (Meter meter : meterRegistry.meters) {
                String name = meter.id.name
                if (!metricNames.contains(name)) {
                    metricNames.add(name)
                }
            }
            metricNames.sort()
            return jsonPayload(200, [names: metricNames])
        } catch (Exception e) {
            log.error("Error getting metrics: ${e.message}", e)
            return jsonPayload(500, [error: "Failed to get metrics: ${e.message}"])
        }
    }

    /**
     * Prometheus text scrape. 503 when the registry bean is not configured.
     */
    @CompileDynamic
    Payload prometheusPayload() {
        try {
            def promRegistry = beanContext().getBean('prometheusMeterRegistry')
            String scrapeOutput = promRegistry.scrape() as String
            log.debug("Returning Prometheus scrape output (${scrapeOutput.length()} bytes)")
            return new Payload(status: 200, contentType: PROMETHEUS, body: scrapeOutput)
        } catch (NoSuchBeanDefinitionException e) {
            log.error("Prometheus registry bean not found: ${e.message}")
            return jsonPayload(503, [error: 'Prometheus metrics not available - bean not configured'])
        } catch (Exception e) {
            log.error("Error getting Prometheus metrics: ${e.message}", e)
            return jsonPayload(503, [error: "Prometheus metrics not available: ${e.message}"])
        }
    }

    /**
     * Liveness-style health body. Always UP when monitoring is enabled.
     */
    Payload healthPayload() {
        jsonPayload(200, [status: 'UP'])
    }

    /**
     * Readiness probe body. Assumes UP when ApplicationAvailability is absent.
     */
    Payload readinessPayload() {
        try {
            ApplicationAvailability applicationAvailability = beanContext().getBean(ApplicationAvailability)
            ReadinessState currentReadiness = applicationAvailability.getReadinessState()
            if (currentReadiness == ReadinessState.ACCEPTING_TRAFFIC) {
                return jsonPayload(200, [status: 'UP'])
            }
            return jsonPayload(503, [status: 'DOWN', readiness: currentReadiness.toString()])
        } catch (Exception e) {
            log.debug("ApplicationAvailability bean not available, assuming ready: ${e.message}")
            return jsonPayload(200, [status: 'UP'])
        }
    }

    /**
     * Application name and version from {@code info.app.*}.
     */
    @CompileDynamic
    Payload infoPayload() {
        jsonPayload(200, [
            app: [
                name   : grailsApplication.config.getProperty('info.app.name', String, 'rundeck'),
                version: grailsApplication.config.getProperty('info.app.version', String, 'unknown')
            ]
        ])
    }

    /**
     * Full JVM thread dump, including locked monitors and synchronizers.
     */
    Payload threadDumpPayload() {
        try {
            ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)
            StringBuilder output = new StringBuilder()
            for (ThreadInfo threadInfo : threads) {
                output.append(threadInfo.toString()).append('\n\n')
            }
            log.debug("Returning thread dump (${threads.length} threads)")
            return new Payload(status: 200, contentType: TEXT, body: output.toString())
        } catch (Exception e) {
            log.error("Error getting thread dump: ${e.message}", e)
            return jsonPayload(500, [error: "Failed to retrieve thread dump: ${e.message}"])
        }
    }

    /**
     * @return true when {@code endpoint} is one of the supported monitoring names
     */
    boolean knownEndpoint(String endpoint) {
        endpoint && ENDPOINTS.contains(endpoint)
    }

    private Payload jsonPayload(int status, Map body) {
        new Payload(status: status, contentType: JSON, body: JsonOutput.toJson(body))
    }

    /**
     * HTTP body produced for a monitoring endpoint.
     */
    static class Payload {
        int status
        String contentType
        String body
    }
}
