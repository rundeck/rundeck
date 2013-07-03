package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.ProviderIdent
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.server.plugins.PluginRegistry
import grails.test.*

class PluginServiceTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
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
        boolean isValidProviderClass(Class clazz) {
            return false
        }

        @Override
        boolean isScriptPluggable() {
            return false
        }

        @Override
        List<ProviderIdent> listProviders() {
            return null
        }

        String createProviderInstance(Class clazz, String name) {

            return null;
        }

        String createScriptProviderInstance(ScriptPluginProvider name) {

            return null;
        }

        String providerOfType(String name){
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
        Object plugin
        Map pluginDescriptor
        Map pluginDescriptorMap
        Map pluginListMap
        Map pluginValidation
        Map extraConfiguration

        @Override
        Map configurePluginByName(String name, PluggableProviderService service, Map configuration) {
            cpbnMapCalled = true
            return [instance:plugin,configuration: extraConfiguration]
        }

        @Override
        Map configurePluginByName(String name, PluggableProviderService service, Framework framework, String project, Map instanceConfiguration) {
            cpWithFrameworkCalled=true
            return [instance: plugin, configuration: extraConfiguration]
        }

        @Override
        Map configurePluginByName(String name, PluggableProviderService service, PropertyResolver resolver, PropertyScope defaultScope) {
            cpWithResolverCalled=true
            return [instance: plugin, configuration: extraConfiguration]
        }

        @Override
        Map validatePluginByName(String name, PluggableProviderService service, PropertyResolver resolver, PropertyScope defaultScope) {
            validateWithResolverCalled=true
            return pluginValidation
        }

        @Override
        Map validatePluginByName(String name, PluggableProviderService service, PropertyResolver resolver, PropertyScope defaultScope, PropertyScope ignoredScope) {
            validateWithResolverIgnoredCalled = true
            return pluginValidation
        }

        @Override
        Map validatePluginByName(String name, PluggableProviderService service, Framework framework, String project, Map instanceConfiguration) {
            validateWithFrameworkCalled=true
            return pluginValidation
        }

        @Override
        Map validatePluginByName(String name, PluggableProviderService service, Map instanceConfiguration) {
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
        Map loadPluginDescriptorByName(String name, PluggableProviderService service) {
            lpdbncalled=true
            return pluginDescriptor
        }

        @Override
        Map<String, Object> listPlugins(Class groovyPluginType, PluggableProviderService service) {
            listPluginsCalled=true
            return pluginListMap
        }

        @Override
        Map<String, Object> listPluginDescriptors(Class groovyPluginType, PluggableProviderService service) {
            listPluginDescriptorsCalled=true
            return pluginDescriptorMap
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
        def test = [test: "description"]
        testReg.pluginDescriptor= test
        service.rundeckPluginRegistry = testReg
        assertFalse(testReg.lpdbncalled)
        assertEquals(test, service.getPluginDescriptor("blah", new testProvider()))
        assertTrue(testReg.lpdbncalled)
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
        assertNull(service.configurePlugin("blah", [:], new testProvider()))
        assertTrue(testReg.cpbnMapCalled)
    }
    void testConfigurePluginByNameExists(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = "Test configure"
        testReg.plugin= test
        testReg.pluginValidation = [valid: true]
        assertFalse(testReg.cpbnMapCalled)
        assertFalse(testReg.validateWithMapCalled)
        def result = service.configurePlugin("blah", [:], new testProvider())
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
        testReg.pluginValidation = [valid: false]
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
        def test = "Test configure"
        testReg.plugin= test
        testReg.pluginValidation= [valid: true]
        assertFalse(testReg.cpWithResolverCalled)
        assertFalse(testReg.validateWithResolverCalled)
        def map = service.configurePlugin("blah", new testProvider(), null, null)
        assertEquals(test, map.instance)
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
        def test = [valid: true, test: true]
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
        def test = ['a': [description: [name:'a',title:'A']], 'b': [description: [name:'b',title:'B']]]
        testReg.pluginDescriptorMap= test
        assertFalse(testReg.listPluginDescriptorsCalled)
        def result = service.listPlugins(String,new testProvider("test service"))
        assertTrue(testReg.listPluginDescriptorsCalled)
        assertEquals([name: 'a', title: 'A'], result['a']['description'])
        assertEquals([name: 'b', title: 'B'], result['b']['description'])
    }
    void testListPluginsCullName(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = ['a': [description: [name:'alphaTestService',title:'A']], 'b': [description: [name:'bTestService',title:'B']]]
        testReg.pluginDescriptorMap= test
        assertFalse(testReg.listPluginDescriptorsCalled)
        def result = service.listPlugins(String,new testProvider("TestService"))
        assertTrue(testReg.listPluginDescriptorsCalled)
        assertEquals([name: 'alpha', title: 'A'], result['a']['description'])
        assertEquals([name: 'b', title: 'B'], result['b']['description'])

    }
}
