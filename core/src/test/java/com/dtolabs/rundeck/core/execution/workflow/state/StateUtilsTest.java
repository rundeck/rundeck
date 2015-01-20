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

package com.dtolabs.rundeck.core.execution.workflow.state;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;

import static com.dtolabs.rundeck.core.execution.workflow.state.StateUtils.isContainedStep;
import static com.dtolabs.rundeck.core.execution.workflow.state.StateUtils.stepContextId;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * StateUtilsTest is ...
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-07-01
 */
@RunWith(JUnit4.class)
public class StateUtilsTest {

    @Test
    public void testIsContainedStepForErrorHandler() {
        assertTrue(isContainedStep(stepContextId(1, false), stepContextId(1, false)));
        assertTrue(isContainedStep(stepContextId(1, false), stepContextId(1, true)));
        assertTrue(isContainedStep(stepContextId(1, true), stepContextId(1, true)));
        assertFalse(isContainedStep(stepContextId(1, true), stepContextId(1, false)));
    }

    @Test
    public void testIsContainedStepForIdent() {
        assertTrue(isContainedStep(stepContextId(1, false), stepContextId(1, false)));
        assertFalse(isContainedStep(stepContextId(2, false), stepContextId(1, true)));
        assertFalse(isContainedStep(stepContextId(1, false), stepContextId(2, true)));
    }

    @Test
    public void testIsContainedStepForParams() {
        assertTrue(isContainedStep(stepContextId(1, false), stepContextId(1, false, map("a", "b"))));
        assertTrue(isContainedStep(stepContextId(1, false, map("a", "b")), stepContextId(1, false, map("a", "b"))));
        assertTrue(isContainedStep(stepContextId(1, false, map("x","y","a", "b")), stepContextId(1, false, map("a",
                "b","x","y"))));

        assertFalse(isContainedStep(stepContextId(1, false, map("a", "b")), stepContextId(1, false)));
    }
    @Test
    public void teststepContextIdFromString() {
        assertStepContextIdFromString("1", 1, false, null);
        assertStepContextIdFromString("2", 2, false, null);
        assertStepContextIdFromString("56", 56, false, null);
        assertStepContextIdFromString("1e", 1, true, null);
        assertStepContextIdFromString("2e", 2, true, null);
        assertStepContextIdFromString("56e", 56, true, null);
        assertStepContextIdFromString("1@a=b", 1, false, map("a","b"));
        assertStepContextIdFromString("1@a=b,c=d", 1, false, map("a","b","c","d"));
    }

    private void assertStepContextIdFromString(String string, int step, boolean eh, Map<String,String> params) {
        StepContextId stepContextId = StateUtils.stepContextIdFromString(string);
        assertEquals(step, stepContextId.getStep());
        assertEquals((!eh) ? StepAspect.Main: StepAspect.ErrorHandler, stepContextId.getAspect());
        assertEquals(params, stepContextId.getParams());
    }

    @Test
    public void testIsMatchedIdentifier() {
        assertTrue(StateUtils.isMatchedIdentifier("1@node=a1/1", "1@node=a1/1@node=x", false));
        assertFalse(StateUtils.isMatchedIdentifier("1@node=a1/1", "1@node=b1/1@node=x", false));
    }

    private static Map<String, String> map(String... vals) {
        HashMap<String, String> map = new HashMap<String, String>();
        String key = null;
        for (int i = 0; i < vals.length; i++) {
            String val = vals[i];
            if (key != null) {
                map.put(key, val);
                key = null;
            } else {
                key = val;
            }
        }
        return map;
    }
}
