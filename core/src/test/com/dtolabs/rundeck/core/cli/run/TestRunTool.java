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

package com.dtolabs.rundeck.core.cli.run;
/*
* TestRunTool.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 31, 2010 4:25:06 PM
* $Id$
*/

import com.dtolabs.rundeck.core.cli.CLIToolOptionsException;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.cli.CommandLine;

public class TestRunTool extends AbstractBaseTest {
    RunTool runTool;

    public TestRunTool(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestRunTool.class);
    }

    protected void setUp()  {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testParseArgs() throws Exception {
        {
            //test invalid action
            final RunTool tool = new RunTool(getFrameworkInstance());
            try {
                final String[] args = {"invalid"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                fail("invalid action should have failed");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
                assertTrue(e.getMessage().startsWith("Invalid action"));
            }

        }
        {
            //test valid actions missing required options
            final RunTool tool = new RunTool(getFrameworkInstance());
            try {
                final String[] args = {"run"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                fail("should have thrown options exception.");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
                assertTrue(e.getMessage().endsWith("is required"));
            }
        }
        {
            //run action with conflicting options
            final RunTool tool = new RunTool(getFrameworkInstance());
            try {
                final String[] args = {"run","-i","1","-j","job"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                fail("should have thrown options exception.");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
                assertTrue("wrong message: " + e.getMessage(), e.getMessage().endsWith(
                    "cannot be combined, please specify only one."));
            }
        }
        {
            //run action with invalid ID
            final RunTool tool = new RunTool(getFrameworkInstance());
            try {
                final String[] args = {"run","-i","job"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                fail("should have thrown options exception.");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
                assertTrue(e.getMessage().endsWith("must be a valid ID number."));
            }
        }
        {
            //valid options
            final RunTool tool = new RunTool(getFrameworkInstance());
            try {
                final String[] args = {"run","-i","1"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);

            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }
        }
        {
            //valid options
            final RunTool tool = new RunTool(getFrameworkInstance());
            try {
                final String[] args = {"run","-i","1","--","some","other","args"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }
        }

        

    }
}