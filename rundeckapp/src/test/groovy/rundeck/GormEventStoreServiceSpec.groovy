package rundeck

import com.dtolabs.rundeck.core.event.EventQueryType
import com.fasterxml.jackson.databind.ObjectMapper
import grails.gorm.transactions.Rollback
import grails.test.hibernate.HibernateSpec
import org.grails.orm.hibernate.HibernateDatastore
import rundeck.services.Evt
import rundeck.services.EvtQuery
import rundeck.services.FrameworkService
import rundeck.services.GormEventStoreService
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import testhelper.RundeckHibernateSpec

class GormEventStoreServiceSpec extends RundeckHibernateSpec {
    @Shared GormEventStoreService service
    @Shared FrameworkService framework

    private static class Foo {
        String event
    }

    List<Class> getDomainClasses() { [StoredEvent] }

    def setupSpec() {
        framework = Mock(FrameworkService) {
            it.serverUUID >> '16b02806-f4b3-4628-9d9c-2dd2cc67d53c'
        }
        service = new GormEventStoreService()
        service.frameworkService = framework
    }

    def setup() {}

    @Rollback
    def "test basic store and querys"() {
        def event = new Foo(event: 'test')

        when:
        service.storeEvent(new Evt(
                projectName: 'test',
                subsystem: 'test',
                topic: 'test',
                meta: event
        ))

        def res  = service.query(new EvtQuery(
                projectName: 'test'
        ))

        def storedEvent = new ObjectMapper().readValue(res.events[0].meta as String, Foo.class)

        then:
        storedEvent.event == 'test'
        res.events.size() == 1
        res.totalCount == 1
    }

    @Rollback
    def "test queries"() {
        when:
        service.storeEventBatch([
                [projectName: 'A', topic: 'test', subsystem: 'webhooks'] as Evt,
                [projectName: 'A', topic: 'test', subsystem: 'cluster'] as Evt,
                [projectName: 'B', topic: 'test', subsystem: 'cluster'] as Evt,
        ])

        def oneRes = service.query([projectName: 'A', subsystem: 'webhooks'] as EvtQuery)
        def twoRes = service.query([projectName: 'A'] as EvtQuery)
        then:
        oneRes.totalCount == 1
        twoRes.totalCount == 2
    }

    @Rollback
    def "test count query does not include events in results"() {
        when:
        service.storeEventBatch([
                [projectName: 'A', topic: 'test', subsystem: 'webhooks'] as Evt,
                [projectName: 'A', topic: 'test', subsystem: 'cluster'] as Evt,
                [projectName: 'B', topic: 'test', subsystem: 'cluster'] as Evt,
        ])

        def oneRes = service.query([projectName: 'A', subsystem: 'webhooks', queryType: EventQueryType.COUNT] as EvtQuery)
        def twoRes = service.query([projectName: 'A', queryType: EventQueryType.COUNT] as EvtQuery)
        then:
        oneRes.totalCount == 1
        oneRes.events.size() == 0
        twoRes.totalCount == 2
        twoRes.events.size() == 0
    }

    @Rollback
    def "test scoped service scopes store calls"() {
        when:
        def scoped = service.scoped([projectName: 'C'] as Evt, null)

        scoped.storeEventBatch([
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'B', topic: 'test', subsystem: 'test'] as Evt,
        ])

        def scopedRes = scoped.query([projectName: 'C'] as EvtQuery)
        def unscopedRes = scoped.query([projectName: 'A'] as EvtQuery)
        then:
        scopedRes.totalCount == 3
        unscopedRes.totalCount == 0
    }

    @Rollback
    def "test scoped service scopes queries"() {
        when:
        service.storeEventBatch([
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'B', topic: 'test', subsystem: 'test'] as Evt,
        ])

        // Scopes queries to project B
        def scoped = service.scoped(null, [projectName: 'B'] as EvtQuery)

        def scopedRes = scoped.query([projectName: 'A'] as EvtQuery)
        def unscopedRes = service.query([projectName: 'A'] as EvtQuery)
        then:
        scopedRes.totalCount == 1
        unscopedRes.totalCount == 2
    }

    @Rollback
    def "test wildcard topic query"() {
        when:
        service.storeEventBatch([
                [topic: 'foo/bar', projectName: 'test', subsystem: 'test'] as Evt,
                [topic: 'foo/bar/baz', projectName: 'test', subsystem: 'test'] as Evt,
                [topic: 'foo/baz',projectName: 'test', subsystem: 'test'] as Evt,
        ])


        def threeRes = service.query([topic: 'foo/*'] as EvtQuery)
        def twoRes = service.query([topic: 'foo/bar*'] as EvtQuery)
        then:
        threeRes.totalCount == 3
        twoRes.totalCount == 2
    }

    @Rollback
    def "test pagination"() {
        when:
        service.storeEventBatch([
                [projectName: 'test', topic: 'test', subsystem: 'test', meta: "ONE", sequence: 0] as Evt,
                [projectName: 'test', topic: 'test', subsystem: 'test', meta: "TWO", sequence: 1] as Evt,
                [projectName: 'test', topic: 'test', subsystem: 'test', meta: "THREE", sequence: 2] as Evt,
        ])

        def oneRes = service.query([maxResults: 1, offset: 0] as EvtQuery)
        def twoRes = service.query([maxResults: 2, offset: 1] as EvtQuery)
        then: 'events are returned newest first and paginated'
        oneRes.events.size() == 1
        oneRes.events[0].meta == '"THREE"'
        twoRes.events[1].meta == '"ONE"'
    }

    @Rollback
    def "test delete"() {
        when:
        service.storeEventBatch([
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'B', topic: 'test', subsystem: 'test'] as Evt,
        ])

        def threeRes = service.query([:] as EvtQuery)

        then:
        threeRes.totalCount == 3

        when:
        def delRes = service.query([projectName: 'A', queryType: EventQueryType.DELETE] as EvtQuery)
        def oneRes = service.query([:] as EvtQuery)

        then:
        delRes.totalCount == 2
        oneRes.totalCount == 1
    }

    // TODO: Limiting does not appear to work on detached criteria deleteAll
//    def "test delete limit"() {
//        when:
//        service.storeEventBatch([
//                [projectName: 'A'] as Evt,
//                [projectName: 'A'] as Evt,
//                [projectName: 'B'] as Evt,
//        ])
//
//        def delRes = service.query([projectName: 'A', maxResults: 1, queryType: EventQueryType.DELETE] as EvtQuery)
//        def twoRes = service.query([:] as EvtQuery)
//
//        then:
//        twoRes.totalCount == 2
//    }
}
