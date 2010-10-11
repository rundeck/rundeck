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
* CheckTest.java
* 
* User: greg
* Created: Oct 7, 2009 6:17:01 PM
* $Id$
*/
package com.dtolabs.launcher;

import com.dtolabs.launcher.check.PolicyAnalyzer;
import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * CheckTest is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class CheckTest extends TestCase {
    private File testfile1;

    public void setUp() {
        // Add your code here
        testfile1 = new File("src/test/com/dtolabs/launcher/check/testfile1.properties");
    }

    public void tearDown() {
        // Add your code here
    }

    public void testRequiredParamsParse() throws Check.CheckException {
        // Add your code here
        {
            //basic, no arguments
            final Check.RequiredParams required = new Check.RequiredParams();
            try {
                final boolean result = required.parse(new String[0]);
                assertFalse("parsing should not succeed", result);
            } catch (Check.CheckException e) {
                assertTrue(e.getMessage().contains("is required"));
            }
        }
        {
            //basic, -n arg
            final Check.RequiredParams required = new Check.RequiredParams();
            try {
                final boolean result = required.parse(new String[]{"-n", "node1"});
                assertFalse("parsing should not succeed", result);
            } catch (Check.CheckException e) {
                assertTrue(e.getMessage().contains("is required"));
            }
        }
        {
            //basic, -n arg -N arg
            final Check.RequiredParams required = new Check.RequiredParams();
            try {
                final boolean result = required.parse(new String[]{"-n", "node1", "-N", "node1"});
                assertFalse("parsing should not succeed", result);
            } catch (Check.CheckException e) {
                assertTrue(e.getMessage().contains("is required"));
            }
        }
        {
            //basic, -n arg -N arg -s arg
            final Check.RequiredParams required = new Check.RequiredParams();
            try {
                final boolean result = required.parse(new String[]{"-n", "node1", "-N", "node1", "-s", "server1"});
                assertTrue("parsing succeeded", result);
            } catch (Check.CheckException e) {
                fail("exception: " + e.getMessage());
            }
        }

        {
            //add -q quiet
            final Check.RequiredParams required = new Check.RequiredParams();
            try {
                final boolean result = required.parse(new String[]{"-n", "node1", "-N", "node1", "-s", "server1","-q"});
                assertTrue("parsing succeeded", result);
            } catch (Check.CheckException e) {
                fail("exception: " + e.getMessage());
            }
        }
        {
            //add -v debug
            final Check.RequiredParams required = new Check.RequiredParams();
            try {
                final boolean result = required.parse(new String[]{"-n", "node1", "-N", "node1", "-s", "server1","-v"});
                assertTrue("parsing succeeded", result);
            } catch (Check.CheckException e) {
                fail("exception: " + e.getMessage());
            }
        }
        {
            //add -f force
            final Check.RequiredParams required = new Check.RequiredParams();
            try {
                final boolean result = required.parse(new String[]{"-n", "node1", "-N", "node1", "-s", "server1","-f"});
                assertTrue("parsing succeeded", result);
            } catch (Check.CheckException e) {
                fail("exception: " + e.getMessage());
            }
        }

        //incorrect input parsing

        {
            //basic, -n arg -a arg
            final Check.RequiredParams required = new Check.RequiredParams();
            try {
                final boolean result = required.parse(new String[]{"-n"});
                assertFalse("parsing should not succeed", result);
            } catch (Check.CheckException e) {
                assertTrue("wrong content", e.getMessage().contains("must take a parameter"));
            }
        }
        {
            //basic, -n arg -a arg
            final Check.RequiredParams required = new Check.RequiredParams();
            try {
                final boolean result = required.parse(new String[]{"-n","-x"});
                assertFalse("parsing should not succeed", result);
            } catch (Check.CheckException e) {
                assertTrue("wrong content", e.getMessage().contains("does not have a parameter"));
            }
        }
        {
            //basic, -n arg -a arg
            final Check.RequiredParams required = new Check.RequiredParams();
            try {
                final boolean result = required.parse(new String[]{"-x"});
                assertFalse("parsing should not succeed", result);
            } catch (Check.CheckException e) {
                assertTrue(e.getMessage().contains("unrecognized argument"));
            }
        }

        //check parsing values

        {
            //basic, -n arg -a arg -s arg
            final Check.RequiredParams required = new Check.RequiredParams();
            final boolean result = required.parse(new String[]{"-n", "node1", "-N", "nodehost", "-s", "server1"});
            assertTrue("parsing succeeded", result);
            assertEquals("wrong value", "node1", required.getNodeName());
            assertEquals("wrong value", "nodehost", required.getNodeHostname());
            assertEquals("wrong value", "server1", required.getServerHostname());
            assertFalse("wrong value", required.isDebugFlag());
            assertFalse("wrong value", required.isForceFlag());
            assertFalse("wrong value", required.isQuietFlag());
        }

        //check parsing values

        {
            //basic, -n arg -N arg -s arg
            final Check.RequiredParams required = new Check.RequiredParams();
            final boolean result = required.parse(new String[]{"-n", "node1", "-N", "nodehost", "-s", "server1","-v"});
            assertTrue("parsing succeeded", result);
            assertEquals("wrong value", "node1", required.getNodeName());
            assertEquals("wrong value", "nodehost", required.getNodeHostname());
            assertEquals("wrong value", "server1", required.getServerHostname());
            assertTrue("wrong value", required.isDebugFlag());
            assertFalse("wrong value", required.isForceFlag());
            assertFalse("wrong value", required.isQuietFlag());
        }

        {
            //basic, -n arg -N arg -s arg
            final Check.RequiredParams required = new Check.RequiredParams();
            final boolean result = required.parse(new String[]{"-n", "node1", "-N", "nodehost", "-s", "server1","-q"});
            assertTrue("parsing succeeded", result);
            assertEquals("wrong value", "node1", required.getNodeName());
            assertEquals("wrong value", "nodehost", required.getNodeHostname());
            assertEquals("wrong value", "server1", required.getServerHostname());
            assertTrue("wrong value", required.isQuietFlag());
            assertFalse("wrong value", required.isForceFlag());
            assertFalse("wrong value", required.isDebugFlag());
        }
        {
            //basic, -n arg -N arg -s arg
            final Check.RequiredParams required = new Check.RequiredParams();
            final boolean result = required.parse(new String[]{"-n", "node1", "-N", "nodehost", "-s", "server1","-f"});
            assertTrue("parsing succeeded", result);
            assertEquals("wrong value", "node1", required.getNodeName());
            assertEquals("wrong value", "nodehost", required.getNodeHostname());
            assertEquals("wrong value", "server1", required.getServerHostname());
            assertTrue("wrong value", required.isForceFlag());
            assertFalse("wrong value", required.isDebugFlag());
            assertFalse("wrong value", required.isQuietFlag());
        }

        {
            //multiple flags
            final Check.RequiredParams required = new Check.RequiredParams();
            final boolean result = required.parse(new String[]{"-n", "node1", "-N", "nodehost", "-s", "server1", "-fv"});
            assertTrue("parsing succeeded", result);
            assertEquals("wrong value", "node1", required.getNodeName());
            assertEquals("wrong value", "nodehost", required.getNodeHostname());
            assertEquals("wrong value", "server1", required.getServerHostname());
            assertTrue("wrong value", required.isForceFlag());
            assertTrue("wrong value", required.isDebugFlag());
            assertFalse("wrong value", required.isQuietFlag());
        }

        {
            //multiple flags
            final Check.RequiredParams required = new Check.RequiredParams();
            final boolean result = required.parse(new String[]{"-n", "node1", "-N", "nodehost", "-s", "server1", "-fvq"});
            assertTrue("parsing succeeded", result);
            assertEquals("wrong value", "node1", required.getNodeName());
            assertEquals("wrong value", "nodehost", required.getNodeHostname());
            assertEquals("wrong value", "server1", required.getServerHostname());
            assertTrue("wrong value", required.isForceFlag());
            assertTrue("wrong value", required.isDebugFlag());
            assertTrue("wrong value", required.isQuietFlag());
        }


    }
    

    public void testtestPropertiesFile() throws Exception {
        {
            //test simple properties exist
            final List<String> exists = Arrays.asList("a-property");

            final boolean[] seen = new boolean[]{false};
            Check.testPropertiesFile(testfile1, exists, null, false, false, null, new TestPolicy() {
                @Override
                public int expectPropertiesExist(Collection<String> keys, Properties props) {
                    assertEquals(exists, keys);
                    seen[0] = true;
                    return 1;
                }
            }
                , new TestReporter() {
                    @Override
                    public void reportNominal(String message) {
                        assertEquals("1/1 properties OK", message);
                    }
                });
            assertTrue("expectPropertiesExist was not called", seen[0]);
        }
        {
            //test simple property values
            final Properties testProps = new Properties();
            testProps.setProperty("a-property", "some value");

            final boolean[] seen = new boolean[]{false};
            Check.testPropertiesFile(testfile1, null, testProps, false, false, null, new TestPolicy() {
                @Override
                public int expectPropertyValues(Properties expectedProps, Properties props) {
                    assertEquals(testProps, expectedProps);
                    seen[0] = true;
                    return 1;
                }
            }
                , new TestReporter() {
                    @Override
                    public void reportNominal(String message) {
                        assertEquals("1/1 properties OK", message);
                    }
                });
            assertTrue("expectPropertyValues was not called", seen[0]);
        }
        {
            //test simple properties exist (require)
            final List<String> exists = Arrays.asList("a-property");

            final boolean[] seen = new boolean[]{false};

            Check.testPropertiesFile(testfile1, exists, null, false, true, null, new TestPolicy() {
                @Override
                public int requirePropertiesExist(Collection<String> keys, Properties props) {
                    assertEquals(exists, keys);
                    seen[0] = true;
                    return 1;
                }
            }
                , new TestReporter() {
                    @Override
                    public void reportNominal(String message) {
                        assertEquals("1/1 required properties OK", message);
                    }
                });
            assertTrue("requirePropertiesExist was not called", seen[0]);
        }
        {
            //test simple property values (require)
            final Properties testProps = new Properties();
            testProps.setProperty("a-property", "some value");

            final boolean[] seen = new boolean[]{false};
            Check.testPropertiesFile(testfile1, null, testProps, false, true, null, new TestPolicy() {
                @Override
                public int requirePropertyValues(Properties expectedProps, Properties props) {
                    assertEquals(testProps, expectedProps);
                    seen[0] = true;
                    return 1;
                }
            }
                , new TestReporter() {
                    @Override
                    public void reportNominal(String message) {
                        assertEquals("1/1 required properties OK", message);
                    }
                });
            assertTrue("requirePropertyValues was not called", seen[0]);
        }

        //test input properties

        {
            //expected keys
            final List<String> exists = Arrays.asList("a-property");

            final boolean[] seen = new boolean[]{false, false};
            final Properties input = new Properties();
            input.setProperty("a-property", "another value");

            Check.testPropertiesFile(testfile1, exists, null, false, false, input, new TestPolicy() {
                @Override
                public int expectPropertiesExist(Collection<String> keys, Properties props) {
                    assertEquals(exists, keys);
                    seen[0] = true;
                    return 1;
                }

                @Override
                public int expectPropertyValues(Properties expectedProps, Properties props) {
                    assertTrue(expectedProps.containsKey("a-property"));
                    assertEquals("another value", expectedProps.getProperty("a-property"));
                    seen[1] = true;
                    return 1;
                }
            }
                , new TestReporter() {
                    @Override
                    public void reportNominal(String message) {
                        assertEquals("2/2 properties OK", message);
                    }
                });
            assertTrue("expectPropertiesExist was not called", seen[0]);
            assertTrue("expectPropertyValues was not called", seen[1]);
        }
        {
            //expected props
            final Properties testProps = new Properties();
            testProps.setProperty("a-property", "some value");
            final Properties input = new Properties();
            input.setProperty("a-property", "another value");

            final boolean[] seen = new boolean[]{false};
            Check.testPropertiesFile(testfile1, null, testProps, false, false, input, new TestPolicy() {
                @Override
                public int expectPropertyValues(Properties expectedProps, Properties props) {
                    assertTrue(expectedProps.containsKey("a-property"));
                    assertEquals("another value", expectedProps.getProperty("a-property"));
                    seen[0] = true;
                    return 1;
                }
            }
                , new TestReporter() {
                    @Override
                    public void reportNominal(String message) {
                        assertEquals("1/1 properties OK", message);
                    }
                });
            assertTrue("expectPropertyValues was not called", seen[0]);
        }
    }

    private class TestReporter implements Check.Reporter {
        public void error(String message) {
        }

        public void warn(String message) {
        }

        public void log(String message) {
        }

        public void beginCheckOnFile(File file) {
        }

        public void beginCheckOnDirectory(File dir) {
        }

        public void beginCheckOnProperties(File file) {
        }

        public void expectedFile(File file) {
        }

        public void expectedDirectory(File file) {
        }

        public void expectedPropertyValue(String key, String value) {
        }

        public void missingFile(File file, boolean invalidated) {
        }

        public void notAFile(File file, boolean invalidated) {
        }

        public void incorrectFile(File file, boolean invalidated) {
        }

        public void missingDirectory(File dir, boolean invalidated) {
        }

        public void notADirectory(File dir, boolean invalidated) {
        }

        public void incorrectDirectory(File dir, boolean invalidated) {
        }

        public void incorrectPropertyValue(String key, String value, String expected, boolean invalidated) {
        }

        public void missingPropertyValue(String key, String expected, boolean invalidated) {
        }

        public void reportNominal(String message) {
        }

        public void reportMissing(String message, boolean invalidated) {
        }

        public void reportError(String message, boolean invalidated) {
        }
    }

    private class TestPolicy implements PolicyAnalyzer {
        public boolean requireFileExists(File dir, boolean directory) {
            fail("requireFileExists was called: " + dir.getAbsolutePath());
            return false;
        }

        public boolean expectFileExists(File dir, boolean directory) {
            fail("expectFileExists was called: " + dir.getAbsolutePath());
            return false;
        }

        public boolean expectPropertyValue(String key, String value, Properties props) {
            fail("expectPropertyValue was called: " + key + ", " + value);
            return false;
        }

        public boolean requirePropertyValue(String key, String value, Properties props) {
            fail("requirePropertyValue was called: " + key + ", " + value);
            return false;
        }

        public int expectPropertyValues(Properties expectedProps, Properties props) {
            fail("expectPropertyValues was called: " + expectedProps + ", " + props);
            return 0;
        }

        public int requirePropertyValues(Properties expectedProps, Properties props) {
            fail("requirePropertyValues was called: " + expectedProps + ", " + props);
            return 0;
        }

        public int expectPropertiesExist(Collection<String> keys, Properties props) {
            fail("expectPropertiesExist was called: " + keys + ", " + props);
            return 0;
        }

        public int requirePropertiesExist(Collection<String> keys, Properties props) {
            fail("requirePropertiesExist was called: " + keys + ", " + props);
            return 0;
        }
    }
}
