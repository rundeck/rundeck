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
* TestFormattedOutputStream.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: May 27, 2010 11:36:00 AM
* $Id$
*/

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class TestFormattedOutputStream extends TestCase {
    FormattedOutputStream formattedOutputStream;

    public TestFormattedOutputStream(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestFormattedOutputStream.class);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testWriteEmptyContext() throws Exception {
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            formattedOutputStream = new FormattedOutputStream(new LogReformatter(
                "%node : %user : %command : %level : %message", (Map<String, String>) null), baos);

            formattedOutputStream.setContext("node", "test1");
            try {
                formattedOutputStream.write("This is a test\n".getBytes());
            }
            catch (IOException ex) {
                fail("Should not have thrown an Exception: " + ex.getMessage());
            }
            assertEquals("test1 :  :  : null : This is a test\n",baos.toString());
        }
    }

    public void testWrite() throws Exception {
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            formattedOutputStream = new FormattedOutputStream(new LogReformatter(
                "%node : %user : %command : %level : %message", (Map<String, String>) null), baos);

            formattedOutputStream.setContext("node", "test1");
            formattedOutputStream.setContext("user", "test2");
            formattedOutputStream.setContext("command", "test3");
            formattedOutputStream.setContext("level", "test4");
            try {
                formattedOutputStream.write("This is a test5\n".getBytes());
            }
            catch (IOException ex) {
                fail("Should not have thrown an Exception: " + ex.getMessage());
            }
            assertEquals("test1 : test2 : test3 : test4 : This is a test5\n",baos.toString());
        }
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            formattedOutputStream = new FormattedOutputStream(new LogReformatter(
                "%command: %message [%level]", (Map<String, String>) null), baos);

            formattedOutputStream.setContext("command", "testcommand");
            formattedOutputStream.setContext("level", "testlevel");
            try {
                formattedOutputStream.write("This is a test5\n".getBytes());
            }
            catch (IOException ex) {
                fail("Should not have thrown an Exception: " + ex.getMessage());
            }
            assertEquals("testcommand: This is a test5 [testlevel]\n",baos.toString());
        }
    }
}