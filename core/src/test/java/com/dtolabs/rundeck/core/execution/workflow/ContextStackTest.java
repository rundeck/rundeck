/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
* ContextStackTest.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/27/12 3:00 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import junit.framework.TestCase;

import java.util.*;


/**
 * ContextStackTest is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ContextStackTest extends TestCase {
    public void testPush() throws Exception {
        ContextStack<String> s = ContextStack.create("blah");
        assertNotNull(s);
        assertEquals(1, s.size());
        s.push("bloo");
        assertEquals(2, s.size());
        s.push("monkey");
        assertEquals(3, s.size());
    }
    public void testPushNull() throws Exception {
        ContextStack<String> s = ContextStack.create("blah");
        assertNotNull(s);
        assertEquals(1, s.size());
        assertEquals("blah", s.peek());
        s.push(null);
        assertEquals(1, s.size());
        assertEquals("blah", s.peek());
    }

    public void testPop() throws Exception {
        ContextStack<String> s = ContextStack.create("blah");
        assertNotNull(s);
        assertEquals(1, s.size());
        s.push("bloo");
        assertEquals(2, s.size());
        s.push("monkey");
        assertEquals(3, s.size());

        assertEquals("monkey", s.pop());
        assertEquals("bloo", s.pop());
        assertEquals("blah", s.pop());
    }

    public void testCopyPush() throws Exception {
        ContextStack<String> s = ContextStack.create("blah");
        ContextStack<String> t = s.copyPush("moo");
        assertEquals(2, t.size());
        assertEquals("moo", t.peek());
    }
    public void testCopyPushMakesCopy() throws Exception {
        ContextStack<String> s = ContextStack.create("blah");
        ContextStack<String> t = s.copyPush("moo");
        assertEquals(1, s.size());
        t.pop();
        t.pop();
        assertEquals(1, s.size());
        t.push("elf");
        t.push("elf2");
        assertEquals(1, s.size());
    }

    public void testCopyPop() throws Exception {
        ContextStack<String> s = ContextStack.create("blah");
        assertEquals(1, s.size());
        ContextStack<String> t = s.copyPop();
        assertEquals(0, t.size());
    }
    public void testCopyPopMakesCopy() throws Exception {
        ContextStack<String> s = ContextStack.create("blah");
        assertEquals(1, s.size());
        ContextStack<String> t = s.copyPop();
        assertEquals(0, t.size());
        assertEquals(1, s.size());
        assertEquals("blah",s.peek());
        t.push("a");
        t.push("b");
        t.push("c");
        assertEquals(1, s.size());
        assertEquals("blah", s.peek());
    }

    public void testSize() throws Exception {
        ContextStack<String> s = ContextStack.create("blah");
        assertNotNull(s);
        assertEquals(1, s.size());
        s.push("bloo");
        assertEquals(2, s.size());
        s.pop();
        assertEquals(1, s.size());
        s.pop();
        assertEquals(0, s.size());
    }

    public void testCreate() throws Exception {
        ContextStack<String> s = ContextStack.create("blah");
        assertNotNull(s);
        assertEquals(1, s.size());
        assertEquals("blah",s.peek());
    }
    public void testCreateNull() throws Exception {
        ContextStack<String> s = ContextStack.create((String)null);
        assertNotNull(s);
        assertEquals(0, s.size());
    }
}
