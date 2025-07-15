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

package com.dtolabs.rundeck.core.cli;
/*
* TestCLIUtils.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jun 3, 2010 12:55:40 PM
* $Id$
*/

import com.dtolabs.rundeck.core.utils.Converter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@RunWith(JUnit4.class)
public class CLIUtilsTest {

    @Test
    public void testGenerateArglineUnsafe() {
        assertEquals("test 1 2", CLIUtils.generateArgline("test", new String[]{"1", "2"}, true), "invalid");
        assertEquals("test 1 2 '3 4'", CLIUtils.generateArgline("test", new String[]{"1", "2", "3 4"}, true), "invalid");
        assertEquals("test 1 2 '\"3 4\"'", CLIUtils.generateArgline("test", new String[]{"1", "2", "\"3 4\""}, true), "invalid");
        assertEquals("test 1 2 \"34\"", CLIUtils.generateArgline("test", new String[]{"1", "2", "\"34\""}, true), "invalid");
        assertEquals("test 1 2 '3 4'", CLIUtils.generateArgline("test", new String[]{"1", "2", "'3 4'"}, true), "invalid");
        // test empty and null values
        assertEquals("test", CLIUtils.generateArgline("test", null, true), "invalid");
        assertEquals("test", CLIUtils.generateArgline("test", new String[0], true), "invalid");
        // demonstrate _why_ this version is unsafe
        assertEquals("test rm * && do\tthings\t>/etc/passwd",
                CLIUtils.generateArgline("test", new String[]{"rm", "*", "&&", "do\tthings\t>/etc/passwd"}, true), "invalid");
    }

    @Test
    public void testGenerateArglineSafe() {
        assertEquals("test 1 2", CLIUtils.generateArgline("test", new String[]{"1", "2"}, false), "invalid");
        assertEquals("test 1 2 '3 4'", CLIUtils.generateArgline("test", new String[]{"1", "2", "3 4"}, false), "invalid");
        assertEquals("test 1 2 '\"3 4\"'", CLIUtils.generateArgline("test", new String[]{"1", "2", "\"3 4\""}, false), "invalid");
        assertEquals("test 1 2 '\"34\"'", CLIUtils.generateArgline("test", new String[]{"1", "2", "\"34\""}, false), "invalid");
        assertEquals("test 1 2 ''\"'\"'3 4'\"'\"''", CLIUtils.generateArgline("test", new String[]{"1", "2", "'3 4'"}, false), "invalid");
        //test empty and null values
        assertEquals("test", CLIUtils.generateArgline("test", null, false), "invalid");
        assertEquals("test", CLIUtils.generateArgline("test", new String[0], false), "invalid");
        assertEquals("test", CLIUtils.generateArgline("test", new String[0], false), "invalid");
        // demonstrate _why_ this version is safe
        assertEquals("test rm '*' '&&' 'do\tthings\t>/etc/passwd'",
                CLIUtils.generateArgline("test", new String[]{"rm", "*", "&&", "do\tthings\t>/etc/passwd"}, false), "invalid");
    }

    @Test
    public void testContainsWhitespace() {
        assertFalse(CLIUtils.containsSpace(""));
        assertFalse(CLIUtils.containsSpace("asdf1234"));
        assertTrue(CLIUtils.containsSpace("asdf123 4"));
        assertTrue(CLIUtils.containsSpace("   "));
        assertFalse(CLIUtils.containsSpace("asdf123\t4"));
        assertFalse(CLIUtils.containsSpace("asdf123\n4"));
        assertFalse(CLIUtils.containsSpace("asdf123\r4"));
    }

    @Test
    public void testArgumentQuoteForOperatingSystem() {
        // Unix case
        Converter<String, String> unixConverter = CLIUtils.argumentQuoteForOperatingSystem("unix", null);
        assertEquals("'foo bar'", unixConverter.convert("foo bar"));
        assertEquals("'foo&bar'", unixConverter.convert("foo&bar"));
        assertEquals("'`foobar`'", unixConverter.convert("`foobar`"));

        // Windows CMD case
        Converter<String, String> windowsCmdConverter = CLIUtils.argumentQuoteForOperatingSystem("windows", "cmd");
        assertEquals("^&foo^|bar", windowsCmdConverter.convert("&foo|bar"));
        assertEquals("foo^&bar", windowsCmdConverter.convert("foo&bar"));
        assertEquals("^`foobar^`", windowsCmdConverter.convert("`foobar`"));

        // Windows PowerShell case (should use WINDOWS_ARGUMENT_QUOTE)
        Converter<String, String> windowsPsConverter = CLIUtils.argumentQuoteForOperatingSystem("windows", "powershell");
        assertEquals("'foo bar'", windowsPsConverter.convert("foo bar"));
        assertEquals("'foo&bar'", windowsPsConverter.convert("foo&bar"));
        assertEquals("'`foobar`'", windowsPsConverter.convert("`foobar`"));
    }
}
