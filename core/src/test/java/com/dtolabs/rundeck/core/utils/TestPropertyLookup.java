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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * TestPropertyLookup
 */
public class TestPropertyLookup extends TestCase {

    private final Properties properties1;
    private final File propertyFile;

    public TestPropertyLookup(final String name) throws IOException {
        super(name);
        propertyFile = File.createTempFile("prop1", "properties");
        properties1 = new Properties();
        properties1.put("foo", "shizzle");
        properties1.put("bar", "madizzle");
        properties1.put("baz", "luzizle");
        properties1.store(new FileOutputStream(propertyFile), "test properties");
    }

    public static Test suite() {
        return new TestSuite(TestPropertyLookup.class);
    }


    public void testConstruction() {
        final PropertyLookup lookup = PropertyLookup.create(propertyFile);
        assertNotNull(lookup);
    }

    public void testLookup() {
        final PropertyLookup lookup = PropertyLookup.create(propertyFile);
        assertTrue(lookup.hasProperty("foo"));
        assertEquals(lookup.getProperty("foo"), "shizzle");
        assertEquals(lookup.getProperty("bar"), "madizzle");
        assertEquals(lookup.getProperty("baz"), "luzizle");
    }

    public void testFetchProperties() throws IOException {
        final Properties props = PropertyLookup.fetchProperties(propertyFile);
        assertEquals(props.size(), 3);
    }

    public void testExpand() throws IOException {
        final File pFile = File.createTempFile("myprops", "properties");
        final Properties p = new Properties();
        p.put("foo", "shizzler");
        p.put("bar", "${foo}-madizzler");
        p.store(new FileOutputStream(pFile), "test properties");
        final PropertyLookup lookup = PropertyLookup.create(pFile);
        assertEquals("test precondition not met", lookup.getProperty("foo"), "shizzler");
        PropertyLookup lookup2 = lookup.expand();
        assertEquals(lookup, lookup2);
        assertEquals("shizzler-madizzler", lookup.getProperty("bar"));
    }

    public void testConstructionWithDefaults() throws IOException {
        final File pFile = File.createTempFile("myprops", "properties");
        final Properties p = new Properties();
        p.put("foo", "shizzler");
        p.put("bar", "bizzle");
        p.store(new FileOutputStream(pFile), "test properties");

        final PropertyLookup defaults = PropertyLookup.create(propertyFile);
        final PropertyLookup authoratitive = PropertyLookup.create(pFile, defaults);

        assertEquals("merged properties should be the same size as defaults",
                authoratitive.countProperties(), defaults.getPropertiesMap().size());
        assertEquals("unexpeced foo value: " + authoratitive.getProperty("foo"),
                "shizzler", authoratitive.getProperty("foo"));
    }

    public void testDifference() {
        final PropertyLookup lookup = PropertyLookup.create(propertyFile);
        final Properties others = new Properties();
        others.put("blah", "bablazo");
        final Properties diff = lookup.difference(others);
        assertEquals(1, diff.size());
        assertTrue(diff.containsKey("blah"));
    }
}
