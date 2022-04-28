package rundeck.services.actuator

import grails.core.GrailsApplication
import grails.events.annotation.Subscriber
import grails.events.bus.EventBusAware
import org.springframework.boot.availability.AvailabilityChangeEvent
import org.springframework.boot.availability.ReadinessState

class RundeckReadinessHealthIndicatorService implements EventBusAware {

    GrailsApplication grailsApplication

    @Subscriber("rundeck.bootstrap")
    void rundeckReady() {
        AvailabilityChangeEvent.publish(grailsApplication.mainContext, ReadinessState.ACCEPTING_TRAFFIC);
    }
}
