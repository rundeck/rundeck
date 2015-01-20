/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.app.internal.workflow

import grails.test.mixin.support.GrailsUnitTestMixin;

import com.dtolabs.rundeck.core.execution.workflow.state.StateUtils

/**
 * MutableWorkflowStepStateImplTest is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-10-24
 */

@TestMixin(GrailsUnitTestMixin)
class MutableWorkflowStepStateImplTest {
    public void testGetParameterizedStepState_withSubworkflow() {
        def mutableWorkflow = new MutableWorkflowStateImpl(['a'], 2)

        MutableWorkflowStepState test = new MutableWorkflowStepStateImpl(
                StateUtils.stepIdentifier(1),
                mutableWorkflow
        )
        def identifier = StateUtils.stepIdentifier(StateUtils
                .stepContextIdFromString("1@node=test1"))
        def resultState = test.getParameterizedStepState(identifier, [node: 'test1'])
        assertNotNull(resultState)
        assertEquals(test, resultState.ownerStepState)
        assertEquals(identifier, resultState.stepIdentifier)
        assertEquals(['a'], resultState.mutableSubWorkflowState.nodeSet)
        assertEquals(2, resultState.mutableSubWorkflowState.stepCount)

    }
    public void testGetParameterizedStepState_withoutSubworkflow() {
        MutableWorkflowStepState test = new MutableWorkflowStepStateImpl(
                StateUtils.stepIdentifier(1)
        )
        def identifier = StateUtils.stepIdentifier(StateUtils
                .stepContextIdFromString("1@node=test1"))
        def resultState = test.getParameterizedStepState(identifier, [node: 'test1'])
        assertNotNull(resultState)
        assertEquals(test, resultState.ownerStepState)
        assertEquals(identifier, resultState.stepIdentifier)
        assertEquals([], resultState.mutableSubWorkflowState.nodeSet)
        assertEquals(1, resultState.mutableSubWorkflowState.stepCount)
    }
}
