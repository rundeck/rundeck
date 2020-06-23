/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.PluginResourceLoader
import com.dtolabs.rundeck.core.plugins.ProviderIdent
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import grails.test.*
import spock.lang.Specification

class PluginServiceTests extends Specification {
    void setup() {
//        super.setUp()
    }

    void tearDown() {
//        super.tearDown()
    }
    class testPlugin{

    }
    class testProvider implements PluggableProviderService<String>{
        String name

        testProvider() {

        }
        testProvider(String name) {
            this.name = name
        }

        @Override
        List<ProviderIdent> listDescribableProviders() {
            return null
        }

        @Override
        List<Description> listDescriptions() {
            return null
        }


        @Override
        List<ProviderIdent> listProviders() {
            return null
        }

        String providerOfType(String name){
            return null
        }

        @Override
        CloseableProvider<String> closeableProviderOfType(final String providerName)
                throws com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
        {
            return null
        }
    }

    class TestRegistry implements PluginRegistry{
        boolean lpbncalled
        boolean cpbnMapCalled
        boolean cpWithFrameworkCalled
        boolean cpWithResolverCalled
        boolean validateWithResolverCalled
        boolean validateWithResolverIgnoredCalled
        boolean validateWithMapCalled
        boolean validateWithFrameworkCalled
        boolean listPluginDescriptorsCalled
        boolean listPluginsCalled
        boolean getPluginConfigurationByNameCalled
        Object plugin
        DescribedPlugin pluginDescriptor
        Map pluginDescriptorMap
        Map pluginListMap
        ValidatedPlugin pluginValidation
        Map extraConfiguration
        Map allConfiguration

        @Override
        <T> ConfiguredPlugin<T> configurePluginByName(String name, PluggableProviderService<T> service,
                                                     Map configuration) {
            cpbnMapCalled = true
            return new ConfiguredPlugin<T>(plugin, extraConfiguration)
        }

        @Override
        def <T> CloseableProvider<T> retainPluginByName(final String name, final PluggableProviderService<T> service) {
            return null
        }

        @Override
        <T> ConfiguredPlugin<T> configurePluginByName(String name, PluggableProviderService<T> service,
                                                      IFramework framework, String project, Map instanceConfiguration) {
            cpWithFrameworkCalled=true
            return new ConfiguredPlugin<T>( plugin,  extraConfiguration)
        }

        @Override
        <T> ConfiguredPlugin<T> configurePluginByName(String name, PluggableProviderService<T> service,
        PropertyResolver resolver, PropertyScope defaultScope) {
            cpWithResolverCalled=true
            return new ConfiguredPlugin<T>( plugin,  extraConfiguration)
        }

        @Override
        def <T> ConfiguredPlugin<T> retainConfigurePluginByName(
                final String name,
                final PluggableProviderService<T> service,
                final PropertyResolver resolver,
                final PropertyScope defaultScope
        )
        {
            return null
        }
        def <T> PluggableProviderService<T> createPluggableService(final Class<T> type) {
            throw new IllegalArgumentException("test not implemented")
        }

        @Override
        def <T> boolean isFrameworkDependentPluginType(final Class<T> type) {
            throw new IllegalArgumentException(" not implemented")
        }

        @Override
        def <T> PluggableProviderService<T> getFrameworkDependentPluggableService(
                final Class<T> type,
                final Framework framework
        ) {
            throw new IllegalArgumentException(" not implemented")
        }

        @Override
        def <T> ConfiguredPlugin<T> configurePluginByName(
                final String name,
                final PluggableProviderService<T> service,
                final IPropertyLookup frameworkLookup,
                final IPropertyLookup projectLookup,
                final Map instanceConfiguration
        )
        {
            return null
        }

        @Override
        ValidatedPlugin validatePluginByName(String name, PluggableProviderService service,
                                               PropertyResolver resolver, PropertyScope defaultScope) {
            validateWithResolverCalled=true
            return pluginValidation
        }

        @Override
        ValidatedPlugin validatePluginByName(String name, PluggableProviderService service, PropertyResolver resolver, PropertyScope defaultScope, PropertyScope ignoredScope) {
            validateWithResolverIgnoredCalled = true
            return pluginValidation
        }

        @Override
        ValidatedPlugin validatePluginByName(String name, PluggableProviderService service, IFramework framework, String project, Map instanceConfiguration) {
            validateWithFrameworkCalled=true
            return pluginValidation
        }

        @Override
        ValidatedPlugin validatePluginByName(String name, PluggableProviderService service, Map instanceConfiguration) {
            validateWithMapCalled=true
            return pluginValidation
        }

        @Override
        Object loadPluginByName(String name, PluggableProviderService service) {
            lpbncalled=true
            return plugin
        }

        boolean lpdbncalled=false
        @Override
        <T> DescribedPlugin<T> loadPluginDescriptorByName(String name, PluggableProviderService<T> service) {
            lpdbncalled=true
            return pluginDescriptor
        }

        @Override
        Map<String, Object> listPlugins(Class groovyPluginType, PluggableProviderService service) {
            listPluginsCalled=true
            return pluginListMap
        }

        @Override
        <T> Map<String, DescribedPlugin<T> > listPluginDescriptors(Class groovyPluginType,
                PluggableProviderService<T> service) {
            listPluginDescriptorsCalled=true
            return pluginDescriptorMap
        }

        @Override
        Map getPluginConfigurationByName(String name, PluggableProviderService service, PropertyResolver resolver, PropertyScope defaultScope) {
            getPluginConfigurationByNameCalled=true
            return allConfiguration
        }

        @Override
        PluginResourceLoader getResourceLoader(final String service, final String provider)
                throws ProviderLoaderException
        {
            return null
        }

        @Override
        PluginMetadata getPluginMetadata(final String service, final String provider) throws ProviderLoaderException {
            return null
        }

        @Override
        void registerPlugin(String type, String name, String beanName) {

        }
    }

    void testGetPluginDNE() {
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry= testReg
        assertFalse(testReg.lpbncalled)
        assertNull(service.getPlugin("blah", new testProvider()))
        assertTrue(testReg.lpbncalled)
    }
    void testGetPluginExists() {
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        def test = "Test Plugin"
        testReg.plugin= test
        service.rundeckPluginRegistry= testReg
        assertFalse(testReg.lpbncalled)
        assertEquals(test, service.getPlugin("blah", new testProvider()))
        assertTrue(testReg.lpbncalled)
    }

    void testGetPluginNoRegistry() {
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = null
        service.rundeckPluginRegistry= testReg
        assertNull(service.getPlugin("blah", new testProvider()))
    }
    void testGetPluginDescriptorNoRegistry(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = null
        service.rundeckPluginRegistry = testReg
        assertNull(service.getPluginDescriptor("blah", new testProvider()))
    }
    void testGetPluginDescriptorDNE(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        assertFalse(testReg.lpdbncalled)
        assertNull(service.getPluginDescriptor("blah", new testProvider()))
        assertTrue(testReg.lpdbncalled)
    }
    void testGetPluginDescriptor(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        def test = new DescribedPlugin(null,null,null)
        testReg.pluginDescriptor= test
        service.rundeckPluginRegistry = testReg
        assertFalse(testReg.lpdbncalled)
        assertEquals(test, service.getPluginDescriptor("blah", new testProvider()))
        assertTrue(testReg.lpdbncalled)
    }
    void testGetPluginConfigurationNoRegistry(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = null
        service.rundeckPluginRegistry = testReg
        assertNull(service.getPluginConfiguration("blah", new testProvider(), null, null))
    }
    void testGetPluginConfiguration(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        testReg.allConfiguration=[test:'abc']
        assertFalse(testReg.getPluginConfigurationByNameCalled)
        assertNotNull(service.getPluginConfiguration("blah", new testProvider(), null, null))
    }
    void testConfigurePluginByNameNoRegistry(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = null
        service.rundeckPluginRegistry = testReg
        assertNull(service.configurePlugin("blah", [:], new testProvider()))

    }
    void testConfigurePluginByNameDNE(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        assertFalse(testReg.cpbnMapCalled)
        assertNull(service.configurePlugin("blah", [:], new testProvider('test')))
        assertTrue(testReg.cpbnMapCalled)
    }
    void testConfigurePluginByNameExists(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = 'Test configure'
        testReg.plugin= test
        testReg.pluginValidation = new ValidatedPlugin(valid: true)
        assertFalse(testReg.cpbnMapCalled)
        assertFalse(testReg.validateWithMapCalled)
        def result = service.configurePlugin("blah", [:], new testProvider('test'))
        assertNotNull(result)
        assertEquals(test, result.instance)
        assertTrue(testReg.cpbnMapCalled)
        assertTrue(testReg.validateWithMapCalled)
    }
    void testConfigurePluginByNameExistsInvalidConfiguration(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = "Test configure"
        testReg.plugin= test
        testReg.pluginValidation = new ValidatedPlugin(valid: false)
        assertFalse(testReg.cpbnMapCalled)
        assertFalse(testReg.validateWithMapCalled)
        assertNull(service.configurePlugin("blah", [:], new testProvider()))
        assertFalse(testReg.cpbnMapCalled)
        assertTrue(testReg.validateWithMapCalled)
    }
    void testConfigurePluginWithFrameworkExists(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = "Test configure"
        testReg.plugin= test
        testReg.pluginValidation=[valid:true]
        assertFalse(testReg.cpWithFrameworkCalled)
        assertFalse(testReg.validateWithFrameworkCalled)
        def result = service.configurePlugin("blah", [:], "project", (Framework) null, new testProvider())
        assertNotNull(result)
        assertEquals(test, result.instance)
        assertTrue(testReg.cpWithFrameworkCalled)
        assertTrue(testReg.validateWithFrameworkCalled)
    }
    void testConfigurePluginWithFrameworkExistsInvalidConfiguration(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = "Test configure"
        testReg.plugin= test
        testReg.pluginValidation=[valid:false]
        assertFalse(testReg.cpWithFrameworkCalled)
        assertFalse(testReg.validateWithFrameworkCalled)
        assertNull(service.configurePlugin("blah",[:], "project",(Framework)null,new testProvider()))
        assertFalse(testReg.cpWithFrameworkCalled)
        assertTrue(testReg.validateWithFrameworkCalled)
    }
    void testConfigurePluginWithResolverExists(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = 'Test configure'
        testReg.plugin= test
        testReg.pluginValidation= new ValidatedPlugin()
        testReg.pluginValidation.valid=true
        assertTrue(testReg.pluginValidation.valid)
        assertFalse(testReg.cpWithResolverCalled)
        assertFalse(testReg.validateWithResolverCalled)
        def result = service.configurePlugin("blah", new testProvider('test'), null, null)
        assertNotNull(result)
        assertEquals(test, result.instance)
        assertTrue(testReg.cpWithResolverCalled)
        assertTrue(testReg.validateWithResolverCalled)
    }
    void testConfigurePluginWithResolverExistsInvalidConfiguration(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = "Test configure"
        testReg.plugin= test
        testReg.pluginValidation= [valid: false]
        assertFalse(testReg.cpWithResolverCalled)
        assertFalse(testReg.validateWithResolverCalled)
        assertNull(service.configurePlugin("blah",new testProvider(),null,null))
        assertFalse(testReg.cpWithResolverCalled)
        assertTrue(testReg.validateWithResolverCalled)
    }
    void testValidatePlugin(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = new ValidatedPlugin(valid: true)
        testReg.pluginValidation= test
        assertFalse(testReg.validateWithResolverCalled)
        assertEquals(test, service.validatePlugin("blah",new testProvider(),null,null))
        assertTrue(testReg.validateWithResolverCalled)
    }
    void testListPlugins(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = ['a': new DescribedPlugin(null,null,'a'), 'b': new DescribedPlugin(null,null,'b')]
        testReg.pluginDescriptorMap= test
        assertFalse(testReg.listPluginDescriptorsCalled)
        def result = service.listPlugins(String,new testProvider("test service"))
        assertTrue(testReg.listPluginDescriptorsCalled)
        assertEquals('a', result['a'].name)
        assertEquals('b', result['b'].name)
    }
    void testListPluginsCullName(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = ['a': new DescribedPlugin(null,null, 'alphaTestService',new File("alphaTestService.groovy")),
                'b': new DescribedPlugin(null,null, 'bTestService',new File("bTestService.groovy"))]
        testReg.pluginDescriptorMap= test
        assertFalse(testReg.listPluginDescriptorsCalled)
        def result = service.listPlugins(String,new testProvider("TestService"))
        assertTrue(testReg.listPluginDescriptorsCalled)
        assertEquals('alpha', result['a'].name)
        assertEquals('b', result['b'].name)

    }
}
