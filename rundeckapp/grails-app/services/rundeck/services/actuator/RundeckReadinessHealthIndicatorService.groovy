package rundeck.services.actuator

import grails.core.GrailsApplication
import grails.events.annotation.Subscriber
import grails.events.bus.EventBusAware
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.boot.availability.AvailabilityChangeEvent
import org.springframework.boot.availability.ReadinessState

@CompileStatic
class RundeckReadinessHealthIndicatorService implements EventBusAware {

    GrailsApplication grailsApplication

    @Subscriber("rundeck.bootstrap")
    void rundeckReady() {
        AvailabilityChangeEvent.publish(grailsApplication.mainContext, ReadinessState.ACCEPTING_TRAFFIC);
    }
}
