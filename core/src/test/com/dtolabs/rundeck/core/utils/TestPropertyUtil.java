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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.tools.ant.Project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * TestPropertyUtil
 */
public class TestPropertyUtil extends TestCase {
    private final File propertyFile;
    private final Properties properties;

    public TestPropertyUtil(final String name) throws IOException {
        super(name);
        propertyFile = File.createTempFile("prop1", "properties");
        properties = new Properties();
        properties.put("foo", "shizzle");
        properties.put("bar", "madizzle");
        properties.put("baz", "luzizle");
        properties.put("zab", "${foo}-${bar}");
        properties.store(new FileOutputStream(propertyFile), "test properties");
    }

    public static Test suite() {
        return new TestSuite(TestPropertyUtil.class);
    }


    public void testExpand() {
        final Properties expanded = PropertyUtil.expand(properties);
        assertEquals(expanded.size(), properties.size());
        assertEquals("shizzle-madizzle", expanded.get("zab"));

        String str = PropertyUtil.expand("${foo}", properties);
        assertEquals("shizzle", str);

        Project project = new Project();
        project.setProperty("foozle", "foo");
        project.setProperty("boozle", "boo");
        project.setProperty("droozle", "${boozle}${foozle}");
        str = PropertyUtil.expand("${foozle}-${boozle}-${droozle}", project);
        assertEquals("expanded was: " + str, "foo-boo-boofoo", str);

        //try to expand properties with embedded reference that does not resolve
        Properties props = new Properties();
        props.put("a", "bcd");
        props.put("test1", "${a}");
        props.put("test2", "test-${b}");
        props.put("test3", "${test3}");

        final Properties props2 = PropertyUtil.expand(props);
        assertEquals("bcd", props2.getProperty("a"));
        assertEquals("bcd", props2.getProperty("test1"));
        assertEquals("test-${b}", props2.getProperty("test2"));
        assertEquals("${test3}", props2.getProperty("test3"));

    }
}
