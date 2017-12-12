/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
