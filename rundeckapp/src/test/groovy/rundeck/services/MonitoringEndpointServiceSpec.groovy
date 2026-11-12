package rundeck.services

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class MonitoringEndpointServiceSpec extends Specification {

    MonitoringEndpointService service
    ApplicationContext applicationContext
    ConfigurationService configurationService

    def setup() {
        applicationContext = Mock(ApplicationContext)
        configurationService = Mock(ConfigurationService)
        service = new MonitoringEndpointService()
        service.applicationContext = applicationContext
        service.configurationService = configurationService
    }

    def "monitoring is enabled by default"() {
        when:
            boolean enabled = service.monitoringEnabled
        then:
            1 * configurationService.getBoolean('metrics.monitoring.enabled', true) >> true
            enabled
    }

    def "metrics payload lists meter names"() {
        given:
            MeterRegistry registry = new SimpleMeterRegistry()
            registry.counter('jvm.memory.used')
            registry.counter('jvm.memory.used')
            applicationContext.getBean(MeterRegistry) >> registry
        when:
            MonitoringEndpointService.Payload payload = service.metricsPayload()
        then:
            payload.status == 200
            payload.contentType == MonitoringEndpointService.JSON
            payload.body.contains('"names"')
            payload.body.contains('jvm.memory.used')
    }

    def "prometheus payload is 503 when the registry bean is missing"() {
        given:
            applicationContext.getBean('prometheusMeterRegistry') >> {
                throw new NoSuchBeanDefinitionException('prometheusMeterRegistry')
            }
        when:
            MonitoringEndpointService.Payload payload = service.prometheusPayload()
        then:
            payload.status == 503
            payload.body.contains('bean not configured')
    }

    def "health payload is UP"() {
        when:
            MonitoringEndpointService.Payload payload = service.healthPayload()
        then:
            payload.status == 200
            payload.body.contains('UP')
    }

    def "readiness assumes UP when ApplicationAvailability is missing"() {
        given:
            applicationContext.getBean(ApplicationAvailability) >> {
                throw new NoSuchBeanDefinitionException(ApplicationAvailability)
            }
        when:
            MonitoringEndpointService.Payload payload = service.readinessPayload()
        then:
            payload.status == 200
            payload.body.contains('UP')
    }

    def "thread dump payload includes stack text"() {
        when:
            MonitoringEndpointService.Payload payload = service.threadDumpPayload()
        then:
            payload.status == 200
            payload.contentType == MonitoringEndpointService.TEXT
            payload.body.contains('Thread')
            payload.body.readLines().size() > 1
    }

    def "unknown endpoint is 404"() {
        when:
            MonitoringEndpointService.Payload payload = service.payloadFor('nope')
        then:
            payload.status == 404
    }
}
