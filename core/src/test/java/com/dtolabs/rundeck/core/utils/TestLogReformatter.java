/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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

package com.dtolabs.rundeck.core.utils;
/*
* TestLogReformatter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: May 27, 2010 11:20:00 AM
* $Id$
*/

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashMap;
import java.util.Map;

public class TestLogReformatter extends TestCase {
    LogReformatter logReformatter;

    public TestLogReformatter(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestLogReformatter.class);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testReformatEmptyContext() throws Exception {
        { //no context data
            LogReformatter reformat = new LogReformatter("%node", (Map<String,String>)null);
            assertEquals("", reformat.reformat(null, "test"));
        }
    }

    public void testReformatEmptyContextMulti() throws Exception {
        { //no context data
            LogReformatter reformat = new LogReformatter("%node%user%level", (Map<String,String>)null);
            assertEquals("null", reformat.reformat(null, "test"));
        }
    }

    public void testReformatNoContextMessage() throws Exception {
        { //no context data, include message
            LogReformatter reformat = new LogReformatter("%node%user%level%message", (Map<String,String>)null);
            assertEquals("nulltest", reformat.reformat(null, "test"));
        }
    }

    public void testReformatOnlyNode() throws Exception {
        { //add context data
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("node", "test1");
            LogReformatter reformat = new LogReformatter("%node:%user:%level:%message", data);
            assertEquals("test1::null:test", reformat.reformat(null, "test"));
        }
    }

    public void testReformatOnlyUser() throws Exception {
        { //add context data
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("user", "test2");
            LogReformatter reformat = new LogReformatter("%node:%user:%level:%message", data);
            assertEquals(":test2:null:test", reformat.reformat(null, "test"));
        }
    }

    public void testReformatOnlyLevel() throws Exception {
        { //add context data
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("level", "test3");
            LogReformatter reformat = new LogReformatter("%node:%user:%level:%message", data);
            assertEquals("::test3:test", reformat.reformat(null, "test"));
        }
    }

    public void testReformatOnlyCommand() throws Exception {
        { //add context data
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("command", "test4");
            LogReformatter reformat = new LogReformatter("%node:%user:%level:%command:%message", data);
            assertEquals("::null:test4:test", reformat.reformat(null, "test"));
        }
    }

    public void testReformatInputLevelContext() throws Exception {

        //test direct context input to reformat

        { //add context data
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("level", "test3");
            LogReformatter reformat = new LogReformatter("%node:%user:%level:%message", (Map<String,String>)null);
            assertEquals("::test3:test", reformat.reformat(data, "test"));
        }
        //test direct context input to reformat
    }

    public void testReformatInputUserContext() throws Exception {
        { //add context data
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("user", "test2");
            LogReformatter reformat = new LogReformatter("%node:%user:%level:%message", (Map<String,String>)null);
            assertEquals(":test2:null:test", reformat.reformat(data, "test"));
        }
        //test direct context input to reformat
    }

    public void testReformatInputNodeContext() throws Exception {
        { //add context data
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("node", "test1");
            LogReformatter reformat = new LogReformatter("%node:%user:%level:%message", (Map<String,String>)null);
            assertEquals("test1::null:test", reformat.reformat(data, "test"));
        }
    }

    public void testReformatInputComandContext() throws Exception {
        //test direct context input to reformat

        { //add context data
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("command", "test4");
            LogReformatter reformat = new LogReformatter("%node:%user:%level:%command:%message", (Map<String,String>)null);
            assertEquals("::null:test4:test", reformat.reformat(data, "test"));
        }
        //test using MapGenerator
    }

    public void testReformatGenerator() throws Exception {
        { //add context data
            final HashMap<String, String> data = new HashMap<String, String>();
            data.put("node", "test1");
            data.put("user", "test2");
            data.put("level", "test3");
            data.put("command", "test4");
            LogReformatter reformat = new LogReformatter("%node:%user:%level:%command:%message", new MapGenerator<String, String>() {
                public Map<String, String> getMap() {
                    return data;
                }
            });
            assertEquals("test1:test2:test3:test4:test", reformat.reformat(null, "test"));
        }
    }
}