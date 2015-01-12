package com.dtolabs.rundeck.app.internal.logging

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin;

/*
 * Copyright 2013 DTO Labs, Inc. (http://dtolabs.com)
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
 * FSFileLineIteratorTest.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/23/13 11:42 PM
 * 
 */
@TestMixin(GrailsUnitTestMixin)
class FSFileLineIteratorTest{

    File testfile1
    public void setUp() throws Exception {
        
        testfile1 = File.createTempFile("FSFileLineIteratorTest1",".txt")
        testfile1.deleteOnExit()
        testfile1.withWriter {
            it<<"monkey chicken\n"
            it<<"balogna lasagna\n"
            it<<"cheese asparagus\n"
        }
    }


    public void testIt(){
        def iterator = new FSFileLineIterator(new FileInputStream(testfile1), "UTF-8")
        assertTrue(iterator.hasNext())
        assertEquals(0,iterator.offset)
        assertEquals("monkey chicken",iterator.next())
        assertEquals(15, iterator.offset)
        assertEquals("balogna lasagna",iterator.next())
        assertEquals(31, iterator.offset)
        assertEquals("cheese asparagus",iterator.next())
        assertEquals(testfile1.length(), iterator.offset)
        assertFalse(iterator.hasNext())
    }
}
