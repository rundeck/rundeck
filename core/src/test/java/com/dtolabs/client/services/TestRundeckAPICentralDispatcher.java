/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.client.services;
/*
* TestRundeckAPICentralDispatcher.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 31, 2010 11:18:40 AM
* $Id$
*/

import com.dtolabs.rundeck.core.utils.NodeSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashMap;


public class TestRundeckAPICentralDispatcher extends TestCase {
    RundeckAPICentralDispatcher rundeckCentralDispatcher;


    public TestRundeckAPICentralDispatcher(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestRundeckAPICentralDispatcher.class);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testAddNodeSetParamsEmpty() throws Exception {
            //test null nodeset
            HashMap<String, String> params = new HashMap<String, String>();
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, null, null, -1, null);
            assertEquals(0, params.size());
        }

    public void testAddNodeSetParamsThreadcount() throws Exception {
            //test empty nodeset, only has threadcount & keepgoing
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, null, null, 1, null);
            assertEquals(1, params.size());
            assertTrue(params.containsKey("nodeThreadcount"));
            assertEquals("1", params.get("nodeThreadcount"));

        }
    public void testAddNodeSetParamsKeepgoing() throws Exception {
            //test empty nodeset, only has threadcount & keepgoing
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, false, null, -1, null);
            assertEquals(1, params.size());
            assertTrue(params.containsKey("nodeKeepgoing"));
            assertEquals("false", params.get("nodeKeepgoing"));
        }

    public void testAddNodeSetParamsBasic2() throws Exception {
            //test empty nodeset, only has threadcount & keepgoing
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            nodeset.setThreadCount(2);
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, true, null, 2, null);
            assertEquals(2, params.size());
            assertTrue(params.containsKey("nodeThreadcount"));
            assertEquals("2", params.get("nodeThreadcount"));
            assertTrue(params.containsKey("nodeKeepgoing"));
            assertEquals("true", params.get("nodeKeepgoing"));

        }

    public void testAddNodeSetParamsFilter() throws Exception {
            //test basic hostname
            HashMap<String, String> params = new HashMap<String, String>();
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, false, "hostname: testhostname1", 1, null);
            assertEquals("incorrect size: " + params, 3 /* keepgoing==false */, params.size());
            assertTrue(params.containsKey("nodeThreadcount"));
            assertEquals("1", params.get("nodeThreadcount"));
            assertTrue(params.containsKey("nodeKeepgoing"));
            assertEquals("false", params.get("nodeKeepgoing"));
            assertTrue(params.containsKey("filter"));
            assertEquals("hostname: testhostname1", params.get("filter"));
        }


    /**
     * blank filter string should not be included
     * @throws Exception
     */
    public void testAddNodeSetParamsBlankFilter() throws Exception {
            //test basic hostname
            HashMap<String, String> params = new HashMap<String, String>();
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, false, "", 1, null);
            assertEquals("incorrect size: " + params, 2 /* keepgoing==false */, params.size());
            assertTrue(params.containsKey("nodeThreadcount"));
            assertEquals("1", params.get("nodeThreadcount"));
            assertTrue(params.containsKey("nodeKeepgoing"));
            assertEquals("false", params.get("nodeKeepgoing"));
            assertFalse(params.containsKey("filter"));
        }


    /**
     * precedence value should only be included if filter is included
     * @throws Exception
     */
    public void testAddNodeSetParamsNoPrecedenceWithoutFilter() throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        RundeckAPICentralDispatcher.addAPINodeSetParams(params, null, null, -1, true);
        assertEquals("incorrect size: " + params, 0 /* keepgoing==false */, params.size());
    }


    public void testAddNodeSetParamsFiltersPrecedence() throws Exception {
            //test precedence filters
            HashMap<String, String> params = new HashMap<String, String>();
            //test other include filters
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, null, "blah", -1, true);
            assertEquals("incorrect size: " + params, 2, params.size());
            assertEquals("blah", params.get("filter"));
            assertEquals("true", params.get("exclude-precedence"));
        }

    public void testAddNodeSetParamsFiltersPrecedence2() throws Exception {
        //test precedence filters
        HashMap<String, String> params = new HashMap<String, String>();

        //test other include filters
        RundeckAPICentralDispatcher.addAPINodeSetParams(params, null, "blah", -1, false);
        assertEquals("incorrect size: " + params, 2, params.size());
        assertEquals("blah", params.get("filter"));
        assertEquals("false", params.get("exclude-precedence"));
    }
}
