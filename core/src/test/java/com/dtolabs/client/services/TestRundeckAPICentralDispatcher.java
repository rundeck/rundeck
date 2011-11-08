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


    public void testAddNodeSetParams() throws Exception {
        // 16 parameters: 14 exclude/include params + nodexcludeprecedence + threadcount
        final int PARAM_COUNT = 16;
        {
            //test null nodeset
            HashMap<String, String> params = new HashMap<String, String>();
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, null, null);
            assertEquals(0, params.size());
        }
        {
            //test empty nodeset, only has threadcount & keepgoing
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, nodeset, false);
            assertEquals(2, params.size());
            assertTrue(params.containsKey("nodeThreadcount"));
            assertEquals("1", params.get("nodeThreadcount"));
            assertTrue(params.containsKey("nodeKeepgoing"));
            assertEquals("false", params.get("nodeKeepgoing"));

        }
        {
            //test empty nodeset, only has threadcount & keepgoing
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            nodeset.setThreadCount(2);
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, nodeset, true);
            assertEquals(2, params.size());
            assertTrue(params.containsKey("nodeThreadcount"));
            assertEquals("2", params.get("nodeThreadcount"));
            assertTrue(params.containsKey("nodeKeepgoing"));
            assertEquals("true", params.get("nodeKeepgoing"));

        }
        {
            //test basic hostname
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            nodeset.setThreadCount(1);
            final NodeSet.Include include = nodeset.createInclude();
            include.setHostname("testhostname1");
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, nodeset, false);
            assertEquals("incorrect size: " + params, PARAM_COUNT + 1 /* keepgoing==false */, params.size());
            assertTrue(params.containsKey("nodeThreadcount"));
            assertEquals("1", params.get("nodeThreadcount"));
            assertTrue(params.containsKey("nodeKeepgoing"));
            assertEquals("false", params.get("nodeKeepgoing"));
            assertTrue(params.containsKey("hostname"));
            assertEquals("testhostname1", params.get("hostname"));
        }
        {
            //test threadcount/keepgoing changes
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            final NodeSet.Include include = nodeset.createInclude();
            include.setHostname("testhostname1");
            nodeset.setThreadCount(2);
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, nodeset, true);
            assertEquals("incorrect size: " + params, PARAM_COUNT + 1 /* keepgoing==true */, params.size());
            assertTrue(params.containsKey("nodeThreadcount"));
            assertEquals("2", params.get("nodeThreadcount"));
            assertTrue(params.containsKey("nodeKeepgoing"));
            assertEquals("true", params.get("nodeKeepgoing"));
        }
        {
            //test include filters
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            final NodeSet.Include include = nodeset.createInclude();
            include.setHostname("testhostname1");
            include.setOsfamily("testosfamily");
            include.setOsname("testosname");
            include.setOsversion("test1234");
            include.setOsarch("testosarch");
            include.setName("testname");
            include.setTags("testtags");

            //test other include filters
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, nodeset, null);

            assertEquals("incorrect size: " + params, PARAM_COUNT, params.size());
            assertEquals("testhostname1", params.get("hostname"));
            assertEquals("testosfamily", params.get("os-family"));
            assertEquals("testosname", params.get("os-name"));
            assertEquals("test1234", params.get("os-version"));
            assertEquals("testosarch", params.get("os-arch"));
            assertEquals("testname", params.get("name"));
            assertEquals("testtags", params.get("tags"));
        }
        {
            //test exclude filters
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            final NodeSet.Exclude exclude = nodeset.createExclude();
            exclude.setHostname("testhostname1");
            exclude.setOsfamily("testosfamily");
            exclude.setOsname("testosname");
            exclude.setOsversion("test1234");
            exclude.setOsarch("testosarch");
            exclude.setName("testname");
            exclude.setTags("testtags");

            //test other include filters
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, nodeset, null);
            assertEquals("incorrect size: " + params, PARAM_COUNT, params.size());
            assertEquals("testhostname1", params.get("exclude-hostname"));
            assertEquals("testosfamily", params.get("exclude-os-family"));
            assertEquals("testosname", params.get("exclude-os-name"));
            assertEquals("test1234", params.get("exclude-os-version"));
            assertEquals("testosarch", params.get("exclude-os-arch"));
            assertEquals("testname", params.get("exclude-name"));
            assertEquals("testtags", params.get("exclude-tags"));
        }
        {
            //test precedence filters
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            final NodeSet.Exclude exclude = nodeset.createExclude();
            final NodeSet.Include include = nodeset.createInclude();
            exclude.setHostname("testhostname1");
            include.setHostname("testhostname2");
            exclude.setDominant(true);


            //test other include filters
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, nodeset, null);
            assertEquals("incorrect size: " + params, PARAM_COUNT, params.size());
            assertEquals("testhostname1", params.get("exclude-hostname"));
            assertEquals("testhostname2", params.get("hostname"));
            assertEquals("true", params.get("exclude-precedence"));
        }
        {
            //test precedence filters
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            final NodeSet.Exclude exclude = nodeset.createExclude();
            final NodeSet.Include include = nodeset.createInclude();
            exclude.setHostname("testhostname1");
            include.setHostname("testhostname2");
            include.setDominant(true);


            //test other include filters
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, nodeset, null);
            assertEquals("incorrect size: " + params, PARAM_COUNT, params.size());
            assertEquals("testhostname1", params.get("exclude-hostname"));
            assertEquals("testhostname2", params.get("hostname"));
            assertEquals("false", params.get("exclude-precedence"));
        }
        {
            //test precedence filters
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            final NodeSet.Exclude exclude = nodeset.createExclude();
            final NodeSet.Include include = nodeset.createInclude();
            exclude.setHostname("testhostname1");
            include.setHostname("testhostname2");
            exclude.setDominant(true);
            include.setDominant(true);


            //test other include filters
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, nodeset, null);
            assertEquals("incorrect size: " + params, PARAM_COUNT, params.size());
            assertEquals("testhostname1", params.get("exclude-hostname"));
            assertEquals("testhostname2", params.get("hostname"));
            assertEquals("true", params.get("exclude-precedence"));
        }
    }
}