package rundeck

import grails.test.hibernate.HibernateSpec
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import rundeck.services.EventStoreService
import rundeck.services.Evt
import rundeck.services.EvtQuery
import rundeck.services.FrameworkService
import spock.lang.Shared

class EventStoreServiceSpec extends HibernateSpec implements ServiceUnitTest<EventStoreService> {
    @Shared FrameworkService framework

    def setup() {
        framework = Mock(FrameworkService) {
            it.serverUUID >> 'ABCDEFG'
        }
    }

    def "test basic thing"() {
        when:
        service.frameworkService = framework

        service.storeEvent(new Evt(
                projectName: 'test',
        ))

        def res  = service.query(new EvtQuery(
                projectName: 'test'
        ))

        then:
        res.events.size() == 1
        res.totalCount == 1
    }

    def "test scoped service"() {
        when:
        service.frameworkService = framework

        service.storeEventBatch([
                [projectName: 'A'] as Evt,
                [projectName: 'A'] as Evt,
                [projectName: 'B'] as Evt,
        ])

        def scoped = service.scoped(null, [projectName: 'B'] as EvtQuery)
        def res = scoped.query([:] as EvtQuery)
        then:
        res.events.size() == 1
        res.totalCount == 1

    }


}
