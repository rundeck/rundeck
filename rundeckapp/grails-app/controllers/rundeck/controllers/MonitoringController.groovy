package rundeck.controllers

import grails.converters.JSON
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.web.RdAuthorizeSystem
import rundeck.services.MonitoringEndpointService

/**
 * Web endpoints at {@code /monitoring/*}.
 *
 * Health and readiness stay anonymous so Kubernetes probes can call them.
 * Metrics, Prometheus, info, and the thread dump require system read or admin,
 * the same check as {@code /api/{version}/monitoring/*}.
 *
 * Disable every endpoint with {@code rundeck.metrics.monitoring.enabled=false}.
 */
class MonitoringController {

    MonitoringEndpointService monitoringEndpointService

    /**
     * {@code /monitoring/metrics} — metric names. Requires system read or admin.
     */
    @RdAuthorizeSystem(
        value = RundeckAccess.System.AUTH_READ_OR_ANY_ADMIN,
        description = 'Read System Monitoring Metrics'
    )
    def metrics() {
        renderEndpoint('metrics')
    }

    /**
     * {@code /monitoring/prometheus} — Prometheus scrape. Requires system read or admin.
     */
    @RdAuthorizeSystem(
        value = RundeckAccess.System.AUTH_READ_OR_ANY_ADMIN,
        description = 'Read System Monitoring Metrics'
    )
    def prometheus() {
        renderEndpoint('prometheus')
    }

    /**
     * {@code /monitoring/health} — anonymous liveness body.
     */
    def health() {
        renderEndpoint('health')
    }

    /**
     * {@code /monitoring/health/readiness} — anonymous readiness probe.
     */
    def readiness() {
        if (!monitoringEndpointService.monitoringEnabled) {
            log.info('Monitoring endpoint /monitoring/health/readiness accessed but disabled via config')
            renderDisabled('health/readiness')
            return
        }
        renderPayload(monitoringEndpointService.readinessPayload())
    }

    /**
     * {@code /monitoring/info} — application name and version. Requires system read or admin.
     */
    @RdAuthorizeSystem(
        value = RundeckAccess.System.AUTH_READ_OR_ANY_ADMIN,
        description = 'Read System Monitoring Metrics'
    )
    def info() {
        renderEndpoint('info')
    }

    /**
     * {@code /monitoring/threaddump} — JVM thread dump. Requires system read or admin.
     */
    @RdAuthorizeSystem(
        value = RundeckAccess.System.AUTH_READ_OR_ANY_ADMIN,
        description = 'Read System Monitoring Metrics'
    )
    def threaddump() {
        renderEndpoint('threaddump')
    }

    /**
     * Render one monitoring endpoint, or the disabled response when the feature is off.
     * Authorization is declared on the action, not here.
     */
    private void renderEndpoint(String endpoint) {
        log.debug("MonitoringController.${endpoint}() - checking config")
        if (!monitoringEndpointService.monitoringEnabled) {
            log.info("Monitoring endpoint /monitoring/${endpoint} accessed but disabled via config")
            renderDisabled(endpoint)
            return
        }
        renderPayload(monitoringEndpointService.payloadFor(endpoint))
    }

    private void renderPayload(MonitoringEndpointService.Payload payload) {
        response.status = payload.status
        render(text: payload.body, contentType: payload.contentType, encoding: 'UTF-8')
    }

    /**
     * Render 404 when monitoring is disabled.
     */
    private void renderDisabled(String endpoint) {
        response.status = 404
        render([
            error   : 'Monitoring endpoints are disabled',
            endpoint: endpoint,
            enable  : 'Set rundeck.metrics.monitoring.enabled=true to re-enable',
            location: 'rundeck-config.properties or System Config'
        ] as JSON)
    }
}
