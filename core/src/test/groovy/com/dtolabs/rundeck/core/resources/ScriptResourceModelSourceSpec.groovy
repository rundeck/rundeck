package com.dtolabs.rundeck.core.resources

import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import spock.lang.Specification

class ScriptResourceModelSourceSpec extends Specification{

    def "file not found"(){
        given:
        def framework = AbstractBaseTest.createTestFramework()
        def source = new ScriptResourceModelSource(framework)

        def properties = new Properties()
        properties.put("project","test")
        properties.put("file","some/path")
        properties.put("format","resourcexml")
        source.configure(properties)
        when:
        def result = source.getNodes()

        then:
        ResourceModelSourceException e = thrown()
        e.message.contains("file does not exist or is not a file")
    }
}
