/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* TestExecutionContextImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/17/12 10:09 AM
* 
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;


/**
 * TestExecutionContextImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestExecutionContextImpl extends TestCase {
    public void testSingleNodeContext() {

        Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
        Map<String, String> ctx1 = new HashMap<String, String>();
        ctx1.put("test", "value");
        map.put("ctx1", ctx1);
        ExecutionContextImpl imp =
            ExecutionContextImpl.builder().dataContext(map).build();

        assertNotNull(imp.getDataContext());
        assertNull(imp.getDataContext().get("node"));
        assertNotNull(imp.getDataContext().get("ctx1"));
        assertEquals("value", imp.getDataContext().get("ctx1").get("test"));

        NodeEntryImpl testNode = new NodeEntryImpl("testNode");
        testNode.setDescription("desc 1");
        testNode.setHostname("host1");
        testNode.setUsername("user1");


        ExecutionContextImpl imp2 =
            ExecutionContextImpl.builder(imp).singleNodeContext(testNode, true).build();

        assertNotNull(imp2.getDataContext());
        assertNotNull(imp2.getDataContext().get("node"));
        assertEquals("desc 1", imp2.getDataContext().get("node").get("description"));
        assertEquals("host1", imp2.getDataContext().get("node").get("hostname"));
        assertEquals("testNode", imp2.getDataContext().get("node").get("name"));
        assertNotNull(imp2.getDataContext().get("ctx1"));
        assertEquals("value", imp2.getDataContext().get("ctx1").get("test"));
    }
}
