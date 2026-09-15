/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dtolabs.rundeck.core.plugins;

import java.io.File;
import java.io.FileWriter;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PluginValidatorTest {

    private File groovyFile;

    @Before
    public void setup() throws Exception {
        groovyFile = File.createTempFile("plugin", ".groovy");
        try (FileWriter writer = new FileWriter(groovyFile)) {
            writer.write("println 'not a real plugin'");
        }
    }

    @After
    public void teardown() {
        if (groovyFile != null) {
            groovyFile.delete();
        }
    }

    @Test
    public void groovyPluginFilesAreNotAccepted() {
        Assert.assertFalse(PluginValidator.validate(groovyFile));
    }

    @Test
    public void unknownFileExtensionsAreNotAccepted() throws Exception {
        File unknownFile = File.createTempFile("plugin", ".txt");
        try (FileWriter writer = new FileWriter(unknownFile)) {
            writer.write("not a plugin");
        }
        try {
            Assert.assertFalse(PluginValidator.validate(unknownFile));
        } finally {
            unknownFile.delete();
        }
    }
}
