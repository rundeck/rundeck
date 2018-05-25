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

package com.dtolabs.rundeck.core.execution.workflow;

import junit.framework.TestCase;

/**
 * $INTERFACE is ... User: greg Date: 10/18/13 Time: 4:03 PM
 */
public class StepContextWorkflowExecutionListenerTest extends TestCase {
    StepContextWorkflowExecutionListener<String, Integer> test1;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        test1 = new StepContextWorkflowExecutionListener<String, Integer>();
    }

    public void testempty() {
        assertContext(null);
    }

    public void testBegin() {
        test1.beginContext();
        assertContext(null);
    }

    public void testBeginFinish() {
        test1.beginContext();
        assertContext(null);

        test1.finishContext();
        assertContext(null);
    }

    public void testBeginStep() {
        test1.beginContext();
        assertContext(null);

        test1.beginStepContext(1);
        assertContext(null, 1);
    }

    public void testBeginStepFinish() {
        test1.beginContext();
        assertContext(null);

        test1.beginStepContext(1);
        assertContext(null, 1);

        test1.finishStepContext();
        assertContext(null);
    }

    public void testBeginStepNode() {
        test1.beginContext();
        test1.beginStepContext(1);
        test1.beginNodeContext("abc");
        assertContext("abc", 1);
    }

    public void testBeginStepNodeFinish() {
        test1.beginContext();
        test1.beginStepContext(1);
        test1.beginNodeContext("abc");
        test1.finishNodeContext();
        assertContext(null, 1);
    }

    public void testBeginStepNodeFinishBoth() {
        test1.beginContext();
        assertContext(null);

        test1.beginStepContext(1);
        assertContext(null, 1);

        test1.beginNodeContext("abc");
        assertContext("abc", 1);

        test1.finishNodeContext();
        assertContext(null, 1);

        test1.finishStepContext();
        assertContext(null);
    }

    public void testBeginStepNodeSub() {
        test1.beginContext();
        test1.beginStepContext(1);
        test1.beginNodeContext("abc");
        assertContext("abc", 1);

        test1.beginContext();
        assertContext(null, 1);

        test1.beginStepContext(1);
        //sub context 1,1
        assertContext(null, 1, 1);

        test1.finishStepContext();
        assertContext(null, 1);

        test1.finishContext();
        assertContext("abc", 1);

        test1.finishNodeContext();
        assertContext(null, 1);

        test1.finishStepContext();
        assertContext(null);

        test1.finishContext();
        assertContext(null);
    }

    private void assertContext(String node, int... context) {
        assertEquals(node, test1.getCurrentNode());
        if (null == context || context.length == 0) {
            assertEquals(null, test1.getCurrentContext());
        } else {
            assertNotNull(test1.getCurrentContext());
            assertEquals(context.length, test1.getCurrentContext().size());
            for (int v = 0; v < context.length; v++) {
                assertEquals(context[v], (int) test1.getCurrentContext().get(v));
            }
        }
    }
}
