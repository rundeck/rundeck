package org.rundeck.spi

import org.rundeck.app.spi.AppService
import org.rundeck.app.spi.SimpleServiceProvider
import spock.lang.Shared
import spock.lang.Specification

class SimpleServiceProviderSpec extends Specification {
    @Shared SimpleServiceProvider ssp

    static final class Service implements AppService {}

    def setup() {
        ssp = new SimpleServiceProvider([(Service): new Service()])
    }

    def "test returns object for type"() {
        when:
        def obj = ssp.getService(Service)
        then:
        obj.class == Service
    }
}
