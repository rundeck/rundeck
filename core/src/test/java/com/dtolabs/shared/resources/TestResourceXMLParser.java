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

package com.dtolabs.shared.resources;
/*
* TestResourceXMLParser.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Apr 26, 2010 3:58:04 PM
* $Id$
*/

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class TestResourceXMLParser extends TestCase {
    File dnefile1;
    File testfile1;
    File testfile2;
    File invalidfile1;
    File invalidfile2;

    public TestResourceXMLParser(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestResourceXMLParser.class);
    }

    protected void setUp() throws Exception {
        dnefile1 = new File("test-does-not-exist.xml");
        testfile1 = new File("src/test/resources/com/dtolabs/shared/resources/test-resources1.xml");
        testfile2 = new File("src/test/resources/com/dtolabs/shared/resources/test-resources2.xml");
        invalidfile1 = new File("src/test/resources/com/dtolabs/shared/resources/test-resources-invalid1.xml");
        invalidfile2 = new File("src/test/resources/com/dtolabs/shared/resources/test-resources-invalid2.xml");
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testParse() throws Exception {
        {//test file that does not exist
            try {
                ResourceXMLParser resourceXMLParser= new ResourceXMLParser(dnefile1);
                resourceXMLParser.parse();
                fail("Should have thrown an Exception");
            } catch (FileNotFoundException e) {
                assertNotNull(e);
            }
        }
        { //test basic file without receiver
            ResourceXMLParser resourceXMLParser= new ResourceXMLParser(testfile1);
            resourceXMLParser.parse();
        }
        { //test basic file with receiver
            ResourceXMLParser resourceXMLParser= new ResourceXMLParser(testfile1);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver(){
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] =true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] =true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 1, items.size());
        }
        { //test basic file with receiver, return false from resourceParsed
            ResourceXMLParser resourceXMLParser = new ResourceXMLParser(testfile1);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver() {
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] = true;
                    return false;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] = true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 1, items.size());
        }


        /**************
         *  test attributes
         ***************/

        { //test attributes of node
            ResourceXMLParser resourceXMLParser = new ResourceXMLParser(testfile2);
            final ArrayList<ResourceXMLParser.Entity> items = new ArrayList<ResourceXMLParser.Entity>();
            final boolean[] resourceParsedCalled = new boolean[]{false};
            final boolean[] resourcesParsed = new boolean[]{false};
            resourceXMLParser.setReceiver(new ResourceXMLReceiver() {
                public boolean resourceParsed(ResourceXMLParser.Entity entity) {
                    items.add(entity);
                    resourceParsedCalled[0] = true;
                    return true;
                }

                public void resourcesParsed(ResourceXMLParser.EntitySet entities) {
                    resourcesParsed[0] = true;
                }
            });
            resourceXMLParser.parse();
            assertTrue(resourceParsedCalled[0]);
            assertTrue(resourcesParsed[0]);
            assertEquals("Wrong size", 1, items.size());
            final ResourceXMLParser.Entity entity = items.get(0);
            assertEquals("wrong node type", "node", entity.getResourceType());
            assertEquals("wrong name", "node1", entity.getName());
            assertEquals("wrong properties size", 14, entity.getProperties().size());
            assertEquals("wrong value", "node1", entity.getProperty(ResourceXMLConstants.COMMON_NAME));
            assertEquals("wrong value", "description1", entity.getProperty(ResourceXMLConstants.COMMON_DESCRIPTION));
            assertEquals("wrong value", "tag1,tag2", entity.getProperty(ResourceXMLConstants.COMMON_TAGS));
            assertEquals("wrong value", "hostname1", entity.getProperty(ResourceXMLConstants.NODE_HOSTNAME));
            assertEquals("wrong value", "username1", entity.getProperty(ResourceXMLConstants.NODE_USERNAME));
            assertEquals("wrong value", "osArch1", entity.getProperty(ResourceXMLConstants.NODE_OS_ARCH));
            assertEquals("wrong value", "osFamily1", entity.getProperty(ResourceXMLConstants.NODE_OS_FAMILY));
            assertEquals("wrong value", "osName1", entity.getProperty(ResourceXMLConstants.NODE_OS_NAME));
            assertEquals("wrong value", "osVersion1", entity.getProperty(ResourceXMLConstants.NODE_OS_VERSION));
            assertEquals("wrong value", "EditURL", entity.getProperty(ResourceXMLConstants.NODE_EDIT_URL));
            assertEquals("wrong value", "RemoteURL", entity.getProperty(ResourceXMLConstants.NODE_REMOTE_URL));
            assertEquals("wrong value", "testvalue", entity.getProperty("testattribute"));
            assertEquals("wrong value", "test value2", entity.getProperty("testattribute2"));
            assertEquals("wrong value", "test value3", entity.getProperty("testattribute3"));
        }


    }
}