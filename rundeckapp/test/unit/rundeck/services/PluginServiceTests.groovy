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
        boolean listPluginDescriptorsCalled
        Object plugin
        Map pluginDescriptor
        Map pluginDescriptorMap
        Map pluginListMap
        Map pluginValidation

        @Override
        Object configurePluginByName(String name, PluggableProviderService service, Map configuration) {
            cpbnMapCalled = true
            return plugin
        }

        @Override
        Object configurePluginByName(String name, PluggableProviderService service, Framework framework, String project, Map instanceConfiguration) {
            cpWithFrameworkCalled=true
            return plugin
        }

        @Override
        Object configurePluginByName(String name, PluggableProviderService service, PropertyResolver resolver, PropertyScope defaultScope) {
            cpWithResolverCalled=true
            return plugin
        }

        @Override
        Map validatePluginByName(String name, PluggableProviderService service, PropertyResolver resolver, PropertyScope defaultScope) {
            validateWithResolverCalled=true
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
        assertFalse(testReg.cpbnMapCalled)
        assertEquals(test, service.configurePlugin("blah", [:], new testProvider()))
        assertTrue(testReg.cpbnMapCalled)
    }
    void testConfigurePluginWithFrameworkExists(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = "Test configure"
        testReg.plugin= test
        assertFalse(testReg.cpWithFrameworkCalled)
        assertEquals(test, service.configurePlugin("blah",[:], "project",(Framework)null,new testProvider()))
        assertTrue(testReg.cpWithFrameworkCalled)
    }
    void testConfigurePluginWithResolverExists(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = "Test configure"
        testReg.plugin= test
        assertFalse(testReg.cpWithResolverCalled)
        assertEquals(test, service.configurePlugin("blah",new testProvider(),null,null))
        assertTrue(testReg.cpWithResolverCalled)
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
        def test = ["a": [description: [name:"a",title:"A"]], "b": [description: [name:"b",title:"B"]]]
        testReg.pluginDescriptorMap= test
        assertFalse(testReg.listPluginDescriptorsCalled)
        assertEquals(test, service.listPlugins(new testProvider("test service")))
        assertTrue(testReg.listPluginDescriptorsCalled)
    }
    void testListPluginsCullName(){
        mockLogging(PluginService)
        def service = new PluginService()
        def TestRegistry testReg = new TestRegistry()
        service.rundeckPluginRegistry = testReg
        def test = ["a": [description: [name:"alphaTestService",title:"A"]], "b": [description: [name:"bTestService",title:"B"]]]
        testReg.pluginDescriptorMap= test
        assertFalse(testReg.listPluginDescriptorsCalled)
        def result = service.listPlugins(new testProvider("TestService"))
        assertEquals(test, result)
        assertTrue(testReg.listPluginDescriptorsCalled)

        assertEquals("alpha",result['a'].description.name)
        assertEquals("b",result['b'].description.name)
    }
}
