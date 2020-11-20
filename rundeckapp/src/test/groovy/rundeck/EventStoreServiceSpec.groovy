package rundeck

import com.dtolabs.rundeck.core.event.EventQueryType
import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import rundeck.services.EventStoreService
import rundeck.services.Evt
import rundeck.services.EvtQuery
import rundeck.services.FrameworkService
import spock.lang.Shared

class EventStoreServiceSpec extends HibernateSpec implements ServiceUnitTest<EventStoreService> {
    @Shared FrameworkService framework

    def setupSpec() {
        framework = Mock(FrameworkService) {
            it.serverUUID >> 'ABCDEFG'
        }
    }

    def setup() {
        service.frameworkService = framework
    }

    def "test basic thing"() {
        when:
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

    def "test queries"() {
        when:
        service.storeEventBatch([
                [projectName: 'A', subsystem: 'webhooks'] as Evt,
                [projectName: 'A', subsystem: 'cluster'] as Evt,
                [projectName: 'B', subsystem: 'cluster'] as Evt,
        ])

        def oneRes = service.query([projectName: 'A', subsystem: 'webhooks'] as EvtQuery)
        def twoRes = service.query([projectName: 'A'] as EvtQuery)
        then:
        oneRes.totalCount == 1
        twoRes.totalCount == 2
    }

    def "test count query does not include events in results"() {
        when:
        service.storeEventBatch([
                [projectName: 'A', subsystem: 'webhooks'] as Evt,
                [projectName: 'A', subsystem: 'cluster'] as Evt,
                [projectName: 'B', subsystem: 'cluster'] as Evt,
        ])

        def oneRes = service.query([projectName: 'A', subsystem: 'webhooks', queryType: EventQueryType.COUNT] as EvtQuery)
        def twoRes = service.query([projectName: 'A', queryType: EventQueryType.COUNT] as EvtQuery)
        then:
        oneRes.totalCount == 1
        oneRes.events.size() == 0
        twoRes.totalCount == 2
        twoRes.events.size() == 0
    }

    def "test scoped service scopes store calls"() {
        when:
        def scoped = service.scoped([projectName: 'C'] as Evt, null)

        scoped.storeEventBatch([
                [projectName: 'A'] as Evt,
                [projectName: 'A'] as Evt,
                [projectName: 'B'] as Evt,
        ])

        def scopedRes = scoped.query([projectName: 'C'] as EvtQuery)
        def unscopedRes = scoped.query([projectName: 'A'] as EvtQuery)
        then:
        scopedRes.totalCount == 3
        unscopedRes.totalCount == 0
    }

    def "test scoped service scopes queries"() {
        when:
        service.storeEventBatch([
                [projectName: 'A'] as Evt,
                [projectName: 'A'] as Evt,
                [projectName: 'B'] as Evt,
        ])

        // Scopes queries to project B
        def scoped = service.scoped(null, [projectName: 'B'] as EvtQuery)

        def scopedRes = scoped.query([projectName: 'A'] as EvtQuery)
        def unscopedRes = service.query([projectName: 'A'] as EvtQuery)
        then:
        scopedRes.totalCount == 1
        unscopedRes.totalCount == 2
    }

    def "test wildcard topic query"() {
        when:
        service.storeEventBatch([
                [topic: 'foo/bar'] as Evt,
                [topic: 'foo/bar/baz'] as Evt,
                [topic: 'foo/baz'] as Evt,
        ])


        def threeRes = service.query([topic: 'foo/*'] as EvtQuery)
        def twoRes = service.query([topic: 'foo/bar*'] as EvtQuery)
        then:
        threeRes.totalCount == 3
        twoRes.totalCount == 2
    }
}
