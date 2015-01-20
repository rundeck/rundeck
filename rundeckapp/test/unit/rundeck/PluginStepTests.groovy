package rundeck

import grails.test.*
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

@TestMixin(GrailsUnitTestMixin)
@Mock([PluginStep])
class PluginStepTests  {

    void testClone() {
        PluginStep t = new PluginStep(type: 'blah',configuration: [elf:'hello'],nodeStep: true, keepgoingOnSuccess: true)
        PluginStep j1 = t.createClone()
        assertEquals('blah', j1.type)
        assertEquals([elf:'hello'], j1.configuration)
        assertEquals(true, j1.nodeStep)
        assertEquals(true, !!j1.keepgoingOnSuccess)
        assertNull(j1.errorHandler)
    }
}
