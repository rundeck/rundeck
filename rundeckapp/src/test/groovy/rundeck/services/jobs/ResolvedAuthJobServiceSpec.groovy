/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package rundeck.services.jobs

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.jobs.JobExecutionError
import com.dtolabs.rundeck.core.jobs.JobNotFound
import com.dtolabs.rundeck.core.jobs.JobService
import grails.testing.services.ServiceUnitTest
import rundeck.services.JobReferenceImpl
import rundeck.services.execution.ExecutionReferenceImpl
import spock.lang.Specification

class ResolvedAuthJobServiceSpec extends Specification implements ServiceUnitTest<ResolvedAuthJobService> {

    def "run job success"() {
        given:
            service.authContext = Mock(UserAndRolesAuthContext)
            service.authJobService = Mock(AuthorizingJobService)
            def jobRef = Mock(JobService.RunJob)
        when:
            def result = service.runJob(jobRef)
        then:
            result != null
            result.id == '123'
            1 * service.authJobService.runJob(service.authContext, jobRef) >>
            new ExecutionReferenceImpl(id: '123')
    }

    def "run job not found"() {
        given:
            service.authContext = Mock(UserAndRolesAuthContext)
            service.authJobService = Mock(AuthorizingJobService)
            def jobRef = Mock(JobService.RunJob)
        when:
            def result = service.runJob(jobRef)
        then:
            JobNotFound expected = thrown()
            expected.jobId == '123'
            1 * service.authJobService.runJob(service.authContext, jobRef) >> {
                throw new JobNotFound("not found", "123", "asdf")
            }

    }
    def "run job error"() {
        given:
            service.authContext = Mock(UserAndRolesAuthContext)
            service.authJobService = Mock(AuthorizingJobService)
            def jobRef = Mock(JobService.RunJob)
        when:
            def result = service.runJob(jobRef)
        then:
            result==null
            JobExecutionError expected = thrown()

            1 * service.authJobService.runJob(service.authContext, jobRef) >> {
                throw new JobExecutionError("error", "123", "asdf")
            }

    }
}
