package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.config.Features
import grails.converters.JSON
import org.rundeck.core.auth.AuthConstants
import rundeck.services.MonitoringEndpointService
import rundeck.services.feature.FeatureService

import javax.security.auth.Subject

/**
 * Web endpoints at {@code /monitoring/*}.
 *
 * Health and readiness stay anonymous so Kubernetes probes can call them.
 * Metrics, Prometheus, info, and the thread dump are anonymous unless
 * {@code rundeck.feature.monitoringAuth.enabled} is true. Then they require
 * system read or admin, the same check as {@code /api/{version}/monitoring/*}.
 *
 * Disable every endpoint with {@code rundeck.metrics.monitoring.enabled=false}.
 */
class MonitoringController {

    private static final Set<String> AUTH_GATED_ENDPOINTS = ['metrics', 'prometheus', 'info', 'threaddump'] as Set

    MonitoringEndpointService monitoringEndpointService
    FeatureService featureService
    def rundeckAuthContextProcessor

    /**
     * {@code /monitoring/metrics} — metric names.
     * Anonymous unless {@code rundeck.feature.monitoringAuth.enabled} is true.
     */
    def metrics() {
        renderEndpoint('metrics')
    }

    /**
     * {@code /monitoring/prometheus} — Prometheus scrape.
     * Anonymous unless {@code rundeck.feature.monitoringAuth.enabled} is true.
     */
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
     * {@code /monitoring/info} — application name and version.
     * Anonymous unless {@code rundeck.feature.monitoringAuth.enabled} is true.
     */
    def info() {
        renderEndpoint('info')
    }

    /**
     * {@code /monitoring/threaddump} — JVM thread dump.
     * Anonymous unless {@code rundeck.feature.monitoringAuth.enabled} is true.
     */
    def threaddump() {
        renderEndpoint('threaddump')
    }

    /**
     * Render one monitoring endpoint, or the disabled response when monitoring is off.
     * Data endpoints check {@code monitoringAuth} before rendering.
     */
    private void renderEndpoint(String endpoint) {
        log.debug("MonitoringController.${endpoint}() - checking config")
        if (AUTH_GATED_ENDPOINTS.contains(endpoint) && !allowDataEndpoint()) {
            return
        }
        if (!monitoringEndpointService.monitoringEnabled) {
            log.info("Monitoring endpoint /monitoring/${endpoint} accessed but disabled via config")
            renderDisabled(endpoint)
            return
        }
        renderPayload(monitoringEndpointService.payloadFor(endpoint))
    }

    /**
     * @return true when the caller may read a data endpoint
     */
    private boolean allowDataEndpoint() {
        if (!featureService.featurePresent(Features.MONITORING_AUTH, false)) {
            return true
        }
        def subject = session?.subject
        if (!(subject instanceof Subject)) {
            redirect(controller: 'user', action: 'login')
            return false
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(subject)
        boolean allowed = rundeckAuthContextProcessor.authorizeApplicationResourceAny(
            authContext,
            AuthConstants.RESOURCE_TYPE_SYSTEM,
            [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_OPS_ADMIN]
        )
        if (!allowed) {
            response.status = 403
            render([error: 'Not authorized'] as JSON)
            return false
        }
        return true
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
