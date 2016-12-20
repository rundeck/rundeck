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
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;

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
        new File("build/test-target").mkdirs();
        test1 = new File("build/test-target/TestResourceXMLGenerator-test1.xml");
        test2 = new File("build/test-target/doesnotexist/test.xml");
        reader = new SAXReader(false);
    }

    protected void tearDown() throws Exception {
        if(test1.exists()){
//            test1.deleteOnExit();
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
            assertEquals("", root.selectSingleNode("node/@tags").getStringValue());
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
            assertEquals("a, d", root.selectSingleNode("node/@tags").getStringValue());
            assertEquals("test desc", root.selectSingleNode("node/@description").getStringValue());
            assertEquals("test arch", root.selectSingleNode("node/@osArch").getStringValue());
            assertEquals("test fam", root.selectSingleNode("node/@osFamily").getStringValue());
            assertEquals("test osname", root.selectSingleNode("node/@osName").getStringValue());
            assertEquals("test vers", root.selectSingleNode("node/@osVersion").getStringValue());
            assertEquals("test user", root.selectSingleNode("node/@username").getStringValue());
        }
        {  //test to outputstream
            assertTrue(test1.delete());
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
            assertEquals("a, d", root.selectSingleNode("node/@tags").getStringValue());
            assertEquals("test desc", root.selectSingleNode("node/@description").getStringValue());
            assertEquals("test arch", root.selectSingleNode("node/@osArch").getStringValue());
            assertEquals("test fam", root.selectSingleNode("node/@osFamily").getStringValue());
            assertEquals("test osname", root.selectSingleNode("node/@osName").getStringValue());
            assertEquals("test vers", root.selectSingleNode("node/@osVersion").getStringValue());
            assertEquals("test user", root.selectSingleNode("node/@username").getStringValue());
        }
        assertTrue(test1.delete());
        {
            //test arbitrary attributes
            final ResourceXMLGenerator gen = new ResourceXMLGenerator(test1);
            //add a node
            final NodeEntryImpl node = new NodeEntryImpl("test1", "test1name");
            node.setDescription("test desc");
            node.setUsername("test user");
            final HashMap<String, String> attributes = new HashMap<String, String>();
            attributes.put("myattr", "myattrvalue");
            attributes.put("-asdf", "test value");
            node.setAttributes(attributes);

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
            assertEquals("myattrvalue", root.selectSingleNode("node/@myattr").getStringValue());
            //weird attr name will be set as attribute subelement
            assertNotNull( root.selectSingleNode("node/attribute"));
            assertEquals(1, root.selectNodes("node/attribute").size());
            assertEquals("-asdf", root.selectSingleNode("node/attribute/@name").getStringValue());
            assertEquals("test value", root.selectSingleNode("node/attribute/@value").getStringValue());
        }
    }
    public void testTags() throws Exception {
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
        tags.add("b");
        node.setTags(tags);
        node.getTags().add("c");
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
        assertEquals("a, b, c", root.selectSingleNode("node/@tags").getStringValue());
        assertEquals("test desc", root.selectSingleNode("node/@description").getStringValue());
        assertEquals("test arch", root.selectSingleNode("node/@osArch").getStringValue());
        assertEquals("test fam", root.selectSingleNode("node/@osFamily").getStringValue());
        assertEquals("test osname", root.selectSingleNode("node/@osName").getStringValue());
        assertEquals("test vers", root.selectSingleNode("node/@osVersion").getStringValue());
        assertEquals("test user", root.selectSingleNode("node/@username").getStringValue());
    }


    /**
     * Attributes with a ":" are generated in separate &lt;attribute .. /&gt; elements
     * @throws Exception
     */
    public void testAttributeCharsColon() throws Exception {
        //test attributes with xml special chars
        final ResourceXMLGenerator gen = new ResourceXMLGenerator(test1);
        //add a node
        final NodeEntryImpl node = new NodeEntryImpl("test1", "test1name");
        node.setDescription("test desc");
        node.setUsername("test user");
        final HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("my:attr", "myattrvalue");
        attributes.put("another:attribute", "test value");
        node.setAttributes(attributes);

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
        //weird attr name will be set as attribute subelement
        assertNotNull( root.selectSingleNode("node/attribute"));
        List list = root.selectNodes("node/attribute");
        assertEquals(2, list.size());

        Node attr1 = (Node) list.get(0);
        assertEquals("another:attribute", attr1.selectSingleNode("@name").getStringValue());
        assertEquals("test value", attr1.selectSingleNode("@value").getStringValue());

        Node attr2 = (Node)list.get(1);
        assertEquals("my:attr", attr2.selectSingleNode("@name").getStringValue());
        assertEquals("myattrvalue", attr2.selectSingleNode("@value").getStringValue());
    }

    /**
     * Attributes with a "." are generated in separate &lt;attribute .. /&gt; elements
     * @throws Exception
     */
    public void testAttributeCharsPeriod() throws Exception {
        //test attributes with xml special chars
        final ResourceXMLGenerator gen = new ResourceXMLGenerator(test1);
        //add a node
        final NodeEntryImpl node = new NodeEntryImpl("test1", "test1name");
        node.setDescription("test desc");
        node.setUsername("test user");
        final HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("my.attr", "myattrvalue");
        attributes.put("another.attribute", "test value");
        node.setAttributes(attributes);

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
        //weird attr name will be set as attribute subelement
        assertNotNull("expected node/attribute elements", root.selectSingleNode("node/attribute"));
        List list = root.selectNodes("node/attribute");
        assertEquals(2, list.size());

        for (Object o : list) {
            Node attr1 = (Node) list.get(0);
            String name = attr1.selectSingleNode("@name").getStringValue();
            if("another.attribute".equals(name)){
                assertEquals("another.attribute", name);
                assertEquals("test value", attr1.selectSingleNode("@value").getStringValue());
            }else{
                assertEquals("my.attr", name);
                assertEquals("myattrvalue", attr1.selectSingleNode("@value").getStringValue());
            }
        }


    }
}
