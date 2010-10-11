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

/*
* CheckerImplTest.java
* 
* User: greg
* Created: Oct 8, 2009 3:08:55 PM
* $Id$
*/
package com.dtolabs.launcher.check;

import junit.framework.TestCase;

import java.io.File;
import java.util.Properties;
import java.util.ArrayList;

/**
 * CheckerImplTest is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class CheckerImplTest extends TestCase {
    CheckerImpl test1;
    File testfile1;
    File testfile2;
    File testdir1;
    File testdir2;

    public void setUp() {
        /** testfile1 is a test properties file */
        testfile1 = new File("src/test/com/dtolabs/launcher/check/testfile1.properties");
        /** testfile2 should not exist */
        testfile2 = new File("src/test/com/dtolabs/launcher/check/testfile2.properties");
        /**testdir1 should exist */
        testdir1 = new File("src/test/com/dtolabs/launcher/check");
        /** testdir2 should not exist*/
        testdir2 = new File("src/test/com/dtolabs/launcher/check/nonexistent");
    }

    public void tearDown() {
        // Add your code here
    }

    public void testCheckPropertyValues() {
        // Add your code here

        Properties expected = new Properties();
        Properties actual = new Properties();
        expected.setProperty("a-property", "some value");
        expected.setProperty("b-property", "another value");

        actual.setProperty("a-property", "some value");
        actual.setProperty("b-property", "another value");
        {
            //two correct values
            CheckerImpl test1 = new CheckerImpl(new FailListener(){
                @Override
                public void expectedPropertyValue(String key, String value) {
                    if("a-property".equals(key)) {
                        assertEquals("some value", value);
                    }else if("b-property".equals(key)) {
                        assertEquals("another value", value);
                    }else {
                        fail("incorrect key: " + key);
                    }
                }
            });
            int result=test1.checkPropertyValues(expected, actual);
            assertEquals(2, result);
        }

        {
            //one incorrect value
            actual.setProperty("b-property", "not equal value");
            CheckerImpl test1 = new CheckerImpl(new FailListener() {
                @Override
                public void expectedPropertyValue(String key, String value) {
                    assertEquals("a-property", key);
                    assertEquals("some value",value);
                }

                @Override
                public void incorrectPropertyValue(String key, String value, String expected) {
                    assertEquals("not equal value", value);
                    assertEquals("another value", expected);
                }
            });
            int result = test1.checkPropertyValues(expected, actual);
            assertEquals(1, result);
        }
        {
            //one missing value
            actual.remove("b-property");
            CheckerImpl test1 = new CheckerImpl(new FailListener() {
                @Override
                public void expectedPropertyValue(String key, String value) {
                    assertEquals("a-property", key);
                    assertEquals("some value", value);
                }

                @Override
                public void missingPropertyValue(String key, String expected) {
                    assertEquals("b-property", key);
                    assertEquals("another value", expected);
                }
            });
            int result = test1.checkPropertyValues(expected, actual);
            assertEquals(1, result);
        }
    }

    public void testCheckPropertiesExist() {

        ArrayList<String> expected = new ArrayList<String>();
        Properties actual = new Properties();
        expected.add("a-property");
        expected.add("b-property");

        actual.setProperty("a-property", "some value");
        actual.setProperty("b-property", "another value");
        {
            //two existing values
            CheckerImpl test1 = new CheckerImpl(new FailListener() {
                @Override
                public void expectedPropertyValue(String key, String value) {
                    if ("a-property".equals(key)) {
                        assertEquals("some value", value);
                    } else if ("b-property".equals(key)) {
                        assertEquals("another value", value);
                    } else {
                        fail("incorrect key: " + key);
                    }
                }
            });
            int result = test1.checkPropertiesExist(expected, actual);
            assertEquals(2, result);
        }
        {
            //one existing values
            actual.remove("b-property");
            CheckerImpl test1 = new CheckerImpl(new FailListener() {
                @Override
                public void expectedPropertyValue(String key, String value) {
                    assertEquals("a-property", key);
                    assertEquals("some value", value);
                }

                @Override
                public void missingPropertyValue(String key, String value) {
                    assertEquals("b-property", key);
                    assertNull(value);
                }
            });
            int result = test1.checkPropertiesExist(expected, actual);
            assertEquals(1, result);
        }
    }

    public void testCheckPropertyValue() {
        ArrayList<String> expected = new ArrayList<String>();
        Properties actual = new Properties();
        expected.add("a-property");
        expected.add("b-property");

        actual.setProperty("a-property", "some value");
        actual.setProperty("b-property", "another value");
        {
            //correct value
            CheckerImpl test1 = new CheckerImpl(new FailListener() {
                @Override
                public void expectedPropertyValue(String key, String value) {
                    assertEquals("a-property", key);
                    assertEquals("some value", value);
                }

            });
            assertTrue(test1.checkPropertyValue("a-property", "some value", actual));
        }
        {
            //correct value
            CheckerImpl test1 = new CheckerImpl(new FailListener() {
                @Override
                public void expectedPropertyValue(String key, String value) {
                    assertEquals("b-property", key);
                    assertEquals("another value", value);
                }

            });
            assertTrue(test1.checkPropertyValue("b-property", "another value", actual));
        }
        {
            //incorrect value
            CheckerImpl test1 = new CheckerImpl(new FailListener() {
                @Override
                public void incorrectPropertyValue(String key, String value, String expected) {
                    assertEquals("a-property", key);
                    assertEquals("some value", value);
                    assertEquals("wrong value", expected);
                }
            });
            assertFalse(test1.checkPropertyValue("a-property", "wrong value", actual));
        }

        {
            //missing value
            CheckerImpl test1 = new CheckerImpl(new FailListener() {
                @Override
                public void missingPropertyValue(String key, String expected) {
                    assertEquals("c-property", key);
                    assertEquals("c prop value", expected);
                }
            });
            assertFalse(test1.checkPropertyValue("c-property", "c prop value", actual));
        }

    }

    public void testCheckFileExists() {
        // test file exists=true
        test1 = new CheckerImpl(new FailListener(){
            @Override
            public void expectedFile(File file) {
                assertEquals(file, testfile1);
            }

        });
        assertTrue(test1.checkFileExists(testfile1, false));

        //test file exists==false
        test1 = new CheckerImpl(new FailListener() {
            @Override
            public void missingFile(File file) {
                assertEquals(file, testfile2);
            }
        });
        assertFalse(test1.checkFileExists(testfile2, false));

        //test file exists==true, but is a directory
        test1 = new CheckerImpl(new FailListener() {
            @Override
            public void notAFile(File file) {
                assertEquals(file, testdir1);
            }
        });
        assertFalse(test1.checkFileExists(testdir1, false));

        /*
        directory test
         */

        //check dir exists==true
        test1 = new CheckerImpl(new FailListener() {
            @Override
            public void expectedDirectory(File file) {
                assertEquals(file, testdir1);
            }
        });
        assertTrue(test1.checkFileExists(testdir1, true));

        //test dir exists==false
        test1 = new CheckerImpl(new FailListener() {
            @Override
            public void missingDirectory(File file) {
                assertEquals(file, testdir2);
            }

        });
        assertFalse(test1.checkFileExists(testdir2, true));

        //test dir exists==true, but is a file
        test1 = new CheckerImpl(new FailListener() {

            @Override
            public void notADirectory(File file) {
                assertEquals(file, testfile1);
            }
        });
        assertFalse(test1.checkFileExists(testfile1, true));
    }

    static class BaseListener implements CheckerListener {
        public void beginCheckOnFile(File file) {
        }

        public void beginCheckOnDirectory(File file) {
        }

        public void beginCheckOnProperties(File file) {
        }

        public void expectedFile(File file) {
        }

        public void expectedDirectory(File file) {
        }

        public void expectedPropertyValue(String key, String value) {
        }

        public void missingFile(File file) {
        }

        public void notAFile(File file) {
        }

        public void missingDirectory(File file) {
        }

        public void notADirectory(File file) {
        }

        public void incorrectPropertyValue(String key, String value, String expected) {
        }

        public void missingPropertyValue(String key, String expected) {
        }
    }
    static class FailListener extends BaseListener{
        @Override
        public void beginCheckOnFile(File file) {
            fail("beginCheckOnFile was called");
        }

        @Override
        public void beginCheckOnDirectory(File file) {
            fail("beginCheckOnDirectory was called");
        }

        @Override
        public void beginCheckOnProperties(File file) {
            fail("beginCheckOnProperties was called");
        }

        @Override
        public void expectedFile(File file) {
            fail("expectedFile was called");
        }

        @Override
        public void expectedDirectory(File file) {
            fail("expectedDirectory was called");
        }

        @Override
        public void expectedPropertyValue(String key, String value) {
            fail("expectedPropertyValue was called");
        }

        @Override
        public void missingFile(File file) {
            fail("missingFile was called");
        }

        @Override
        public void notAFile(File file) {
            fail("notAFile was called");
        }

        @Override
        public void missingDirectory(File file) {
            fail("missingDirectory was called");
        }

        @Override
        public void notADirectory(File file) {
            fail("notADirectory was called");
        }

        @Override
        public void incorrectPropertyValue(String key, String value, String expected) {
            fail("incorrectPropertyValue was called");
        }

        @Override
        public void missingPropertyValue(String key, String expected) {
            fail("missingPropertyValue was called");
        }
    }
}
