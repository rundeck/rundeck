package rundeck.controllers

import grails.converters.JSON
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

/**
 * Controller wrapper for monitoring endpoints at /monitoring/*.
 * 
 * Provides config-based enable/disable control for monitoring endpoints.
 * Uses the same pattern as LegacyMetricsController - simple and proven.
 * 
 * Default: enabled=true (monitoring endpoints ON)
 * Can be disabled: rundeck.metrics.monitoring.enabled=false
 * 
 * When enabled, calls Spring Boot Actuator beans (MeterRegistry, PrometheusMeterRegistry) directly.
 * When disabled, returns 404 with helpful message.
 */
class MonitoringController {
    
    @Autowired
    ApplicationContext applicationContext
    
    /**
     * /monitoring/metrics - List of available metrics
     */
    def metrics() {
        log.debug("MonitoringController.metrics() - checking config")
        
        if (!isMonitoringEnabled()) {
            log.info("Monitoring endpoint /monitoring/metrics accessed but disabled via config")
            renderDisabled('metrics')
            return
        }
        
        try {
            MeterRegistry meterRegistry = applicationContext.getBean(MeterRegistry)
            def metricNames = meterRegistry.meters.collect { it.id.name }.unique().sort()
            log.debug("Returning ${metricNames.size()} metric names")
            
            render([names: metricNames] as JSON)
        } catch (Exception e) {
            log.error("Error getting metrics: ${e.message}", e)
            response.status = 500
            render([error: "Failed to get metrics: ${e.message}"] as JSON)
        }
    }
    
    /**
     * /monitoring/prometheus - Prometheus scrape format
     */
    def prometheus() {
        log.debug("MonitoringController.prometheus() - checking config")
        
        if (!isMonitoringEnabled()) {
            log.info("Monitoring endpoint /monitoring/prometheus accessed but disabled via config")
            renderDisabled('prometheus')
            return
        }
        
        try {
            // Get Prometheus registry bean by name (avoids classloader issues)
            // Spring Boot auto-configures this bean when micrometer-registry-prometheus is on the classpath
            def promRegistry = applicationContext.getBean('prometheusMeterRegistry')
            def scrapeOutput = promRegistry.scrape()
            log.debug("Returning Prometheus scrape output (${scrapeOutput.length()} bytes)")
            
            response.contentType = 'text/plain; version=0.0.4'
            response.characterEncoding = 'UTF-8'
            render scrapeOutput
        } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
            log.error("Prometheus registry bean not found - Spring Boot may not have auto-configured it: ${e.message}")
            response.status = 503
            render([error: "Prometheus metrics not available - bean not configured"] as JSON)
        } catch (Exception e) {
            log.error("Error getting Prometheus metrics: ${e.message}", e)
            response.status = 503
            render([error: "Prometheus metrics not available: ${e.message}"] as JSON)
        }
    }
    
    /**
     * /monitoring/health - Simple health check
     */
    def health() {
        log.debug("MonitoringController.health() - checking config")
        
        if (!isMonitoringEnabled()) {
            log.info("Monitoring endpoint /monitoring/health accessed but disabled via config")
            renderDisabled('health')
            return
        }
        
        render([status: 'UP'] as JSON)
    }
    
    /**
     * /monitoring/health/readiness - Readiness probe for Kubernetes/test framework
     * Returns readiness state from Spring Boot AvailabilityState
     */
    def readiness() {
        log.debug("MonitoringController.readiness() - checking config")
        
        if (!isMonitoringEnabled()) {
            log.info("Monitoring endpoint /monitoring/health/readiness accessed but disabled via config")
            renderDisabled('health/readiness')
            return
        }
        
        try {
            // Get ReadinessState from Spring Boot AvailabilityState
            // This is set by RundeckReadinessHealthIndicatorService when rundeck.bootstrap fires
            def readinessState = org.springframework.boot.availability.ReadinessState.ACCEPTING_TRAFFIC
            def applicationAvailability = applicationContext.getBean(org.springframework.boot.availability.ApplicationAvailability)
            def currentReadiness = applicationAvailability.getReadinessState()
            
            if (currentReadiness == readinessState) {
                render([status: 'UP'] as JSON)
            } else {
                response.status = 503
                render([status: 'DOWN', readiness: currentReadiness.toString()] as JSON)
            }
        } catch (Exception e) {
            // If ApplicationAvailability bean not available, assume ready (backward compatibility)
            log.debug("ApplicationAvailability bean not available, assuming ready: ${e.message}")
            render([status: 'UP'] as JSON)
        }
    }
    
    /**
     * /monitoring/info - Application information
     */
    def info() {
        log.debug("MonitoringController.info() - checking config")
        
        if (!isMonitoringEnabled()) {
            log.info("Monitoring endpoint /monitoring/info accessed but disabled via config")
            renderDisabled('info')
            return
        }
        
        render([
            app: [
                name: grailsApplication.config.getProperty('info.app.name', String, 'rundeck'),
                version: grailsApplication.config.getProperty('info.app.version', String, 'unknown')
            ]
        ] as JSON)
    }
    
    /**
     * /monitoring/threaddump - Thread dump in text format
     */
    def threaddump() {
        log.debug("MonitoringController.threaddump() - checking config")
        
        if (!isMonitoringEnabled()) {
            log.info("Monitoring endpoint /monitoring/threaddump accessed but disabled via config")
            renderDisabled('threaddump')
            return
        }
        
        try {
            // Get thread dump from JVM
            def threadMXBean = java.lang.management.ManagementFactory.getThreadMXBean()
            def threads = threadMXBean.dumpAllThreads(true, true)
            
            def output = new StringBuilder()
            threads.each { threadInfo ->
                output.append(threadInfo.toString()).append("\n\n")
            }
            
            log.debug("Returning thread dump (${threads.length} threads)")
            
            response.contentType = 'text/plain;charset=UTF-8'
            response.characterEncoding = 'UTF-8'
            render output.toString()
        } catch (Exception e) {
            log.error("Error getting thread dump: ${e.message}", e)
            response.status = 500
            render([error: "Failed to retrieve thread dump: ${e.message}"] as JSON)
        }
    }
    
    /**
     * Check if monitoring endpoints are enabled via config.
     * Same pattern as LegacyMetricsController - reads from grailsApplication.config.
     */
    private boolean isMonitoringEnabled() {
        grailsApplication.config.getProperty(
            'rundeck.metrics.monitoring.enabled',
            Boolean,
            true  // Default: ON
        )
    }
    
    /**
     * Render 404 with helpful message when monitoring is disabled.
     * Same pattern as LegacyMetricsController.
     */
    private void renderDisabled(String endpoint) {
        response.status = 404
        render([
            error: 'Monitoring endpoints are disabled',
            endpoint: endpoint,
            enable: 'Set rundeck.metrics.monitoring.enabled=true to re-enable',
            location: 'rundeck-config.properties or System Config'
        ] as JSON)
    }
}

