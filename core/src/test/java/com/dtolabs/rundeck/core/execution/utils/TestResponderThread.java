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
* TestResponderThread.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/21/11 12:35 PM
* 
*/
package com.dtolabs.rundeck.core.execution.utils;

import com.dtolabs.rundeck.core.utils.PartialLineBuffer;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;


/**
 * TestResponderThread is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestResponderThread extends TestCase {

    public void testDetectSuccess() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah\n".getBytes());
        boolean result = ResponderTask.detect("^Test2: .*", null, 1000, 20, new InputStreamReader(bais),
                                              new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectFail() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah\n".getBytes());
        boolean result = ResponderTask.detect(null, "^Test2: .*", 1000, 20, new InputStreamReader(bais),
                                              new PartialLineBuffer());
        assertFalse(result);
    }

    public void testDetectSuccessPartial() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah".getBytes());
        boolean result = ResponderTask.detect("^Test2: .*", null, 1000, 20, new InputStreamReader(bais),
                                              new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectFailPartial() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah".getBytes());
        boolean result = ResponderTask.detect(null, "^Test2: .*", 1000, 20, new InputStreamReader(bais),
                                              new PartialLineBuffer());
        assertFalse(result);
    }

    public void testDetectSuccessPartial2() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\r\nTest2: blah\r".getBytes());
        boolean result = ResponderTask.detect("^Test2: .*", null, 1000, 20, new InputStreamReader(bais),
                                              new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectFailPartial2() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\r\nTest2: blah\r".getBytes());
        boolean result = ResponderTask.detect(null, "^Test2: .*", 1000, 20, new InputStreamReader(bais),
                                              new PartialLineBuffer());
        assertFalse(result);
    }

    public void testDetectSuccessPartial3() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\rTest2: blah".getBytes());
        boolean result = ResponderTask.detect("^Test2: .*", null, 1000, 20, new InputStreamReader(bais),
                                              new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectFailPartial3() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\rTest2: blah".getBytes());
        boolean result = ResponderTask.detect(null, "^Test2: .*", 1000, 20, new InputStreamReader(bais),
                                              new PartialLineBuffer());
        assertFalse(result);
    }

    public void testDetectBoth() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah\n".getBytes());
        boolean result = ResponderTask.detect("^Test2: .*", "^Test2: .*", 1000, 20, new InputStreamReader(bais),
                                              new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectBothOrderSuccessFirst() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah\nTest3: bloo".getBytes());
        boolean result = ResponderTask.detect("^Test2: .*", "^Test3: .*", 1000, 20, new InputStreamReader(bais),
                                              new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectBothOrderFailureFirst() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah\nTest3: bloo".getBytes());
        boolean result = ResponderTask.detect("^Test3: .*", "^Test2: .*", 1000, 20, new InputStreamReader(bais),
                                              new PartialLineBuffer());
        assertFalse(result);
    }

    public void testDetectMaxLines() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest1\nTest1\nTest2: blah\nTest3: bloo".getBytes());
        try {
            boolean result = ResponderTask.detect("^Test2: .*", null, 1000, 5, new InputStreamReader(bais),
                                                  new PartialLineBuffer());
            fail("Should have thrown exception");
        } catch (ResponderTask.ThreshholdException e) {
            assertNotNull(e);
            assertEquals(Integer.valueOf(5), e.getValue());
            assertEquals(ResponderTask.ThresholdType.lines, e.getType());
        }
    }

    public void testDetectMaxLinesOK() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest1\nTest1\nTest2: blah\nTest3: bloo".getBytes());
        boolean result = ResponderTask.detect("^Test2: .*", null, 1000, 6, new InputStreamReader(bais),
                                              new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectMaxTimeout() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest1\nTest1\nTest2: blah\nTest3: bloo".getBytes());
        try {
            boolean result = ResponderTask.detect("^TestZ: .*", null, 100, 50, new InputStreamReader(bais),
                                                  new PartialLineBuffer());
            fail("Should have thrown exception");
        } catch (ResponderTask.ThreshholdException e) {
            assertNotNull(e);
            assertEquals(Long.valueOf(100), e.getValue());
            assertEquals(ResponderTask.ThresholdType.milliseconds, e.getType());
        }
    }

    static class testResponder implements Responder{
        private String inputSuccessPattern;
        private String inputFailurePattern;
        private String responseSuccessPattern;
        private String responseFailurePattern;
        private int inputMaxLines;
        private long inputMaxTimeout;
        private boolean failOnInputLinesThreshold;
        private boolean failOnInputTimeoutThreshold;
        private boolean successOnInputThreshold;
        private int responseMaxLines;
        private long responseMaxTimeout;
        private boolean failOnResponseThreshold;
        private String inputString;
        private byte[] inputBytes;

        public String getInputSuccessPattern() {
            return inputSuccessPattern;
        }

        public String getInputFailurePattern() {
            return inputFailurePattern;
        }

        public String getResponseSuccessPattern() {
            return responseSuccessPattern;
        }

        public String getResponseFailurePattern() {
            return responseFailurePattern;
        }

        public int getInputMaxLines() {
            return inputMaxLines;
        }

        public long getInputMaxTimeout() {
            return inputMaxTimeout;
        }

        public boolean isFailOnInputLinesThreshold() {
            return failOnInputLinesThreshold;
        }

        public int getResponseMaxLines() {
            return responseMaxLines;
        }

        public long getResponseMaxTimeout() {
            return responseMaxTimeout;
        }

        public boolean isFailOnResponseThreshold() {
            return failOnResponseThreshold;
        }

        @Override
        public byte[] getInputBytes() { return null!=inputBytes?inputBytes:null!=inputString?inputString.getBytes():null; }

        public boolean isSuccessOnInputThreshold() {
            return successOnInputThreshold;
        }

        public boolean isFailOnInputTimeoutThreshold() {
            return failOnInputTimeoutThreshold;
        }
    }
    public void testRunResponderDefault() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
    }
    public void testRunResponderInputSuccess() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
    }

    public void testRunResponderInputSuccessMiss() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnInputTimeoutThreshold = true;
        testResponder.failOnResponseThreshold = true;
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertFalse(call.isSuccess());
        assertNotNull(call.getFailureReason());
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 5 lines", call.getFailureReason());
    }
    public void testRunResponderInputSuccessMissTimeout() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 20;
        testResponder.inputMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnInputTimeoutThreshold = true;
        testResponder.failOnResponseThreshold = true;
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertFalse(call.isSuccess());
        assertNotNull(call.getFailureReason());
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 1000 milliseconds",
                     call.getFailureReason());
    }

    public void testRunResponderInputSuccessMissNofail() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = false;
        testResponder.failOnInputTimeoutThreshold = false;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test";
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
        assertEquals("Test", baos.toString());
    }

    public void testRunResponderInputSuccessOnInputThreshold() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.successOnInputThreshold = true; //set to true
        testResponder.failOnInputLinesThreshold = false;
        testResponder.failOnInputTimeoutThreshold = false;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test";
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());

        //result is no output 
        assertEquals("", baos.toString());
    }

    public void testRunResponderInputFailure() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.inputFailurePattern = "^Test2: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertFalse(call.isSuccess());
        assertNotNull(call.getFailureReason());
        assertEquals("Failed waiting for input prompt: Expected input was not seen",
                     call.getFailureReason());
    }
    public void testRunResponderInputFailureMiss() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.inputFailurePattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertFalse(call.isSuccess());
        assertNotNull(call.getFailureReason());
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 5 lines",
                     call.getFailureReason());
    }
    public void testRunResponderInputFailureMissNoFail() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.inputFailurePattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = false;
        testResponder.failOnResponseThreshold = true;
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
    }

    public void testRunResponderResponseSuccess() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseSuccessPattern = "^Test3: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;

        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
    }
    public void testRunResponderResponseSuccessMiss() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo\nblah\nasdfa\nasdf\nasdf\n".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;

        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertFalse(call.isSuccess());
        assertNotNull(call.getFailureReason());
        assertEquals("Failed waiting for response: Expected input was not seen in 5 lines",
                     call.getFailureReason());
    }
    public void testRunResponderResponseSuccessMissNoFail() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = false;

        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
    }
    public void testRunResponderResponseFailure() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseFailurePattern = "^Test3: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;

        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertFalse(call.isSuccess());
        assertNotNull(call.getFailureReason());
        assertEquals("Failed waiting for response: Did not see the correct response",
                     call.getFailureReason());
    }
    public void testRunResponderResponseFailureMiss() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloon\nsdf\nsdf\nsdf\nsdf\nsdf\n".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseFailurePattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;

        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertFalse(call.isSuccess());
        assertNotNull(call.getFailureReason());
        assertEquals("Failed waiting for response: Expected input was not seen in 5 lines",
                     call.getFailureReason());
    }
    public void testRunResponderResponseFailureMissNoFail() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = false;

        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
    }
    public void testRunResponderWrite() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = null;
        testResponder.responseSuccessPattern = null;
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test";

        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
        assertEquals("Test", baos.toString());
    }
    public void testRunResponderWriteBytes() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = null;
        testResponder.responseSuccessPattern = null;
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputBytes = "Test".getBytes();

        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
        assertEquals("Test", baos.toString());
    }
    public void testChainedResponderOutput() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = null;
        testResponder.responseSuccessPattern = null;
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test1\n";
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);


        final testResponder testResponder2 = new testResponder();
        testResponder2.inputSuccessPattern = null;
        testResponder2.responseSuccessPattern = null;
        testResponder2.inputMaxLines = 5;
        testResponder2.inputMaxTimeout = 1000;
        testResponder2.responseMaxLines = 5;
        testResponder2.responseMaxTimeout = 1000;
        testResponder2.failOnInputLinesThreshold = true;
        testResponder2.failOnResponseThreshold = true;
        testResponder2.inputString = "Test2\n";
        final Callable<ResponderTask.ResponderResult> task = responderThread.createSequence(testResponder2, null);
        final ResponderTask.ResponderResult call = task.call();

        assertEquals(testResponder2, call.getResponder());
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
        assertEquals("Test1\nTest2\n", baos.toString());
    }
    public void testChainedResponderInput1Detect() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTestZ: bloo\nFlip1\nFlip2\nFlip3".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test1\n";
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);


        final testResponder testResponder2 = new testResponder();
        testResponder2.inputSuccessPattern = null;
        testResponder2.responseSuccessPattern = null;
        testResponder2.inputMaxLines = 5;
        testResponder2.inputMaxTimeout = 1000;
        testResponder2.responseMaxLines = 5;
        testResponder2.responseMaxTimeout = 1000;
        testResponder2.failOnInputLinesThreshold = true;
        testResponder2.failOnResponseThreshold = true;
        testResponder2.inputString = "Test2\n";
        final Callable<ResponderTask.ResponderResult> task = responderThread.createSequence(testResponder2, null);
        final ResponderTask.ResponderResult call = task.call();

        assertEquals(testResponder2, call.getResponder());
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
        assertEquals("Test1\nTest2\n", baos.toString());
    }
    public void testChainedResponderInput2Detect() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTestZ: bloo\nFlip1: input\nFlip2\nFlip3\nFlipZ: OK".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test1\n";
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);


        final testResponder testResponder2 = new testResponder();
        testResponder2.inputSuccessPattern = "^Flip1: .*";
        testResponder2.responseSuccessPattern = "^FlipZ: .*";
        testResponder2.inputMaxLines = 5;
        testResponder2.inputMaxTimeout = 1000;
        testResponder2.responseMaxLines = 5;
        testResponder2.responseMaxTimeout = 1000;
        testResponder2.failOnInputLinesThreshold = true;
        testResponder2.failOnResponseThreshold = true;
        testResponder2.inputString = "Test2\n";

        final Callable<ResponderTask.ResponderResult> task = responderThread.createSequence(testResponder2, null);
        final ResponderTask.ResponderResult call = task.call();

        assertEquals(testResponder2, call.getResponder());
        assertNull(call.getFailureReason(), call.getFailureReason());
        assertTrue(call.isSuccess());
        assertEquals("Test1\nTest2\n", baos.toString());
    }
    public void testChainedResponderInput1NotDetected() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTestZ: bloo\nFlip1: input\nFlip2\nFlip3\nFlipZ: OK".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^TestX: .*";
        testResponder.responseSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test1\n";
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);


        final testResponder testResponder2 = new testResponder();
        testResponder2.inputSuccessPattern = "^Flip1: .*";
        testResponder2.responseSuccessPattern = "^FlipZ: .*";
        testResponder2.inputMaxLines = 5;
        testResponder2.inputMaxTimeout = 1000;
        testResponder2.responseMaxLines = 5;
        testResponder2.responseMaxTimeout = 1000;
        testResponder2.failOnInputLinesThreshold = true;
        testResponder2.failOnResponseThreshold = true;
        testResponder2.inputString = "Test2\n";

        final Callable<ResponderTask.ResponderResult> task = responderThread.createSequence(testResponder2, null);
        final ResponderTask.ResponderResult call = task.call();

        assertEquals(testResponder, call.getResponder());
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 5 lines",
                     call.getFailureReason());
        assertFalse(call.isSuccess());
        assertEquals(0, baos.size());
    }
    public void testChainedResponderInput1Failure() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTestZ: bloo\nFlip1: input\nFlip2\nFlip3\nFlipZ: OK".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^TestX: .*";
        testResponder.responseSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test1\n";
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);


        final testResponder testResponder2 = new testResponder();
        testResponder2.inputSuccessPattern = "^Flip1: .*";
        testResponder2.responseSuccessPattern = "^FlipZ: .*";
        testResponder2.inputMaxLines = 5;
        testResponder2.inputMaxTimeout = 1000;
        testResponder2.responseMaxLines = 5;
        testResponder2.responseMaxTimeout = 1000;
        testResponder2.failOnInputLinesThreshold = true;
        testResponder2.failOnResponseThreshold = true;
        testResponder2.inputString = "Test2\n";

        final Callable<ResponderTask.ResponderResult> task = responderThread.createSequence(testResponder2, null);
        final ResponderTask.ResponderResult call = task.call();

        assertEquals(testResponder, call.getResponder());
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 5 lines",
                     call.getFailureReason());
        assertFalse(call.isSuccess());
        assertEquals(0, baos.size());
    }

    public void testChainedResponderInput2NotDetected() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTestZ: bloo\nFlip1: input\nFlip2\nFlip3\nFlipZ: OK".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test1\n";
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);


        final testResponder testResponder2 = new testResponder();
        testResponder2.inputSuccessPattern = "^FlipX: .*";
        testResponder2.responseSuccessPattern = "^FlipZ: .*";
        testResponder2.inputMaxLines = 50;//will not be reached
        testResponder2.inputMaxTimeout = 1000;//will be met
        testResponder2.responseMaxLines = 5;
        testResponder2.responseMaxTimeout = 1000;
        testResponder2.failOnInputLinesThreshold = true;
        testResponder2.failOnInputTimeoutThreshold = true; //fail on timeout waiting for input
        testResponder2.failOnResponseThreshold = true;
        testResponder2.inputString = "Test2\n";

        final Callable<ResponderTask.ResponderResult> task = responderThread.createSequence(testResponder2, null);
        final ResponderTask.ResponderResult call = task.call();

        assertEquals(testResponder2, call.getResponder());
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 1000 milliseconds", call.getFailureReason());
        assertFalse(call.isSuccess());
        assertEquals("Test1\n", baos.toString());
    }

    public void testChainedResponderInput2Failure() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTestZ: bloo\nFlip1: input\nFlip2\nFlip3\nFlipZ: NOPE".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseSuccessPattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test1\n";
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);


        final testResponder testResponder2 = new testResponder();
        testResponder2.inputSuccessPattern = "^Flip1: .*";
        testResponder2.responseSuccessPattern = "^FlipXX: .*";
        testResponder2.responseFailurePattern = "^FlipZ: .*";
        testResponder2.inputMaxLines = 4;
        testResponder2.inputMaxTimeout = 1000;
        testResponder2.responseMaxLines = 50;
        testResponder2.responseMaxTimeout = 1000;
        testResponder2.failOnInputLinesThreshold = true;
        testResponder2.failOnInputTimeoutThreshold = true;
        testResponder2.failOnResponseThreshold = true;
        testResponder2.inputString = "Test2\n";

        final Callable<ResponderTask.ResponderResult> task = responderThread.createSequence(testResponder2, null);
        final ResponderTask.ResponderResult call = task.call();

        assertEquals(testResponder2,call.getResponder());
        assertEquals("Failed waiting for response: Did not see the correct response", call.getFailureReason());
        assertFalse(call.isSuccess());
        assertEquals("Test1\nTest2\n", baos.toString());
    }
    public void testRunResponderWriteOnSuccess() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseSuccessPattern = null;
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test";

        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
        assertEquals("Test", baos.toString());
    }
    public void testRunResponderNoWriteOnFailure() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputFailurePattern = "^Test2: .*";
        testResponder.responseSuccessPattern = null;
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test";

        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertEquals("", baos.toString());
        assertFalse(call.isSuccess());
        assertNotNull(call.getFailureReason(), call.getFailureReason());
        assertEquals("Failed waiting for input prompt: Expected input was not seen", call.getFailureReason());
    }

    public void testRunResponderWriteAndResponseSuccess() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseSuccessPattern = "^Test3: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test";

        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
        assertEquals("Test", baos.toString());
    }

    public void testRunResponderWriteAndResponseFailure() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseFailurePattern = "^Test3: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test";

        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, null);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertEquals("Test", baos.toString());
        assertFalse(call.isSuccess());
        assertNotNull(call.getFailureReason());
    }
    public void testResultHandlerSuccess() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.responseSuccessPattern = "^Test3: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test";

        final boolean[] result = {false};
        final String[] resultReason = {null};
        final ResponderTask.ResultHandler resultHandler = new ResponderTask.ResultHandler() {
            public void handleResult(boolean success, String reason) {
                result[0]=success;
                resultReason[0]=reason;
            }
        };
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, resultHandler);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertTrue(call.isSuccess());
        assertNull(call.getFailureReason());
        assertEquals("Test", baos.toString());
        assertTrue(result[0]);
        assertNull(resultReason[0]);
    }
    public void testResultHandlerInputFailure() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^TestZ: .*";
        testResponder.responseSuccessPattern = "^Test3: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.responseMaxLines = 5;
        testResponder.responseMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        testResponder.inputString = "Test";

        final boolean[] result = {true};
        final String[] resultReason = {null};
        final ResponderTask.ResultHandler resultHandler = new ResponderTask.ResultHandler() {
            public void handleResult(boolean success, String reason) {
                result[0]=success;
                resultReason[0]=reason;
            }
        };
        final ResponderTask responderThread = new ResponderTask(testResponder, bais, baos, resultHandler);
        final ResponderTask.ResponderResult call = responderThread.call();
        assertFalse(call.isSuccess());
        assertNotNull(call.getFailureReason());
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 5 lines",
                     call.getFailureReason());
        assertEquals("", baos.toString());
        assertFalse(result[0]);
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 5 lines", resultReason[0]);
    }
}
