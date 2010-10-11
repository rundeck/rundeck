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
 * TestStringArrayUtil.java
 * 
 * User: greg
 * Created: Sep 12, 2005 12:00:34 PM
 * $Id: TestStringArrayUtil.java 1079 2008-02-05 04:53:32Z ahonor $
 */


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.HashSet;


/**
 * Test for StringArrayUtil
 */
public class TestStringArrayUtil extends TestCase {
    String[] arr1;
    String[] arr2;
    String[] arr3;

    public TestStringArrayUtil(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestStringArrayUtil.class);
    }

    protected void setUp() throws Exception {
        arr1 = new String[]{"a", "b", "c"};
        arr2 = new String[]{"d", "e", "f"};
        arr3 = new String[]{"a", "e", "z"};
    }

    protected void initAssert() throws Exception{
        assertEquals("unexpected size of arr1", 3, arr1.length);
        assertEquals("unexpected size of arr2", 3, arr2.length);
        assertEquals("unexpected size of arr3", 3, arr3.length);
        assertEquals("unexpected element 0 of arr1", "a", arr1[0]);
        assertEquals("unexpected element 1 of arr1", "b", arr1[1]);
        assertEquals("unexpected element 2 of arr1", "c", arr1[2]);

        assertEquals("unexpected element 0 of arr2", "d", arr2[0]);
        assertEquals("unexpected element 1 of arr2", "e", arr2[1]);
        assertEquals("unexpected element 2 of arr2", "f", arr2[2]);

        assertEquals("unexpected element 0 of arr3", "a", arr3[0]);
        assertEquals("unexpected element 1 of arr3", "e", arr3[1]);
        assertEquals("unexpected element 2 of arr3", "z", arr3[2]);
    }

    protected void tearDown() throws Exception {
        arr1=null;
        arr2=null;
        arr3=null;
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    protected void assertContainsAll(String[] set1, String[] t1, String[] t2){
        assertContainsAll(set1, t1);
        assertContainsAll(set1, t2);
    }

    protected void assertContainsAll(String[] set1, String[] t1) {
        HashSet s1 = new HashSet(Arrays.asList(set1));
        for (int i = 0; i < t1.length; i++) {
            String s = t1[i];
            assertTrue("element " + s + " is not in set", s1.contains(s));
        }
    }


    protected void assertContainsNone(String[] set1, String[] t1) {
        HashSet s1 = new HashSet(Arrays.asList(set1));
        for (int i = 0; i < t1.length; i++) {
            String s = t1[i];
            assertFalse("element " + s + " is in set", s1.contains(s));
        }
    }

    protected void assertContainsNone(String[] set1, String[] t1, String[] t2) {
        assertContainsNone(set1, t1);
        assertContainsNone(set1, t2);
    }
    public void testMerge() throws Exception{
        initAssert();

        //1. perform merge (A U B)
        //2. check size == A + B
        //3. check contents = A U B
        //4. perform merge (B U A)
        //5. repeat #2
        //6. repeat #3


        //set arr1 and arr2

        String[] t1 = StringArrayUtil.merge(arr1, arr2);
        assertEquals("merged size wrong: " + t1.length, 6, t1.length);
        assertContainsAll(t1, arr1, arr2);

        t1 = StringArrayUtil.merge(arr2, arr1);
        assertEquals("merged size wrong: " + t1.length, 6, t1.length);
        assertContainsAll(t1, arr1, arr2);


        //set arr1 and arr3

        t1 = StringArrayUtil.merge(arr1, arr3);
        assertEquals("merged size wrong: " + t1.length, 5, t1.length);
        assertContainsAll(t1, arr1, arr3);

        t1 = StringArrayUtil.merge(arr3, arr1);
        assertEquals("merged size wrong: " + t1.length, 5, t1.length);
        assertContainsAll(t1, arr1, arr3);


        //set arr2 and arr3

        t1 = StringArrayUtil.merge(arr2, arr3);
        assertEquals("merged size wrong: " + t1.length, 5, t1.length);
        assertContainsAll(t1, arr2, arr3);

        t1 = StringArrayUtil.merge(arr3, arr2);
        assertEquals("merged size wrong: " + t1.length, 5, t1.length);
        assertContainsAll(t1, arr2, arr3);

    }

    public void testSubtract() throws Exception{
        initAssert();

        //1. perform subtract (B - A)
        //2. check size == B - A
        //3. check contents = B - A
        //4. perform subtract (A - B)
        //5. check size == A - B
        //6. check contents = A - B


        //set arr1 and arr2

        String[] t1 = StringArrayUtil.subtract(arr1, arr2);
        assertEquals("subtract size wrong: " + t1.length, 3, t1.length);
        assertContainsNone(t1, arr1);

        t1 = StringArrayUtil.subtract(arr2, arr1);
        assertEquals("subtract size wrong: " + t1.length, 3, t1.length);
        assertContainsNone(t1, arr2);


        //set arr1 and arr3

        t1 = StringArrayUtil.subtract(arr1, arr3);
        assertEquals("subtract size wrong: " + t1.length, 2, t1.length);
        assertContainsNone(t1, arr1);

        t1 = StringArrayUtil.subtract(arr3, arr1);
        assertEquals("subtract size wrong: " + t1.length, 2, t1.length);
        assertContainsNone(t1, arr3);


        //set arr2 and arr3


        t1 = StringArrayUtil.subtract(arr2, arr3);
        assertEquals("subtract size wrong: " + t1.length, 2, t1.length);
        assertContainsNone(t1, arr2);

        t1 = StringArrayUtil.subtract(arr3, arr2);
        assertEquals("subtract size wrong: " + t1.length, 2, t1.length);
        assertContainsNone(t1, arr3);



    }

    public void testDifference() throws Exception{
        initAssert();


        //1. perform difference (B ^ A)
        //2. check size == B ^ A
        //3. check contents = B ^ A
        //4. perform difference (A ^ B)
        //5. check size == A ^ B
        //6. check contents = A ^ B
        //7. check (B ^ A) == ((A - B) U (B - A))


        //set arr1 and arr2

        String[] t1 = StringArrayUtil.difference(arr1, arr2);
        assertEquals("difference size wrong: " + t1.length, 6, t1.length);
        assertContainsAll(t1, arr1, arr2);
        String[] t2 = StringArrayUtil.subtract(arr1, arr2);
        String[] t3 = StringArrayUtil.subtract(arr2, arr1);
        String[] t4 = StringArrayUtil.merge(t2, t3);
        assertContainsAll(t1, t4);
        assertContainsAll(t4, t1);

        t1 = StringArrayUtil.difference(arr2, arr1);
        assertEquals("difference size wrong: " + t1.length, 6, t1.length);
        assertContainsAll(t1, arr2, arr1);
        t2 = StringArrayUtil.subtract(arr2, arr1);
        t3 = StringArrayUtil.subtract(arr1, arr2);
        t4 = StringArrayUtil.merge(t2, t3);
        assertContainsAll(t1, t4);
        assertContainsAll(t4, t1);

        //set arr1 and arr3

        t1 = StringArrayUtil.difference(arr1, arr3);
        assertEquals("difference size wrong: " + t1.length, 4, t1.length);
        assertContainsNone(t1, new String[]{"a"});
        assertContainsAll(t1, new String[]{"b","c","e","z"});
        t2 = StringArrayUtil.subtract(arr3, arr1);
        t3 = StringArrayUtil.subtract(arr1, arr3);
        t4 = StringArrayUtil.merge(t2, t3);
        assertContainsAll(t1, t4);
        assertContainsAll(t4, t1);

        t1 = StringArrayUtil.difference(arr3, arr1);
        assertEquals("difference size wrong: " + t1.length, 4, t1.length);
        assertContainsNone(t1, new String[]{"a"});
        assertContainsAll(t1, new String[]{"b", "c", "e", "z"});
        t2 = StringArrayUtil.subtract(arr1, arr3);
        t3 = StringArrayUtil.subtract(arr3, arr1);
        t4 = StringArrayUtil.merge(t2, t3);
        assertContainsAll(t1, t4);
        assertContainsAll(t4, t1);


        //set arr2 and arr3

        t1 = StringArrayUtil.difference(arr2, arr3);
        assertEquals("difference size wrong: " + t1.length, 4, t1.length);
        assertContainsNone(t1, new String[]{"e"});
        assertContainsAll(t1, new String[]{"d", "f", "a", "z"});
        t2 = StringArrayUtil.subtract(arr2, arr3);
        t3 = StringArrayUtil.subtract(arr3, arr2);
        t4 = StringArrayUtil.merge(t2, t3);
        assertContainsAll(t1, t4);
        assertContainsAll(t4, t1);

        t1 = StringArrayUtil.difference(arr3, arr2);
        assertEquals("difference size wrong: " + t1.length, 4, t1.length);
        assertContainsNone(t1, new String[]{"e"});
        assertContainsAll(t1, new String[]{"d", "f", "a", "z"});
        t2 = StringArrayUtil.subtract(arr3, arr2);
        t3 = StringArrayUtil.subtract(arr2, arr3);
        t4 = StringArrayUtil.merge(t2, t3);
        assertContainsAll(t1, t4);
        assertContainsAll(t4, t1);
    }

    public void testContains() throws Exception {
        initAssert();
        assertTrue(StringArrayUtil.contains(arr1, "a"));
        assertTrue(StringArrayUtil.contains(arr1, "b"));
        assertTrue(StringArrayUtil.contains(arr1, "c"));
        assertFalse(StringArrayUtil.contains(arr1, "d"));
        assertFalse(StringArrayUtil.contains(arr1, "e"));
        assertFalse(StringArrayUtil.contains(arr1, "f"));
        assertFalse(StringArrayUtil.contains(arr1, "z"));

        assertTrue(StringArrayUtil.contains(arr2, "d"));
        assertTrue(StringArrayUtil.contains(arr2, "e"));
        assertTrue(StringArrayUtil.contains(arr2, "f"));
        assertFalse(StringArrayUtil.contains(arr2, "a"));
        assertFalse(StringArrayUtil.contains(arr2, "b"));
        assertFalse(StringArrayUtil.contains(arr2, "c"));
        assertFalse(StringArrayUtil.contains(arr2, "z"));

        assertTrue(StringArrayUtil.contains(arr3, "a"));
        assertTrue(StringArrayUtil.contains(arr3, "e"));
        assertTrue(StringArrayUtil.contains(arr3, "z"));
        assertFalse(StringArrayUtil.contains(arr3, "d"));
        assertFalse(StringArrayUtil.contains(arr3, "b"));
        assertFalse(StringArrayUtil.contains(arr3, "f"));
        assertFalse(StringArrayUtil.contains(arr3, "c"));
    }


}
