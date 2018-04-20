/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.plugins

import com.dtolabs.rundeck.core.utils.FileUtils
import spock.lang.Specification

import java.nio.file.Files

/**
 * @author greg
 * @since 3/9/17
 */
class JarPluginProviderLoaderSpec extends Specification {
    public static final String CURRENT_PLUGIN_VERSION = "1.1";
    private File testDir = new File("src/test/resources/com/dtolabs/rundeck/core/plugins");
    private File testJarDNE = new File("src/test/resources/com/dtolabs/rundeck/core/plugins/DNE-plugin.jar");
    private File testCachedir;
    private File testPluginJarCacheDirectory;


    def setup() {
        testCachedir = Files.createTempDirectory("JarPluginProviderLoaderSpec").toFile()
        testPluginJarCacheDirectory = Files.createTempDirectory("tempRundeckJars-" + UUID.randomUUID().toString()).
                toFile();
        testPluginJarCacheDirectory.deleteOnExit();
        testPluginJarCacheDirectory.mkdirs();
    }

    def cleanup() {

    }

    static class MyTestService extends TestJarPluginProviderLoader.testService1 {
        @Override
        public boolean isValidProviderClass(Class clazz) {
            return JarTestType1.class.isAssignableFrom(clazz);
        }
    }

    def "does not close after expire if retained"() {
        given:
        MyTestService service = new MyTestService()
        service.name = "TestService";
        service.isvalid = true;
        service.createInstance = new TestJarPluginProviderLoader.testProvider2();

        final Class[] classes = [TestJarPluginProviderLoader.testProvider2];

        final Map<String, String> entries = new HashMap<>();
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, CURRENT_PLUGIN_VERSION);
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES, classes*.toString().join(','));
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_LIBS, "lib/fakejar.jar");
        File[] libs = new File[1];
        libs[0] = new File("fakejar.jar");
        final File testJar11 = TestJarPluginProviderLoader.createTestJar(entries, null, classes, libs);

        FileUtils.deleteDir(testPluginJarCacheDirectory);
        testPluginJarCacheDirectory.mkdirs();

        final JarPluginProviderLoader jarPluginProviderLoader = new JarPluginProviderLoader(
                testJar11,
                testPluginJarCacheDirectory,
                testCachedir
        );

        when:
        def closeable = jarPluginProviderLoader.loadCloseable(service, "test2")
        jarPluginProviderLoader.expire()
        then:
        jarPluginProviderLoader.isExpired()
        !jarPluginProviderLoader.isClosed()
    }

    def "does close after expire if unretained"() {
        given:
        MyTestService service = new MyTestService()
        service.name = "TestService";
        service.isvalid = true;
        service.createInstance = new TestJarPluginProviderLoader.testProvider2();

        final Class[] classes = [TestJarPluginProviderLoader.testProvider2];

        final Map<String, String> entries = new HashMap<>();
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, CURRENT_PLUGIN_VERSION);
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES, classes*.toString().join(','));
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_LIBS, "lib/fakejar.jar");
        File[] libs = new File[1];
        libs[0] = new File("fakejar.jar");
        final File testJar11 = TestJarPluginProviderLoader.createTestJar(entries, null, classes, libs);

        FileUtils.deleteDir(testPluginJarCacheDirectory);
        testPluginJarCacheDirectory.mkdirs();

        final JarPluginProviderLoader jarPluginProviderLoader = new JarPluginProviderLoader(
                testJar11,
                testPluginJarCacheDirectory,
                testCachedir
        );

        when:
        def loaded = jarPluginProviderLoader.load(service, "test2")
        jarPluginProviderLoader.expire()
        then:
        jarPluginProviderLoader.isExpired()
        jarPluginProviderLoader.isClosed()
    }

    def "does close after expire if released"() {
        given:
        MyTestService service = new MyTestService()
        service.name = "TestService";
        service.isvalid = true;
        service.createInstance = new TestJarPluginProviderLoader.testProvider2();

        final Class[] classes = [TestJarPluginProviderLoader.testProvider2];

        final Map<String, String> entries = new HashMap<>();
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, CURRENT_PLUGIN_VERSION);
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES, classes*.toString().join(','));
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_LIBS, "lib/fakejar.jar");
        File[] libs = new File[1];
        libs[0] = new File("fakejar.jar");
        final File testJar11 = TestJarPluginProviderLoader.createTestJar(entries, null, classes, libs);

        FileUtils.deleteDir(testPluginJarCacheDirectory);
        testPluginJarCacheDirectory.mkdirs();

        final JarPluginProviderLoader jarPluginProviderLoader = new JarPluginProviderLoader(
                testJar11,
                testPluginJarCacheDirectory,
                testCachedir
        );
        def closeable = jarPluginProviderLoader.loadCloseable(service, "test2")
        def closeable2 = jarPluginProviderLoader.loadCloseable(service, "test2")

        when:
        closeable.close()
        closeable2.close()
        jarPluginProviderLoader.expire()
        then:
        jarPluginProviderLoader.isExpired()
        jarPluginProviderLoader.isClosed()
    }

    def "does close after all released if expired"() {
        given:
        MyTestService service = new MyTestService()
        service.name = "TestService";
        service.isvalid = true;
        service.createInstance = new TestJarPluginProviderLoader.testProvider2();

        final Class[] classes = [TestJarPluginProviderLoader.testProvider2];

        final Map<String, String> entries = new HashMap<>();
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, CURRENT_PLUGIN_VERSION);
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES, classes*.toString().join(','));
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_LIBS, "lib/fakejar.jar");
        File[] libs = new File[1];
        libs[0] = new File("fakejar.jar");
        final File testJar11 = TestJarPluginProviderLoader.createTestJar(entries, null, classes, libs);

        FileUtils.deleteDir(testPluginJarCacheDirectory);
        testPluginJarCacheDirectory.mkdirs();

        final JarPluginProviderLoader jarPluginProviderLoader = new JarPluginProviderLoader(
                testJar11,
                testPluginJarCacheDirectory,
                testCachedir
        );
        def closeable = jarPluginProviderLoader.loadCloseable(service, "test2")
        def closeable2 = jarPluginProviderLoader.loadCloseable(service, "test2")

        when:
        jarPluginProviderLoader.expire()
        closeable.close()
        closeable2.close()
        then:
        jarPluginProviderLoader.isExpired()
        jarPluginProviderLoader.isClosed()
    }

    def "does not close after one released if expired"() {
        given:
        MyTestService service = new MyTestService()
        service.name = "TestService";
        service.isvalid = true;
        service.createInstance = new TestJarPluginProviderLoader.testProvider2();

        final Class[] classes = [TestJarPluginProviderLoader.testProvider2];

        final Map<String, String> entries = new HashMap<>();
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, CURRENT_PLUGIN_VERSION);
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES, classes*.toString().join(','));
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_LIBS, "lib/fakejar.jar");
        File[] libs = new File[1];
        libs[0] = new File("fakejar.jar");
        final File testJar11 = TestJarPluginProviderLoader.createTestJar(entries, null, classes, libs);

        FileUtils.deleteDir(testPluginJarCacheDirectory);
        testPluginJarCacheDirectory.mkdirs();

        final JarPluginProviderLoader jarPluginProviderLoader = new JarPluginProviderLoader(
                testJar11,
                testPluginJarCacheDirectory,
                testCachedir
        );
        def closeable = jarPluginProviderLoader.loadCloseable(service, "test2")
        def closeable2 = jarPluginProviderLoader.loadCloseable(service, "test2")

        when:
        jarPluginProviderLoader.expire()
        closeable2.close()
        then:
        jarPluginProviderLoader.isExpired()
        !jarPluginProviderLoader.isClosed()
    }

    def "does not close after expire if one released"() {
        given:
        MyTestService service = new MyTestService()
        service.name = "TestService";
        service.isvalid = true;
        service.createInstance = new TestJarPluginProviderLoader.testProvider2();

        final Class[] classes = [TestJarPluginProviderLoader.testProvider2];

        final Map<String, String> entries = new HashMap<>();
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_ARCHIVE, "true");
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_VERSION, CURRENT_PLUGIN_VERSION);
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES, classes*.toString().join(','));
        entries.put(JarPluginProviderLoader.RUNDECK_PLUGIN_LIBS, "lib/fakejar.jar");
        File[] libs = new File[1];
        libs[0] = new File("fakejar.jar");
        final File testJar11 = TestJarPluginProviderLoader.createTestJar(entries, null, classes, libs);

        FileUtils.deleteDir(testPluginJarCacheDirectory);
        testPluginJarCacheDirectory.mkdirs();

        final JarPluginProviderLoader jarPluginProviderLoader = new JarPluginProviderLoader(
                testJar11,
                testPluginJarCacheDirectory,
                testCachedir
        );
        def closeable = jarPluginProviderLoader.loadCloseable(service, "test2")
        def closeable2 = jarPluginProviderLoader.loadCloseable(service, "test2")

        when:
        closeable2.close()
        jarPluginProviderLoader.expire()

        then:
        jarPluginProviderLoader.isExpired()
        !jarPluginProviderLoader.isClosed()
    }
}
