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

/**
 * TestResponderThread is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestResponderThread extends TestCase {

    public void testDetectSuccess() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah\n".getBytes());
        boolean result = ResponderThread.detect("^Test2: .*", null, 1000, 20, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return false;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectFail() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah\n".getBytes());
        boolean result = ResponderThread.detect(null, "^Test2: .*", 1000, 20, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return false;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertFalse(result);
    }

    public void testDetectSuccessPartial() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah".getBytes());
        boolean result = ResponderThread.detect("^Test2: .*", null, 1000, 20, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return false;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectFailPartial() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah".getBytes());
        boolean result = ResponderThread.detect(null, "^Test2: .*", 1000, 20, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return false;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertFalse(result);
    }

    public void testDetectSuccessPartial2() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\r\nTest2: blah\r".getBytes());
        boolean result = ResponderThread.detect("^Test2: .*", null, 1000, 20, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return false;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectFailPartial2() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\r\nTest2: blah\r".getBytes());
        boolean result = ResponderThread.detect(null, "^Test2: .*", 1000, 20, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return false;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertFalse(result);
    }

    public void testDetectSuccessPartial3() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\rTest2: blah".getBytes());
        boolean result = ResponderThread.detect("^Test2: .*", null, 1000, 20, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return false;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectFailPartial3() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\rTest2: blah".getBytes());
        boolean result = ResponderThread.detect(null, "^Test2: .*", 1000, 20, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return false;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertFalse(result);
    }

    public void testDetectBoth() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah\n".getBytes());
        boolean result = ResponderThread.detect("^Test2: .*", "^Test2: .*", 1000, 20, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return false;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectBothOrderSuccessFirst() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah\nTest3: bloo".getBytes());
        boolean result = ResponderThread.detect("^Test2: .*", "^Test3: .*", 1000, 20, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return false;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectBothOrderFailureFirst() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("Test1\nTest2: blah\nTest3: bloo".getBytes());
        boolean result = ResponderThread.detect("^Test3: .*", "^Test2: .*", 1000, 20, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return false;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertFalse(result);
    }

    public void testDetectMaxLines() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest1\nTest1\nTest2: blah\nTest3: bloo".getBytes());
        try {
            boolean result = ResponderThread.detect("^Test2: .*", null, 1000, 5, new InputStreamReader(bais),
                new ResponderStopper() {
                    public boolean isResponderStopped() {
                        return false;
                    }

                    public void stopResponder() {
                    }
                }, new PartialLineBuffer());
            fail("Should have thrown exception");
        } catch (ResponderThread.ThreshholdException e) {
            assertNotNull(e);
            assertEquals(Integer.valueOf(5), e.getValue());
            assertEquals(ResponderThread.ThresholdType.lines, e.getType());
        }
    }

    public void testDetectMaxLinesOK() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest1\nTest1\nTest2: blah\nTest3: bloo".getBytes());
        boolean result = ResponderThread.detect("^Test2: .*", null, 1000, 6, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return false;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertTrue(result);
    }

    public void testDetectMaxTimeout() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest1\nTest1\nTest2: blah\nTest3: bloo".getBytes());
        try {
            boolean result = ResponderThread.detect("^TestZ: .*", null, 100, 50, new InputStreamReader(bais),
                new ResponderStopper() {
                    public boolean isResponderStopped() {
                        return false;
                    }

                    public void stopResponder() {
                    }
                }, new PartialLineBuffer());
            fail("Should have thrown exception");
        } catch (ResponderThread.ThreshholdException e) {
            assertNotNull(e);
            assertEquals(Long.valueOf(100), e.getValue());
            assertEquals(ResponderThread.ThresholdType.milliseconds, e.getType());
        }
    }

    public void testDetectStopped() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest1\nTest1\nTest2: blah\nTest3: bloo".getBytes());
        boolean result = ResponderThread.detect("^TestZ: .*", null, 1000, 50, new InputStreamReader(bais),
            new ResponderStopper() {
                public boolean isResponderStopped() {
                    return true;
                }

                public void stopResponder() {
                }
            }, new PartialLineBuffer());
        assertFalse(result);
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

        public String getInputString() {
            return inputString;
        }

        public boolean isSuccessOnInputThreshold() {
            return successOnInputThreshold;
        }

        public boolean isFailOnInputTimeoutThreshold() {
            return failOnInputTimeoutThreshold;
        }
    }
    public void testRunResponderDefault(){
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertTrue(responderThread.isSuccess());
        assertFalse(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
    }
    public void testRunResponderInputSuccess(){
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.inputSuccessPattern = "^Test2: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertTrue(responderThread.isSuccess());
        assertFalse(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
    }

    public void testRunResponderInputSuccessMiss(){
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
        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertFalse(responderThread.isSuccess());
        assertTrue(responderThread.isFailed());
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 5 lines", responderThread.getFailureReason());
        assertFalse(responderThread.isResponderStopped());
    }
    public void testRunResponderInputSuccessMissTimeout(){
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
        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertFalse(responderThread.isSuccess());
        assertTrue(responderThread.isFailed());
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 1000 milliseconds", responderThread.getFailureReason());
        assertFalse(responderThread.isResponderStopped());
    }

    public void testRunResponderInputSuccessMissNofail(){
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
        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertTrue(responderThread.isSuccess());
        assertFalse(responderThread.isFailed());
        assertNull(responderThread.getFailureReason());
        assertFalse(responderThread.isResponderStopped());
        assertEquals("Test", baos.toString());
    }

    public void testRunResponderInputSuccessOnInputThreshold() {
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
        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertTrue(responderThread.isSuccess());
        assertFalse(responderThread.isFailed());
        assertNull(responderThread.getFailureReason());
        assertFalse(responderThread.isResponderStopped());

        //result is no output 
        assertEquals("", baos.toString());
    }

    public void testRunResponderInputFailure() {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.inputFailurePattern = "^Test2: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertFalse(responderThread.isSuccess());
        assertTrue(responderThread.isFailed());
        assertEquals("Failed waiting for input prompt: Expected input was not seen",
            responderThread.getFailureReason());
        assertFalse(responderThread.isResponderStopped());
    }
    public void testRunResponderInputFailureMiss() {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.inputFailurePattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = true;
        testResponder.failOnResponseThreshold = true;
        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertFalse(responderThread.isSuccess());
        assertTrue(responderThread.isFailed());
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 5 lines",
            responderThread.getFailureReason());
        assertFalse(responderThread.isResponderStopped());
    }
    public void testRunResponderInputFailureMissNoFail() {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "Test1\nTest1\nTest1\nTest2: blah\nTest1\nTest3: blah\nTest4: bloo".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final testResponder testResponder = new testResponder();
        testResponder.inputFailurePattern = "^TestZ: .*";
        testResponder.inputMaxLines = 5;
        testResponder.inputMaxTimeout = 1000;
        testResponder.failOnInputLinesThreshold = false;
        testResponder.failOnResponseThreshold = true;
        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertTrue(responderThread.isSuccess());
        assertFalse(responderThread.isFailed());
        assertNull(responderThread.getFailureReason());
        assertFalse(responderThread.isResponderStopped());
    }

    public void testRunResponderResponseSuccess() {
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

        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertNull(responderThread.getFailureReason(), responderThread.getFailureReason());
        assertTrue(responderThread.isSuccess());
        assertFalse(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
    }
    public void testRunResponderResponseSuccessMiss() {
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

        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertEquals("Failed waiting for response: Expected input was not seen in 5 lines",
            responderThread.getFailureReason());
        assertFalse(responderThread.isSuccess());
        assertTrue(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
    }
    public void testRunResponderResponseSuccessMissNoFail() {
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

        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertNull(responderThread.getFailureReason(), responderThread.getFailureReason());
        assertTrue(responderThread.isSuccess());
        assertFalse(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
    }
    public void testRunResponderResponseFailure() {
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

        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertEquals("Failed waiting for response: Did not see the correct response",
            responderThread.getFailureReason());
        assertFalse(responderThread.isSuccess());
        assertTrue(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
    }
    public void testRunResponderResponseFailureMiss() {
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

        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertEquals("Failed waiting for response: Expected input was not seen in 5 lines",
            responderThread.getFailureReason());
        assertFalse(responderThread.isSuccess());
        assertTrue(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
    }
    public void testRunResponderResponseFailureMissNoFail() {
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

        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertNull(responderThread.getFailureReason(), responderThread.getFailureReason());
        assertTrue(responderThread.isSuccess());
        assertFalse(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
    }
    public void testRunResponderWrite() {
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

        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertNull(responderThread.getFailureReason(), responderThread.getFailureReason());
        assertTrue(responderThread.isSuccess());
        assertFalse(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
        assertEquals("Test", baos.toString());
    }
    public void testRunResponderWriteOnSuccess() {
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

        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertNull(responderThread.getFailureReason(), responderThread.getFailureReason());
        assertTrue(responderThread.isSuccess());
        assertFalse(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
        assertEquals("Test", baos.toString());
    }
    public void testRunResponderNoWriteOnFailure() {
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

        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertNotNull(responderThread.getFailureReason(), responderThread.getFailureReason());
        assertFalse(responderThread.isSuccess());
        assertTrue(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
        assertEquals("", baos.toString());
    }

    public void testRunResponderWriteAndResponseSuccess() {
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

        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertNull(responderThread.getFailureReason(), responderThread.getFailureReason());
        assertTrue(responderThread.isSuccess());
        assertFalse(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
        assertEquals("Test", baos.toString());
    }

    public void testRunResponderWriteAndResponseFailure() {
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

        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, null);
        responderThread.run();
        assertNotNull(responderThread.getFailureReason(), responderThread.getFailureReason());
        assertFalse(responderThread.isSuccess());
        assertTrue(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
        assertEquals("Test", baos.toString());
    }
    public void testResultHandlerSuccess(){
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
        final ResponderThread.ResultHandler resultHandler = new ResponderThread.ResultHandler() {
            public void handleResult(boolean success, String reason) {
                result[0]=success;
                resultReason[0]=reason;
            }
        };
        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, resultHandler);
        responderThread.run();
        assertNull("reason; " + responderThread.getFailureReason(), responderThread.getFailureReason());
        assertTrue(responderThread.isSuccess());
        assertFalse(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
        assertEquals("Test", baos.toString());
        assertTrue(result[0]);
        assertNull(resultReason[0]);
    }
    public void testResultHandlerInputFailure(){
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
        final ResponderThread.ResultHandler resultHandler = new ResponderThread.ResultHandler() {
            public void handleResult(boolean success, String reason) {
                result[0]=success;
                resultReason[0]=reason;
            }
        };
        final ResponderThread responderThread = new ResponderThread(testResponder, bais, baos, resultHandler);
        responderThread.run();
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 5 lines",
            responderThread.getFailureReason());
        assertFalse(responderThread.isSuccess());
        assertTrue(responderThread.isFailed());
        assertFalse(responderThread.isResponderStopped());
        assertEquals("", baos.toString());
        assertFalse(result[0]);
        assertEquals("Failed waiting for input prompt: Expected input was not seen in 5 lines", resultReason[0]);
    }
}
