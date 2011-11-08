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
* TestJarPluginProviderLoader.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/14/11 8:54 AM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * TestJarPluginProviderLoader is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestJarPluginProviderLoader extends AbstractBaseTest {
    private static final String TEST_SERVICE = "TestService";
    Framework testFramework;
    String testnode;
    private static final String TEST_PROJECT = "TestJarPluginProviderLoader";
    private PluginManagerService service;
    private File testDir = new File("src/test/resources/com/dtolabs/rundeck/core/plugins");
    private File testJar1 = new File("src/test/resources/com/dtolabs/rundeck/core/plugins/test-plugin1.jar");
    private File testJarDNE = new File("src/test/resources/com/dtolabs/rundeck/core/plugins/DNE-plugin.jar");
    private File testCachedir;

    public TestJarPluginProviderLoader(final String name) {
        super(name);
    }

    public void setUp() {
        super.setUp();
        testCachedir = getFrameworkInstance().getLibextCacheDir();
    }

    public void testConstruct() throws Exception {
        try {
            new JarPluginProviderLoader(null,null);
            fail("expected npe");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
        try {
            new JarPluginProviderLoader(testJarDNE,null);
            fail("expected illegal argument");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        try {
            new JarPluginProviderLoader(testDir,null);
            fail("expected illegal argument");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        final File testJar = createTestJar(null, null);
        final JarPluginProviderLoader jarPluginProviderLoader = new JarPluginProviderLoader(testJar, testCachedir);
        assertNotNull(jarPluginProviderLoader);
    }

    public void testValidateJarManifestEmptyManifest() throws Exception {
        //no plugin archive attribute
        try {
            JarPluginProviderLoader.validateJarManifest(new Attributes());
            fail("should not validate");
        } catch (JarPluginProviderLoader.InvalidManifestException e) {
            assertNotNull(e);
            assertEquals("Jar plugin manifest attribute missing: " + JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE,
                e.getMessage());
        }
        //plugin archive attribute was not 'true'
        try {
            final Attributes mainAttributes = new Attributes();
            mainAttributes.putValue(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "false");
            JarPluginProviderLoader.validateJarManifest(mainAttributes);
            fail("should not validate");
        } catch (JarPluginProviderLoader.InvalidManifestException e) {
            assertNotNull(e);
            assertEquals(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE + " was not 'true': false",
                e.getMessage());
        }
        //no plugin version attribute
        try {
            final Attributes mainAttributes = new Attributes();
            mainAttributes.putValue(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
            JarPluginProviderLoader.validateJarManifest(mainAttributes);
            fail("should not validate");
        } catch (JarPluginProviderLoader.InvalidManifestException e) {
            assertNotNull(e);
            assertEquals("Jar plugin manifest attribute missing: " + JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION,
                e.getMessage());
        }

        //invalid version attribute
        try {
            final Attributes mainAttributes = new Attributes();
            mainAttributes.putValue(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
            mainAttributes.putValue(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, "invalid");
            JarPluginProviderLoader.validateJarManifest(mainAttributes);
            fail("should not validate");
        } catch (JarPluginProviderLoader.InvalidManifestException e) {
            assertNotNull(e);
            assertEquals(
                "Unssupported plugin version: " + JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION + ": invalid",
                e.getMessage());
        }
        //no plugin classnames attribute
        try {
            final Attributes mainAttributes = new Attributes();
            mainAttributes.putValue(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
            mainAttributes.putValue(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, "1.0");
            JarPluginProviderLoader.validateJarManifest(mainAttributes);
            fail("should not validate");
        } catch (JarPluginProviderLoader.InvalidManifestException e) {
            assertNotNull(e);
            assertEquals(
                "Jar plugin manifest attribute missing: " + JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES,
                e.getMessage());
        }
        {
            //valid
            final Attributes mainAttributes = new Attributes();
            mainAttributes.putValue(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
            mainAttributes.putValue(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, "1.0");
            mainAttributes.putValue(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES, "something");
            JarPluginProviderLoader.validateJarManifest(mainAttributes);
        }
    }

    public void testIsValidJarPlugin() throws Exception {
        //test jar files
        {
            final File testJar = createTestJar(null, null);
            assertFalse(JarPluginProviderLoader.isValidJarPlugin(testJar));
        }
        {
            final Map<String, String> entries = new HashMap<String, String>();
            entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
            final File testJar = createTestJar(entries, null);
            assertFalse(JarPluginProviderLoader.isValidJarPlugin(testJar));
        }
        {
            final Map<String, String> entries = new HashMap<String, String>();
            entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
            entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, "1.0");
            final File testJar = createTestJar(entries, null);
            assertFalse(JarPluginProviderLoader.isValidJarPlugin(testJar));
        }
        {
            final Map<String, String> entries = new HashMap<String, String>();
            entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
            entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, "1.0");
            entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES, "something");
            final File testJar = createTestJar(entries, null);
            assertTrue(JarPluginProviderLoader.isValidJarPlugin(testJar));
        }

        //invalid with DNE file
        assertFalse(JarPluginProviderLoader.isValidJarPlugin(testJarDNE));

        File test2File = File.createTempFile("testIsValidJarPlugin", ".jar");
        test2File.deleteOnExit();
        //invalid with non-jar file
        assertFalse(JarPluginProviderLoader.isValidJarPlugin(test2File));

    }

    @Plugin (name = "test1", service = "TestService")
    public static class testProvider1 {

    }

    public void testMatchesAnnotation() throws Exception {
        assertTrue(JarPluginProviderLoader.matchesProviderDeclaration(new ProviderIdent("TestService", "test1"),
            testProvider1.class));
        assertFalse(JarPluginProviderLoader.matchesProviderDeclaration(new ProviderIdent("XService", "test1"),
            testProvider1.class));
        assertFalse(JarPluginProviderLoader.matchesProviderDeclaration(new ProviderIdent("TestService", "testX"),
            testProvider1.class));
    }

    @Plugin (name = "test2", service = "TestService")
    public static class testProvider2 extends JarTestType1 {
        public testProvider2() {
        }
    }

    /**
     * blank name
     */
    @Plugin (name = "", service = "TestService")
    public static class invalidProvider1 extends JarTestType1 {
        public invalidProvider1() {
        }
    }

    /**
     * blank service
     */
    @Plugin (name = "invalid3", service = "")
    public static class invalidProvider2 extends JarTestType1 {
        public invalidProvider2() {
        }
    }

    /**
     * no annotation
     */
    public static class invalidProviderNoAnnotation extends JarTestType1 {
        public invalidProviderNoAnnotation() {
        }
    }


    public void testLoadInvalid() throws Exception {
        testService1 service = new testService1();
        service.name = "TestService";
        service.isvalid = false;
        service.createInstance = new testProvider2();
        final Class[] classes = {testProvider1.class, testProvider2.class};

        final Map<String, String> entries = new HashMap<String, String>();
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, "1.0");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES, classnameString(classes));

        final File testJar11 = createTestJar(entries, null, classes);


        final JarPluginProviderLoader jarPluginProviderLoader = new JarPluginProviderLoader(testJar11, testCachedir);
        //non-existent
        final JarTestType1 testx = jarPluginProviderLoader.load(service, "testX");
        assertNull(testx);
        //valid
        try {
            final JarTestType1 test2 = jarPluginProviderLoader.load(service, "test2");
            fail("should fail");
        } catch (ProviderLoaderException e) {
            assertTrue(e.getCause() instanceof PluginException);
            assertEquals(
                "Class " + testProvider2.class.getName() + " was not a valid plugin class for service: TestService",
                e.getCause().getMessage());

        }
    }


    public void testLoadValid() throws Exception {
        testService1 service = new testService1();
        service.name = "TestService";
        service.isvalid = true;
        service.createInstance = new testProvider2();
        final Class[] classes = {testProvider1.class, testProvider2.class};

        final Map<String, String> entries = new HashMap<String, String>();
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, "1.0");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES, classnameString(classes));

        final File testJar11 = createTestJar(entries, null, classes);


        final JarPluginProviderLoader jarPluginProviderLoader = new JarPluginProviderLoader(testJar11, testCachedir);
        //non-existent
        final JarTestType1 testx = jarPluginProviderLoader.load(service, "testX");
        assertNull(testx);
        //valid
        final JarTestType1 test2 = jarPluginProviderLoader.load(service, "test2");
        assertNotNull(test2);
        assertTrue(test2 instanceof testProvider2);
    }

    public void testLoadClass() throws Exception {
        testService1 service = new testService1() {
            @Override
            public boolean isValidProviderClass(Class clazz) {
                return JarTestType1.class.isAssignableFrom(clazz);
            }
        };
        service.name = "TestService";
        service.isvalid = true;
        service.createInstance = new testProvider2();
        final Class[] classes = {testProvider1.class, testProvider2.class};

        final Map<String, String> entries = new HashMap<String, String>();
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, "1.0");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES, classnameString(classes));

        final File testJar11 = createTestJar(entries, null, classes);


        final JarPluginProviderLoader jarPluginProviderLoader = new JarPluginProviderLoader(testJar11, testCachedir);
        //non-existent
        final JarTestType1 testx = jarPluginProviderLoader.load(service, "testX");
        assertNull(testx);
        //valid
        final JarTestType1 test2 = jarPluginProviderLoader.load(service, "test2");
        assertNotNull(test2);
        assertTrue(test2 instanceof testProvider2);
        assertNull(testx);
        //valid
        try {
            final JarTestType1 test1 = jarPluginProviderLoader.load(service, "test1");
        } catch (ProviderLoaderException e) {
            assertTrue(e.getCause() instanceof PluginException);
            assertEquals(
                "Class " + testProvider1.class.getName() + " was not a valid plugin class for service: TestService",
                e.getCause().getMessage());
        }
    }

    public void testCreateProviderForClass() throws Exception {
        testService1 service1 = new testService1() {
            @Override
            public boolean isValidProviderClass(Class clazz) {
                return JarTestType1.class.isAssignableFrom(clazz);
            }
        };
        service1.name = "TestService";
        service1.createInstance = new testProvider2();

        final JarTestType1 object = JarPluginProviderLoader.createProviderForClass(service1,
            testProvider2.class);
        assertNotNull(object);
        assertTrue(object instanceof testProvider2);

        testService1 service2 = new testService1();
        service2.name = "TestService";
        service2.isvalid = false;
        service2.createInstance = new testProvider2();

        try {
            final JarTestType1 object2 = JarPluginProviderLoader.createProviderForClass(service2,
                testProvider2.class);
            fail("should fail");
        } catch (PluginException e) {
            assertEquals(
                "Class " + testProvider2.class.getName() + " was not a valid plugin class for service: TestService",
                e.getMessage());
        } catch (ProviderCreationException e) {
            fail("unexpected: " + e.getMessage());
        }
    }

    public void testGetPluginMetadata() throws Exception {

        //no annnotation on class
        try {
            JarPluginProviderLoader.getPluginMetadata(invalidProviderNoAnnotation.class);
            fail("should fail");
        } catch (PluginException e) {
            assertEquals("No Plugin annotation was found for the class: " + invalidProviderNoAnnotation.class.getName(),
                e.getMessage());
        }

        //blank name annotation
        try {
            JarPluginProviderLoader.getPluginMetadata(invalidProvider1.class);
            fail("should fail");
        } catch (PluginException e) {
            assertEquals("Plugin annotation 'name' cannot be empty for the class: " + invalidProvider1.class.getName(),
                e.getMessage());
        }
        //blank service annotation
        try {
            JarPluginProviderLoader.getPluginMetadata(invalidProvider2.class);
            fail("should fail");
        } catch (PluginException e) {
            assertEquals(
                "Plugin annotation 'service' cannot be empty for the class: " + invalidProvider2.class.getName(),
                e.getMessage());
        }


    }

    private String classnameString(final Class... classes) {
        return StringArrayUtil.asString(classnames(classes), ",");
    }

    private String[] classnames(final Class... classes) {
        final String[] names = new String[classes.length];
        int i = 0;
        for (final Class aClass : classes) {
            names[i++] = aClass.getName();
        }
        return names;
    }

    public void testJarClassnames() throws Exception {
        final Class[] classes = {testProvider2.class, testProvider1.class};

        final Map<String, String> entries = new HashMap<String, String>();
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, "1.0");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES, classnameString(classes));

        final File testJar11 = createTestJar(entries, null, classes);
        final JarPluginProviderLoader jarPluginProviderLoader = new JarPluginProviderLoader(testJar11, testCachedir);
        final String[] classnames = jarPluginProviderLoader.getClassnames();
        assertTrue(Arrays.equals(classnames(classes), classnames));
    }

    /**
     * Create test jar file with manifest entries
     */
    static File createTestJar(final Map<String, String> entries, final File test) throws IOException {
        final File file = null != test ? test : File.createTempFile("createTestJar", ".jar");
        if (null == test) {
            file.deleteOnExit();
        }

        final Manifest manifest = new Manifest();
        final Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        if (null != entries) {
            for (final String s : entries.keySet()) {
                mainAttributes.putValue(s, entries.get(s));
            }
        }
        final JarOutputStream jarstream = new JarOutputStream(new FileOutputStream(file), manifest);
        jarstream.putNextEntry(new JarEntry("test"));
        jarstream.write("test".getBytes());
        jarstream.closeEntry();
        jarstream.flush();
        jarstream.close();
        return file;
    }

    /**
     * Create test jar file with manifest entries
     */
    static File createTestJar(final Map<String, String> entries, final File test, final Class[] classes) throws
        IOException {
        final File file = null != test ? test : File.createTempFile("createTestJar", ".jar");
        if (null == test) {
//            file.deleteOnExit();
            System.out.println("file: " + file.getAbsolutePath());
        }

        final Manifest manifest = new Manifest();
        final Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        if (null != entries) {
            for (final String s : entries.keySet()) {
                mainAttributes.putValue(s, entries.get(s));
            }
        }
        final JarOutputStream jarstream = new JarOutputStream(new FileOutputStream(file), manifest);
        for (final Class aClass : classes) {
            writeClassEntry(jarstream, aClass);
        }
        jarstream.flush();
        jarstream.close();
        return file;
    }

    private static void writeClassEntry(JarOutputStream jarstream, Class aClass) throws IOException {
        String name = aClass.getName().replaceAll("\\.", "/") + ".class";
        final JarEntry jarEntry = new JarEntry(name);
        jarstream.putNextEntry(jarEntry);
        final InputStream resourceAsStream = aClass.getClassLoader().getResourceAsStream(name);
        byte[] bytes = new byte[2048];
        int v = resourceAsStream.read(bytes);
        while (v != -1) {
            jarstream.write(bytes, 0, v);
            v = resourceAsStream.read(bytes);
        }
        jarstream.closeEntry();
    }

    public void testIsLoaderFor() throws Exception {

    }

    public static class testService1 implements PluggableService<JarTestType1> {
        boolean isvalid;
        JarTestType1 createScriptInstance;
        private String name;

        public boolean isValidProviderClass(Class clazz) {
            return isvalid;
        }

        JarTestType1 createInstance;

        public JarTestType1 createProviderInstance(Class<JarTestType1> clazz, String name) throws PluginException,
            ProviderCreationException {
            return createInstance;
        }

        boolean isScriptPluggable;

        public boolean isScriptPluggable() {
            return isScriptPluggable;
        }

        public JarTestType1 createScriptProviderInstance(ScriptPluginProvider provider) throws PluginException {
            return createScriptInstance;
        }

        public String getName() {
            return name;
        }
    }

}
