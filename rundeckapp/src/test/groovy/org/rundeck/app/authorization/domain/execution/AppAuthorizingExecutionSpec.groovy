/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.authorization.domain.execution

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ProjectResIdentifier
import rundeck.Execution
import rundeck.ScheduledExecution
import spock.lang.Specification

import javax.security.auth.Subject

class AppAuthorizingExecutionSpec extends Specification {

    private AppAuthorizingExecution makeSut(AuthContextProcessor processor) {
        new AppAuthorizingExecution(processor, new Subject(), Mock(NamedAuthProvider), Mock(ProjectResIdentifier))
    }

    def "a job-less execution is authorized against the resource its execution type resolves to"() {
        given:
            def adHocStepResource = AuthorizationUtil.resource('ad-hoc-step')
            def processor = Mock(AuthContextProcessor)
            def sut = makeSut(processor)
            def exec = new Execution(project: 'p', executionType: 'adhoc-step')
        when:
            def result = sut.getAuthResource(exec)
        then: 'the execution-type resolver decides the resource, not a hard-coded adhoc kind'
            1 * processor.authResourceForExecutionType('adhoc-step') >> adHocStepResource
            0 * processor.authResourceForJob(*_)
            result.resourceMap == adHocStepResource
    }

    def "a job-less execution falls back to the adhoc resource when the type resolves to null"() {
        given:
            def processor = Mock(AuthContextProcessor)
            def sut = makeSut(processor)
            def exec = new Execution(project: 'p', executionType: 'adhoc')
        when:
            def result = sut.getAuthResource(exec)
        then:
            1 * processor.authResourceForExecutionType('adhoc') >> null
            result.resourceMap == AuthConstants.RESOURCE_ADHOC
    }

    def "an execution with a job is authorized against the job resource"() {
        given:
            def jobResource = [type: 'job', name: 'n', group: 'g', uuid: 'u']
            def processor = Mock(AuthContextProcessor)
            def sut = makeSut(processor)
            def job = new ScheduledExecution(jobName: 'n', groupPath: 'g', uuid: 'u')
            def exec = new Execution(project: 'p', scheduledExecution: job)
        when:
            def result = sut.getAuthResource(exec)
        then:
            1 * processor.authResourceForJob('n', 'g', 'u') >> jobResource
            0 * processor.authResourceForExecutionType(_)
            result.resourceMap == jobResource
    }
}
