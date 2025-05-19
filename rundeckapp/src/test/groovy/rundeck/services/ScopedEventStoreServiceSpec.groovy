package rundeck.services

import com.dtolabs.rundeck.core.event.Event
import com.dtolabs.rundeck.core.event.EventQueryImpl
import com.dtolabs.rundeck.core.event.EventStoreService
import org.rundeck.app.data.model.v1.storedevent.StoredEventQuery
import org.rundeck.app.data.model.v1.storedevent.StoredEventQueryType
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class ScopedEventStoreServiceSpec extends Specification {
    def "scoped service query uses input type"() {
        given: "a scoped query service with query template"
            def eventStoreService = Mock(EventStoreService)
            def event = Mock(Event)
            def query = Mock(StoredEventQuery)
            def scopedEventStoreService = new ScopedEventStoreService(eventStoreService, event, query)
            def scoped = scopedEventStoreService.scoped(
                new Evt(projectName: 'aproj', subsystem: 'webhooks'),
                new EvtQuery(projectName: 'aproj', subsystem: 'webhooks')
            )
            def toDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))

        when: "query specifies query type"
            def result = scoped.query(
                new EventQueryImpl(
                    queryType: qtype,
                    projectName: 'some-other-project',
                    subsystem: "some-other-subsystem",
                    topic: topic,
                    dateTo: toDate
                )
            )

        then: "query is called with the correct parameters"
            1 * eventStoreService.query(
                {
                    it.queryType == qtype
                    it.projectName == 'aproj'
                    it.subsystem == 'webhooks'
                    it.topic == topic
                    it.dateTo == toDate
                }
            )
        where:
            qtype                       | topic
            StoredEventQueryType.DELETE | 'atopic'
            StoredEventQueryType.SELECT | 'atopic'
            StoredEventQueryType.COUNT  | 'atopic'
    }
}
