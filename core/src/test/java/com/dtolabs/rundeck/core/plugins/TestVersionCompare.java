/*
 * Copyright 2012 DTO Solutions, Inc. (http://dtosolutions.com)
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

/*
* TestVersionCompare.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 6/20/12 5:16 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import junit.framework.TestCase;

/**
 * TestVersionCompare is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestVersionCompare extends TestCase {
    public void testComp() throws Exception {
        assertEquals(0, VersionCompare.comp(null, null, null, null));
        assertEquals(1, VersionCompare.comp(1, null, null, null));
        assertEquals(-1, VersionCompare.comp(null, null, 1, null));

        assertEquals(1, VersionCompare.comp(1, "1", null, null));
        assertEquals(-1, VersionCompare.comp(null, null, 1, "1"));

        assertEquals(0, VersionCompare.comp(1, null, 1, null));
        assertEquals(1, VersionCompare.comp(2, null, 1, null));
        assertEquals(-1, VersionCompare.comp(2, null, 3, null));

        assertEquals(0, VersionCompare.comp(null, "1", null, "1"));
        assertEquals(1, VersionCompare.comp(null, "2", null, "1"));
        assertEquals(-1, VersionCompare.comp(null, "2", null, "3"));

        assertEquals(0, VersionCompare.comp(null, "abc", null, "abc"));
        assertTrue(VersionCompare.comp(null, "def", null, "abc") > 0);
        assertTrue(VersionCompare.comp(null, "def", null, "ghi") < 0);

    }

    public void testCompareTo() throws Exception {
        assertEquals(0, VersionCompare.forString("1").compareTo(VersionCompare.forString("1")));
        assertEquals(0, VersionCompare.forString("1.1").compareTo(VersionCompare.forString("1.1")));
        assertEquals(0, VersionCompare.forString("1.1.1").compareTo(VersionCompare.forString("1.1.1")));
        assertEquals(0, VersionCompare.forString("1.1.1-blah").compareTo(VersionCompare.forString("1.1.1-blah")));

        assertEquals(-1, VersionCompare.forString("1").compareTo(VersionCompare.forString("2")));
        assertEquals(-1, VersionCompare.forString("1.1").compareTo(VersionCompare.forString("1.2")));
        assertEquals(-1, VersionCompare.forString("1.1.1").compareTo(VersionCompare.forString("1.2.1")));
        assertEquals(-1, VersionCompare.forString("1.1.1").compareTo(VersionCompare.forString("1.1.2")));
        assertEquals(-1, VersionCompare.forString("1.1.1-blah").compareTo(VersionCompare.forString("1.1.2-blah")));

        assertEquals(1, VersionCompare.forString("3").compareTo(VersionCompare.forString("2")));
        assertEquals(1, VersionCompare.forString("1.3").compareTo(VersionCompare.forString("1.2")));
        assertEquals(1, VersionCompare.forString("1.3.1").compareTo(VersionCompare.forString("1.2.1")));
        assertEquals(1, VersionCompare.forString("1.1.3").compareTo(VersionCompare.forString("1.1.2")));
        assertEquals(1, VersionCompare.forString("1.1.3-blah").compareTo(VersionCompare.forString("1.1.2-blah")));

        assertEquals(1, VersionCompare.forString("3").compareTo(VersionCompare.forString("2")));
        assertEquals(1, VersionCompare.forString("3.1").compareTo(VersionCompare.forString("2.1")));
        assertEquals(1, VersionCompare.forString("3.1.1").compareTo(VersionCompare.forString("2.1.1")));
        assertEquals(1, VersionCompare.forString("3.1.1-blah").compareTo(VersionCompare.forString("2.1.1-blah")));
    }
    public void testInvalid(){
        assertEquals(-1, VersionCompare.forString("invalid").compareTo(VersionCompare.forString("0")));
        assertEquals(-1, VersionCompare.forString("invalid").compareTo(VersionCompare.forString("2")));
        assertEquals(-1, VersionCompare.forString("invalid").compareTo(VersionCompare.forString("2.1")));
        assertEquals(-1, VersionCompare.forString("invalid").compareTo(VersionCompare.forString("2.1.1")));
        assertEquals(-1, VersionCompare.forString("invalid").compareTo(VersionCompare.forString("2.1.1-blah")));
    }
    public void testNull(){
        assertEquals(-1, VersionCompare.forString(null).compareTo(VersionCompare.forString("0")));
        assertEquals(-1, VersionCompare.forString(null).compareTo(VersionCompare.forString("2")));
        assertEquals(-1, VersionCompare.forString(null).compareTo(VersionCompare.forString("2.1")));
        assertEquals(-1, VersionCompare.forString(null).compareTo(VersionCompare.forString("2.1.1")));
        assertEquals(-1, VersionCompare.forString(null).compareTo(VersionCompare.forString("2.1.1-blah")));

    }

    public void testForString() throws Exception {
        assertVersionCompare("1", 1, "1", null, null, null, null, null, VersionCompare.forString("1"));
        assertVersionCompare("1a", null, "1a", null, null, null, null, null, VersionCompare.forString("1a"));
        assertVersionCompare("1.1", 1, "1", 1, "1", null, null, null, VersionCompare.forString("1.1"));
        assertVersionCompare("1a.1", null, "1a", 1, "1", null, null, null, VersionCompare.forString("1a.1"));
        assertVersionCompare("1.1.1", 1, "1", 1, "1", 1, "1", null, VersionCompare.forString("1.1.1"));
        assertVersionCompare("1.1.1-blah", 1, "1", 1, "1", 1, "1", "blah", VersionCompare.forString("1.1.1-blah"));
        assertVersionCompare("1-dev", 1, "1", null, null, null, null, "dev", VersionCompare.forString("1-dev"));
        assertVersionCompare("1.1-dev", 1, "1", 1, "1", null, null, "dev", VersionCompare.forString("1.1-dev"));
    }

    private void assertVersionCompare(String versTest, final Integer maj, final String majString, final Integer min,
                                      final String minString, final Integer patch, final String patchString,
                                      final String tag, final VersionCompare versionCompare) {
        assertEquals("wrong maj for " + versTest, maj, versionCompare.maj);
        assertEquals("wrong majString for " + versTest, majString, versionCompare.majString);
        assertEquals("wrong min for " + versTest, min, versionCompare.min);
        assertEquals("wrong minString for " + versTest, minString, versionCompare.minString);
        assertEquals("wrong patch for " + versTest, patch, versionCompare.patch);
        assertEquals("wrong patchString for " + versTest, patchString, versionCompare.patchString);
        assertEquals("wrong tag for " + versTest, tag, versionCompare.tag);
    }
}
