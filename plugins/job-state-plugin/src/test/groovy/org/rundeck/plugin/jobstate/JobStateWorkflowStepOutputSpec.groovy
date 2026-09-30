/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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
import com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtility
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.PluginLogger
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import spock.lang.Specification

/**
 * Covers the {@code @PluginOutput} exposure of the state the referenced job was actually found in,
 * so a later conditional step can branch on it.
 */
class JobStateWorkflowStepOutputSpec extends Specification {

    private static Property outputProperty(String name) {
        return PluginAdapterUtility
                .buildDescription(new JobStateWorkflowStep(), DescriptionBuilder.builder())
                .getProperties()
                .find { it.name == name }
    }

    def "observedExecutionState is declared as output metadata"() {
        when:
        Property property = outputProperty("observedExecutionState")

        then:
        property != null
        property.outputMetadata.size() == 1
        property.outputMetadata[0].group == "data"
        property.outputMetadata[0].name == "observedExecutionState"
        !property.outputMetadata[0].description.isEmpty()
    }

    def "observedExecutionState is output-only, never a job-configurable input"() {
        expect:
        outputProperty("observedExecutionState").isOutputOnly()
    }

    def "no configuration property is exposed as an output"() {
        given:
        def description = PluginAdapterUtility
                .buildDescription(new JobStateWorkflowStep(), DescriptionBuilder.builder())

        expect:
        description.getProperties().findAll { it.getOutputMetadata() }*.name == ["observedExecutionState"]
    }

    def "captures the state the job was actually found in, even when the assertion does not hold"() {
        given:
        def step = new JobStateWorkflowStep()
        step.halt = false
        step.fail = false
        step.jobUUID = 'auuid'
        step.executionState = 'succeeded'

        def context = Mock(PluginStepContext)

        when:
        step.executeStep(context, [:])

        then:
        1 * context.getFrameworkProject() >> 'projectName'
        1 * context.getExecutionContext() >> Mock(ExecutionContext) {
            getJobService() >> Mock(JobService) {
                1 * jobForID('auuid', _) >> Mock(JobReference)
                1 * getJobState(_) >> Mock(JobState) {
                    getPreviousExecutionState() >> 'failed'
                    getPreviousExecutionStatusString() >> 'failed'
                }
            }
        }
        1 * context.getLogger() >> Mock(PluginLogger)
        context.getFlowControl() >> Mock(FlowControl)

        and: 'the observed state is exposed, not the asserted one'
        step.observedExecutionState == 'failed'
    }
}
