package rundeck.services.scm

import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import spock.lang.Specification

/**
 * Created by greg on 10/16/15.
 */
class ProjectJobChangeListenerSpec extends Specification {
    def "ignores wrong project"() {
        given:
        def listener = Mock(JobChangeListener)
        def p = new ProjectJobChangeListener(listener, 'projectA')
        JobChangeEvent event = Mock(JobChangeEvent) {
            1 * getOriginalJobReference() >> Mock(JobReference) {
                getProject() >> 'projectB'

            }
        }

        when:
        p.jobChangeEvent(event, null)

        then:
        0 * listener.jobChangeEvent(*_)

    }

    def "accepts right project"() {
        given:
        def listener = Mock(JobChangeListener)
        def p = new ProjectJobChangeListener(listener, 'projectA')
        JobChangeEvent event = Mock(JobChangeEvent) {
            1 * getOriginalJobReference() >> Mock(JobReference) {
                getProject() >> 'projectA'

            }
        }

        when:
        p.jobChangeEvent(event, null)

        then:
        1 * listener.jobChangeEvent(event, _)

    }
}
