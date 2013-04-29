/*
 * Copyright 2013 DTO Solutions, Inc. (http://dtosolutions.com)
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
 * JarPluginScannerTest.java
 * 
 * User: Kim Ho <a href="mailto:kim.ho@salesforce.com">kim.ho@salesforce.com</a>
 * Created: 4/16/13 1:15 PM
 * 
 */
package com.dtolabs.rundeck.core.plugins;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.utils.FileUtils;

public class JarPluginScannerTest {

    protected File cacheDir;
    protected JarPluginScanner scanner;

    @Before
    public void setup() {
        cacheDir = new File(Constants.getBaseTempDirectory() + Constants.FILE_SEP + UUID.randomUUID().toString()
                + "cache");
        FileUtils.deleteDir(cacheDir);
        cacheDir.deleteOnExit();
    }

    @After
    public void teardown() {
        FileUtils.deleteDir(cacheDir);
        if (scanner != null) {
            // This should be the same as cacheDir but just in case something is broken.
            FileUtils.deleteDir(scanner.cachedir);
            FileUtils.deleteDir(scanner.pluginJarCacheDirectory);
        }
    }

    @Test
    public void testIntialization() throws IOException {
        scanner = new JarPluginScanner(null, cacheDir, null, Integer.MAX_VALUE);
        Assert.assertSame("Expected cache directory to be the same as provided", cacheDir, scanner.cachedir);
        Assert.assertNotNull("Expected pluginJarCacheDirectory to be set", scanner.pluginJarCacheDirectory);
        String canonicalTempDirectory = new File(Constants.getBaseTempDirectory()).getCanonicalPath();
        String canonicalPluginJarCacheDirectory = scanner.pluginJarCacheDirectory.getCanonicalPath();
        Assert.assertTrue("Expected pluginJarCacheDirectory to be under base temp directory",
                canonicalPluginJarCacheDirectory.startsWith(canonicalTempDirectory));
    }

    @Test
    public void testCreatesCacheDirectoriesIfNotExisting() {
        scanner = new JarPluginScanner(null, cacheDir, null, Integer.MAX_VALUE);
        Assert.assertTrue("Expected cache dir to be created", cacheDir.exists() && cacheDir.isDirectory());
        Assert.assertEquals("Expected cache dir to be empty", 0, cacheDir.listFiles().length);
        Assert.assertTrue("Expected pluginJarCacheDirectory to be created", scanner.pluginJarCacheDirectory.exists()
                && scanner.pluginJarCacheDirectory.isDirectory());
        Assert.assertEquals("Expected pluginJarCacheDirectory to be empty", 0,
                scanner.pluginJarCacheDirectory.listFiles().length);
    }

    @Test
    public void testDeletesCacheDirectoriesIfExisting() throws IOException {
        File expectedPluginJarCachePath = new File(Constants.getBaseTempDirectory() + Constants.FILE_SEP
                + JarPluginScanner.JAR_SCRATCH_DIRECTORY);
        expectedPluginJarCachePath.mkdirs();
        File cachedJar = File.createTempFile("some", "jar", expectedPluginJarCachePath);
        cachedJar.deleteOnExit();

        cacheDir.mkdirs();
        File dependency = File.createTempFile("some", "jar", cacheDir);
        dependency.deleteOnExit();

        scanner = new JarPluginScanner(null, cacheDir, null, Integer.MAX_VALUE);

        Assert.assertFalse("Expected dependency to be deleted", dependency.exists());
        Assert.assertTrue("Expected cache dir to be created", cacheDir.exists() && cacheDir.isDirectory());
        Assert.assertEquals("Expected cache dir to be empty", 0, cacheDir.listFiles().length);

        Assert.assertFalse("Expected cached jar to be deleted", cachedJar.exists());
        Assert.assertTrue("Expected pluginJarCacheDirectory to be created", scanner.pluginJarCacheDirectory.exists()
                && scanner.pluginJarCacheDirectory.isDirectory());
        Assert.assertEquals("Expected pluginJarCacheDirectory to be empty", 0,
                scanner.pluginJarCacheDirectory.listFiles().length);

    }
}
