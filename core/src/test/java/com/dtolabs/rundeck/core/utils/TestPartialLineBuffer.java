/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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
* TestPartialLineBuffer.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/21/11 4:27 PM
* 
*/
package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.utils.PartialLineBuffer;
import junit.framework.TestCase;

import java.util.List;

/**
 * TestPartialLineBuffer is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestPartialLineBuffer extends TestCase {

    public void testPartialLineBuffer() {
        final PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        assertNull(partialLineBuffer.readLine());
        assertNull(partialLineBuffer.getPartialLine());

        char[] data = "Test1".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);
        assertNull(partialLineBuffer.readLine());
        assertEquals("Test1", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());

        data = " Test2".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertNull(partialLineBuffer.readLine());
        assertEquals("Test1 Test2", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());

        data = " Test3\n".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test1 Test2 Test3", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertNull("not null: '" + partialLineBuffer.getPartialLine(false) + "'", partialLineBuffer.getPartialLine(
            false));
    }

    public void testSplit() {
        String arr[] = "abc:def::ghi".split(":", -1);
        assertEquals(4, arr.length);
        String arr2[] = "ghi:".split(":", -1);
        assertEquals(2, arr2.length);
        String arr3[] = ":ghi::".split(":", -1);
        assertEquals(4, arr3.length);
        String arr4[] = "::ghi::".split(":", -1);
        assertEquals(5, arr4.length);
    }

    public void testSplitLine() {
        //a: 1,1: yes,no
        //b: 0,1: yes,no
        //c: 0,0: yes,no (pass)
        //d: 1,0: yes,(skip)
        String arr[] = "abc\rdef\r\rghi".split("\\r", -1);
        assertEquals(4, arr.length);
        String arr2[] = "ghi\r".split("\\r", -1);
        assertEquals(2, arr2.length);
        String arr3[] = "\rghi\r\r".split("\\r", -1);
        assertEquals(4, arr3.length);
        String arr4[] = "\r\rghi\r\r".split("\\r", -1);
        assertEquals(5, arr4.length);
        String arr5[] = "\r".split("\\r", -1);
        assertEquals(2, arr5.length);
    }

    public void testPartialLineBufferMultiSplit() {
        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        assertNull(partialLineBuffer.readLine());
        assertNull(partialLineBuffer.getPartialLine());

        //multiline

        char[] data = "Test4\nTest5".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test4", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertEquals("Test5", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());

        data = " Test6\r\nTest7".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test5 Test6", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertEquals("Test7", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());

        data = " Test8\rTest9".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test7 Test8", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertEquals("Test9", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());
    }

    public void testPartialLineBufferMulti() {

        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test10\nTest11\n".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test10", partialLineBuffer.readLine());
        assertEquals("Test11", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertNull(partialLineBuffer.getPartialLine());

        data = "Test10\r\nTest11\r\n".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test10", partialLineBuffer.readLine());
        assertEquals("Test11", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertNull(partialLineBuffer.getPartialLine());

        data = "Test10\rTest11\r".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test10", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertEquals("Test11", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());
    }

    public void testPartialLineBufferLeftoverCR() {

        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test10\rTest11\r".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test10", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertEquals("Test11", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());

        //append to line
        data = " Test12\rTest13".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test11", partialLineBuffer.readLine());
        assertEquals(" Test12", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertEquals("Test13", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());
    }

    public void testPartialLineBufferLeftoverCRPartial() {

        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test10\rTest11\r".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test10", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertEquals("Test11", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());

        //append to line
        data = " Test12".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test11", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertEquals(" Test12", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());
    }

    public void testPartialLineBufferLeftoverCRNL() {

        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test10\r\nTest11\r".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test10", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertEquals("Test11", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());

        //append to line
        data = " Test12\r\n".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test11", partialLineBuffer.readLine());
        assertEquals(" Test12", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertNull(partialLineBuffer.getPartialLine());
    }

    public void testPartialLineBufferLeftoverCRNLPartialNL() {

        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test10\r\nTest11\r".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test10", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertEquals("Test11", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());

        //append to line
        data = "\n Test12\r\n".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test11", partialLineBuffer.readLine());
        assertEquals(" Test12", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertNull(partialLineBuffer.getPartialLine());
    }

    public void testPartialLineBufferLeftoverCRNLPartial() {

        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test10\r\nTest11\r".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test10", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertEquals("Test11", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());

        //append to line
        data = "\n Test12".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals("Test11", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());
        assertEquals(" Test12", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());
    }

    public void testPartialLineBufferFull() {
        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test1\rTest2\rTest3: ".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals(2, partialLineBuffer.getLines().size());
        assertEquals("Test3: ", partialLineBuffer.getPartialLine(false));

        assertEquals("Test1", partialLineBuffer.readLine());
        assertEquals("Test2", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());

        assertEquals("Test3: ", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());

    }

    public void testPartialLineBufferFull2() {
        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test1\r\nTest2\r\nTest3: ".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals(2, partialLineBuffer.getLines().size());
        assertEquals("Test3: ", partialLineBuffer.getPartialLine(false));

        assertEquals("Test1", partialLineBuffer.readLine());
        assertEquals("Test2", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());

        assertEquals("Test3: ", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());
    }

    public void testPartialLineBufferFull3() {
        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test1\nTest2\nTest3: ".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals(2, partialLineBuffer.getLines().size());
        assertEquals("Test3: ", partialLineBuffer.getPartialLine(false));

        assertEquals("Test1", partialLineBuffer.readLine());
        assertEquals("Test2", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());

        assertEquals("Test3: ", partialLineBuffer.getPartialLine());
        assertNull(partialLineBuffer.getPartialLine());
    }

    public void testPartialLineBufferEmptyLine() {
        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test1\nTest2\n".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals(2, partialLineBuffer.getLines().size());
        assertNull(partialLineBuffer.getPartialLine());

        assertEquals("Test1", partialLineBuffer.readLine());
        assertEquals("Test2", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.readLine());

        data = "\r\n".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);
        assertEquals("", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.getPartialLine());

        data = "\r\n\r\n".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);
        assertEquals("", partialLineBuffer.readLine());
        assertEquals("", partialLineBuffer.readLine());
        assertNull(partialLineBuffer.getPartialLine());
    }
    public void testUnmarkPartial() {
        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test1\r\nTest2\r\nTest3: ".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        assertEquals(2, partialLineBuffer.getLines().size());
        assertEquals("Test3: ", partialLineBuffer.getPartialLine(false));
        assertEquals("Test3: ", partialLineBuffer.getPartialLine());
        assertEquals(null, partialLineBuffer.getPartialLine());
        partialLineBuffer.unmarkPartial();
        assertEquals("Test3: ", partialLineBuffer.getPartialLine(false));
    }

    /**
     * Test input ending with CR, then empty string
     */
    public void testCRLine(){
        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test1\r".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        data = "".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        List<String> lines = partialLineBuffer.getLines();
        assertEquals(0, lines.size());

        String partialLine = partialLineBuffer.getPartialLine(true);
        assertEquals("Test1",partialLine);
    }

    /**
     * test input ending with CR, then NL
     */
    public void testCRNLLine(){
        PartialLineBuffer partialLineBuffer = new PartialLineBuffer();
        char[] data = "Test1\r".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        data = "\n".toCharArray();
        partialLineBuffer.addData(data, 0, data.length);

        List<String> lines = partialLineBuffer.getLines();
        assertEquals(1, lines.size());
        assertEquals("Test1", lines.get(0));

        String partialLine = partialLineBuffer.getPartialLine(true);
        assertEquals(null,partialLine);
    }
}
