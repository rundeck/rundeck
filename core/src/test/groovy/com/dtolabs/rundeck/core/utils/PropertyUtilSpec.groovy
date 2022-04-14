package com.dtolabs.rundeck.core.utils

import groovy.transform.CompileStatic
import org.apache.tools.ant.Project
import spock.lang.Specification

class PropertyUtilSpec extends Specification {
    private File propertyFile
    private Properties properties

    def setup() {
        propertyFile = File.createTempFile("prop1", "properties")
        propertyFile.deleteOnExit()
        properties = new Properties()
        properties.put("foo", "shizzle")
        properties.put("bar", "madizzle")
        properties.put("baz", "luzizle")
        properties.put("zab", '${foo}-${bar}')
        properties.store(new FileOutputStream(propertyFile), "test properties")
    }

    def teardown() {
        propertyFile.delete()
    }

    def "invalid expansion should not modify result or throw exception"() {
        given:
            Properties props = new Properties()
            props.put("a", "bcd")
            props.put("b", "")
        when:
            def result = PropertyUtil.expand(input, props)
        then:
            result == expected
        where:
            input                           | expected
            '${a}'                          | 'bcd'
            '${a'                           | '${a'
            'asdf${a'                       | 'asdf${a'
            '${aasdf'                       | '${aasdf'
            '$'                             | '$'
            'asd$'                          | 'asd$'
            '${'                            | '${'
            'asdf${'                        | 'asdf${'
            '${b}'                          | ''
            '${ziggy}'                      | '${ziggy}'
            '${{a}'                         | '${{a}'
            '$$a}'                          | '$$a}'
            '$}a}'                          | '$}a}'
            'test $something other $$ blah' | 'test $something other $$ blah'

    }

    def testExpand() {
        given:
            final Properties expanded = PropertyUtil.expand(properties)
            Project project = new Project()
            project.setProperty("foozle", "foo")
            project.setProperty("boozle", "boo")
            project.setProperty("droozle", '${boozle}${foozle}')
            Properties props = new Properties()
            props.put("a", "bcd")
            props.put("test1", '${a}')
            props.put("test2", 'test-${b}')
            props.put("test3", '${test3}')

        expect:
            properties.size() == expanded.size()
            expanded.get("zab") == "shizzle-madizzle"

        when:
            String str = PropertyUtil.expand('${foo}', properties)
        then:
            str == "shizzle"


        when:
            str = PropertyUtil.expand('${foozle}-${boozle}-${droozle}', project)
        then:
            str == "foo-boo-boofoo"

            //try to expand properties with embedded reference that does not resolve
        when:
            final Properties props2 = PropertyUtil.expand(props)
        then:
            props2.getProperty("a") == "bcd"
            props2.getProperty("test1") == "bcd"
            props2.getProperty("test2") == 'test-${b}'
            props2.getProperty("test3") == '${test3}'

    }
}
