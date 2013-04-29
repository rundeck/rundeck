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



    public void testGenerateArglineUnsafe() throws Exception {
        assertEquals("invalid", "test 1 2", CLIUtils.generateArgline("test", new String[]{"1", "2"}, true));
        assertEquals("invalid", "test 1 2 '3 4'", CLIUtils.generateArgline("test", new String[]{"1", "2", "3 4"}, true));
        assertEquals("invalid", "test 1 2 '\"3 4\"'", CLIUtils.generateArgline("test", new String[]{"1", "2", "\"3 4\""}, true));
        assertEquals("invalid", "test 1 2 \"34\"", CLIUtils.generateArgline("test", new String[]{"1", "2", "\"34\""}, true));
        assertEquals("invalid", "test 1 2 '3 4'", CLIUtils.generateArgline("test", new String[]{"1", "2", "'3 4'"}, true));
        // test empty and null values
        assertEquals("invalid", "test", CLIUtils.generateArgline("test", null, true));
        assertEquals("invalid", "test", CLIUtils.generateArgline("test", new String[0], true));
        // demonstrate _why_ this version is unsafe
        assertEquals("invalid",
                "test rm * && do\tthings\t>/etc/passwd",
                CLIUtils.generateArgline("test", new String[]{"rm", "*", "&&", "do\tthings\t>/etc/passwd"}, true));
    }

    public void testGenerateArglineSafe() throws Exception {
        assertEquals("invalid", "test 1 2", CLIUtils.generateArgline("test", new String[]{"1", "2"}, false));
        assertEquals("invalid", "test 1 2 '3 4'", CLIUtils.generateArgline("test", new String[]{"1", "2", "3 4"}, false));
        assertEquals("invalid", "test 1 2 '\"3 4\"'", CLIUtils.generateArgline("test", new String[]{"1", "2", "\"3 4\""}, false));
        assertEquals("invalid", "test 1 2 '\"34\"'", CLIUtils.generateArgline("test", new String[]{"1", "2", "\"34\""}, false));
        assertEquals("invalid", "test 1 2 ''\"'\"'3 4'\"'\"''", CLIUtils.generateArgline("test", new String[]{"1", "2", "'3 4'"}, false));
        //test empty and null values
        assertEquals("invalid", "test", CLIUtils.generateArgline("test", null, false));
         assertEquals("invalid", "test", CLIUtils.generateArgline("test", new String[0], false));
        // demonstrate _why_ this version is safe
        assertEquals("invalid",
                "test rm '*' '&&' 'do\tthings\t>/etc/passwd'",
                CLIUtils.generateArgline("test", new String[]{"rm", "*", "&&", "do\tthings\t>/etc/passwd"}, false));
    }
    public void testContainsWhitespace() throws Exception {
        assertFalse("invalid",  CLIUtils.containsSpace(""));
        assertFalse("invalid",  CLIUtils.containsSpace("asdf1234"));
        assertTrue("invalid", CLIUtils.containsSpace("asdf123 4"));
        assertTrue("invalid", CLIUtils.containsSpace("   "));
        assertFalse("invalid", CLIUtils.containsSpace("asdf123\t4"));
        assertFalse("invalid", CLIUtils.containsSpace("asdf123\n4"));
        assertFalse("invalid", CLIUtils.containsSpace("asdf123\r4"));
    }
}
