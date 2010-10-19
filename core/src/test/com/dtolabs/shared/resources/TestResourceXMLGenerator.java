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
* TestResourceXMLGenerator.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Oct 7, 2010 2:37:09 PM
* 
*/

import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.HashMap;

public class TestResourceXMLGenerator extends TestCase {
    ResourceXMLGenerator resourceXMLGenerator;
    private File test1;
    private File test2;
    private SAXReader reader;

    public TestResourceXMLGenerator(final String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestResourceXMLGenerator.class);
    }

    protected void setUp() throws Exception {
        test1 = new File("target/TestResourceXMLGenerator-test1.xml");
        test2 = new File("target/doesnotexist/test.xml");
        reader = new SAXReader(true);
        reader.setEntityResolver(ResourceXMLParser.createEntityResolver());
    }

    protected void tearDown() throws Exception {
        if(test1.exists()){
            test1.deleteOnExit();
        }
    }

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(suite());
    }


    public void testResourceXMLGenerator() throws Exception {
        {
            final ResourceXMLGenerator gen = new ResourceXMLGenerator(test2);
            try {
                gen.generate();
                fail("Should throw IOException due to missing dir");
            } catch (IOException e) {
                assertNotNull(e);
            }
        }
        {
            final ResourceXMLGenerator gen = new ResourceXMLGenerator(test1);
            gen.generate();
            assertTrue(test1.exists());
            assertTrue(test1.isFile());
        }
        assertTrue(test1.delete());
        {
            final ResourceXMLGenerator gen = new ResourceXMLGenerator(test1);
            //add a node
            final NodeEntryImpl node = new NodeEntryImpl("test1", "test1name");
            gen.addNode(node);
            gen.generate();
            assertTrue(test1.exists());
            assertTrue(test1.isFile());
            //assert contents
            final Document d = reader.read(test1);
            assertNotNull(d);
            final Element root = d.getRootElement();
            assertEquals("project", root.getName());
            assertEquals(1, root.selectNodes("/project/*").size());
            assertEquals(1, root.selectNodes("node").size());
            assertEquals("test1name", root.selectSingleNode("node/@name").getStringValue());
            assertEquals("test1", root.selectSingleNode("node/@hostname").getStringValue());
            assertEquals("Node", root.selectSingleNode("node/@type").getStringValue());
            assertEquals("", root.selectSingleNode("node/@description").getStringValue());
            assertEquals("", root.selectSingleNode("node/@osArch").getStringValue());
            assertEquals("", root.selectSingleNode("node/@osFamily").getStringValue());
            assertEquals("", root.selectSingleNode("node/@osName").getStringValue());
            assertEquals("", root.selectSingleNode("node/@osVersion").getStringValue());
            assertEquals("", root.selectSingleNode("node/@username").getStringValue());
        }
        assertTrue(test1.delete());
        {
            final ResourceXMLGenerator gen = new ResourceXMLGenerator(test1);
            //add a node
            final NodeEntryImpl node = new NodeEntryImpl("test1", "test1name");
            node.setDescription("test desc");
            node.setOsArch("test arch");
            node.setOsFamily("test fam");
            node.setOsName("test osname");
            node.setOsVersion("test vers");
            final HashSet<String> tags = new HashSet<String>();
            tags.add("a");
            tags.add("d");
            node.setTags(tags);
            node.setType("TestNode");
            node.setUsername("test user");

            gen.addNode(node);
            gen.generate();
            assertTrue(test1.exists());
            assertTrue(test1.isFile());
            //assert contents
            final Document d = reader.read(test1);
            assertNotNull(d);
            final Element root = d.getRootElement();
            assertEquals("project", root.getName());
            assertEquals(1, root.selectNodes("/project/*").size());
            assertEquals(1, root.selectNodes("node").size());
            assertEquals("test1name", root.selectSingleNode("node/@name").getStringValue());
            assertEquals("test1", root.selectSingleNode("node/@hostname").getStringValue());
            assertEquals("a,d", root.selectSingleNode("node/@tags").getStringValue());
            assertEquals("TestNode", root.selectSingleNode("node/@type").getStringValue());
            assertEquals("test desc", root.selectSingleNode("node/@description").getStringValue());
            assertEquals("test arch", root.selectSingleNode("node/@osArch").getStringValue());
            assertEquals("test fam", root.selectSingleNode("node/@osFamily").getStringValue());
            assertEquals("test osname", root.selectSingleNode("node/@osName").getStringValue());
            assertEquals("test vers", root.selectSingleNode("node/@osVersion").getStringValue());
            assertEquals("test user", root.selectSingleNode("node/@username").getStringValue());
        }
        {  //test to outputstream
            assertTrue(test1.delete());
            final ResourceXMLGenerator gen = new ResourceXMLGenerator(new FileOutputStream(test1));
            //add a node
            final NodeEntryImpl node = new NodeEntryImpl("test1", "test1name");
            node.setDescription("test desc");
            node.setOsArch("test arch");
            node.setOsFamily("test fam");
            node.setOsName("test osname");
            node.setOsVersion("test vers");
            final HashSet<String> tags = new HashSet<String>();
            tags.add("a");
            tags.add("d");
            node.setTags(tags);
            node.setType("TestNode");
            node.setUsername("test user");

            gen.addNode(node);
            gen.generate();
            assertTrue(test1.exists());
            assertTrue(test1.isFile());
            //assert contents
            final Document d = reader.read(test1);
            assertNotNull(d);
            final Element root = d.getRootElement();
            assertEquals("project", root.getName());
            assertEquals(1, root.selectNodes("/project/*").size());
            assertEquals(1, root.selectNodes("node").size());
            assertEquals("test1name", root.selectSingleNode("node/@name").getStringValue());
            assertEquals("test1", root.selectSingleNode("node/@hostname").getStringValue());
            assertEquals("a,d", root.selectSingleNode("node/@tags").getStringValue());
            assertEquals("TestNode", root.selectSingleNode("node/@type").getStringValue());
            assertEquals("test desc", root.selectSingleNode("node/@description").getStringValue());
            assertEquals("test arch", root.selectSingleNode("node/@osArch").getStringValue());
            assertEquals("test fam", root.selectSingleNode("node/@osFamily").getStringValue());
            assertEquals("test osname", root.selectSingleNode("node/@osName").getStringValue());
            assertEquals("test vers", root.selectSingleNode("node/@osVersion").getStringValue());
            assertEquals("test user", root.selectSingleNode("node/@username").getStringValue());
        }
    }

    public void testSettings() throws Exception{
        {
            final ResourceXMLGenerator gen = new ResourceXMLGenerator(test1);
            //add a node
            final NodeEntryImpl node = new NodeEntryImpl("test1", "test1name");
            node.setDescription("test desc");
            node.setOsArch("test arch");
            node.setOsFamily("test fam");
            node.setOsName("test osname");
            node.setOsVersion("test vers");
            node.setSettings(new HashMap<String, String>());
            node.getSettings().put("testSetting", "testValue");

            gen.addNode(node);
            gen.generate();
            assertTrue(test1.exists());
            assertTrue(test1.isFile());
            //assert contents
            final Document d = reader.read(test1);
            assertNotNull(d);
            final Element root = d.getRootElement();
            assertEquals("project", root.getName());
            assertEquals(2, root.selectNodes("/project/*").size());
            assertEquals(1, root.selectNodes("node").size());
            assertEquals(1, root.selectNodes("setting").size());
            assertEquals(1, root.selectNodes("node/resources/resource").size());
            assertEquals("testSetting", root.selectSingleNode("node/resources/resource/@name").getStringValue());
            assertEquals("Setting", root.selectSingleNode("node/resources/resource/@type").getStringValue());
            
            assertEquals("testSetting", root.selectSingleNode("setting/@name").getStringValue());
            assertEquals("Setting", root.selectSingleNode("setting/@type").getStringValue());
            assertEquals("testValue", root.selectSingleNode("setting/@settingValue").getStringValue());
        }
    }
}