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

package com.dtolabs.rundeck.core.cli;
/*
* TestCLIUtils.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jun 3, 2010 12:55:40 PM
* $Id$
*/

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;

public class TestCLIUtils extends TestCase {
    CLIUtils cliUtils;

    public TestCLIUtils(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestCLIUtils.class);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testSplitArgLine() throws Exception {

        assertEquals("incorrect argline", Arrays.asList( "-test1"),
            CLIUtils.splitArgLine("-test1"));

        assertEquals("incorrect argline",
            Arrays.asList("-test1", "something"),
            CLIUtils.splitArgLine("-test1 something"));

        assertEquals("incorrect argline",
            Arrays.asList("-test1", "something else"),
            CLIUtils.splitArgLine("-test1 \"something else\""));

        assertEquals("incorrect argline",
            Arrays.asList("-test1", "something else"),
            CLIUtils.splitArgLine("-test1 'something else'"));

        assertEquals("incorrect argline",
            Arrays.asList("-test1", "something else","-test2","another","-test3","what if"), 
            CLIUtils.splitArgLine("-test1 'something else' -test2 another -test3 \"what if\""));

        //test element with embedded quote
        assertEquals("incorrect argline",
            Arrays.asList("-test1", "value","--something='else'"),
            CLIUtils.splitArgLine("-test1 value --something='else'"));
        assertEquals("incorrect argline",
            Arrays.asList("-test1", "value","--something='else'"),
            CLIUtils.splitArgLine("-test1 value \"--something='else'\""));
        assertEquals("incorrect argline",
            Arrays.asList("-test1", "value","--something=\"else\""),
            CLIUtils.splitArgLine("-test1 value '--something=\"else\"'"));
    }
}