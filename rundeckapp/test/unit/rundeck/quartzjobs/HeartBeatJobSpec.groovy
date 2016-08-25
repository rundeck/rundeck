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

package rundeck.quartzjobs

import grails.test.mixin.Mock
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import rundeck.Messaging
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService
import rundeck.services.MessagingService
import spock.lang.Specification


@Mock([ScheduledExecution, Messaging])
class HeartBeatJobSpec extends Specification {
    public static final String TEST_UUID = 'BB27B7BB-4F13-44B7-B64B-D2435E2DD8C7'
    MessagingService messagingService = Mock(MessagingService)

    def setup(boolean enabled=false){
        HeartBeatJob job = new HeartBeatJob()
        job.frameworkService = Stub(FrameworkService) {
            existsFrameworkProject('testProject') >> true
            isClusterModeEnabled()>>enabled
            getServerUUID()>>TEST_UUID
        }
        job.messagingService = messagingService
        job
    }

    def "execute heartbeat on cluster mode"() {
        given:
        def job = setup(true)
        def context = Mock(JobExecutionContext)


        when:
        job.execute(context)

        then:
        notThrown(Exception)

    }
    def "execute heartbeat on other mode"() {
        given:
        def job = setup()
        def context = Mock(JobExecutionContext)


        when:
        job.execute(context)

        then:
        notThrown(Exception)

    }
}
