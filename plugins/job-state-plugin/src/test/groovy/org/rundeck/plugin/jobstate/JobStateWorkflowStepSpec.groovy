/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.plugin.jobstate

import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.FlowControl
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobService
import com.dtolabs.rundeck.core.jobs.JobState
import com.dtolabs.rundeck.plugins.PluginLogger
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 4/12/17
 */
class JobStateWorkflowStepSpec extends Specification {
    @Unroll
    def "state check #rightstate halt #halt and fail #fail should log at #loglevel"() {
        given:
        def step = new JobStateWorkflowStep()
        step.halt = halt
        step.fail = fail
        def actualState = 'failed'
        step.executionState = rightstate ? actualState : 'wrongstate'
        step.jobUUID = 'auuid'


        def context = Mock(PluginStepContext)
        def config = [:]

        when:
        step.executeStep(context, config)
        then:
        1 * context.getFrameworkProject() >> 'projectName'
        1 * context.getExecutionContext() >> Mock(ExecutionContext) {
            getJobService() >> Mock(JobService) {
                1 * jobForID('auuid', _) >> Mock(JobReference)
                1 * getJobState(_) >> Mock(JobState) {
                    getPreviousExecutionState() >> actualState
                }
            }
        }
        1 * context.getLogger() >> Mock(PluginLogger) {
            1 * log(loglevel, _)
        }
        if (!rightstate) {
            if (halt) {
                context.getFlowControl() >> Mock(FlowControl) {
                    1 * Halt(!fail)
                }
            } else {
                context.getFlowControl() >> Mock(FlowControl) {
                    1 * Continue()
                }
            }
        }


        where:
        halt  | fail  | rightstate | loglevel
        true  | false | false      | 1
        true  | true  | false      | 0
        false | true  | false      | 1
        false | true  | true       | 2


    }

    def "state check shouldn't fail with or without jobProject"() {
        given:
        def step = new JobStateWorkflowStep()
        step.halt = false
        step.fail = true
        def actualState = 'failed'
        step.executionState =  actualState
        step.jobUUID = 'auuid'
        step.jobProject = jobProject


        def context = Mock(PluginStepContext)
        def config = [:]

        when:
        step.executeStep(context, config)
        then:
        1 * context.getFrameworkProject() >> 'projectName'
        1 * context.getExecutionContext() >> Mock(ExecutionContext) {
            getJobService() >> Mock(JobService) {
                1 * jobForID('auuid', _) >> Mock(JobReference)
                1 * getJobState(_) >> Mock(JobState) {
                    getPreviousExecutionState() >> actualState
                }
            }
        }
        1 * context.getLogger() >> Mock(PluginLogger) {
            1 * log(2, _)
        }


        where:
        jobProject | _
        null       | _
        'project'  | _


    }
}
