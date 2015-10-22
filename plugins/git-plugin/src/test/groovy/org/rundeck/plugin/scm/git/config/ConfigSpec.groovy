package org.rundeck.plugin.scm.git.config

import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import spock.lang.Specification

/**
 * Created by greg on 10/13/15.
 */
class ConfigSpec extends Specification {
    static class Bogart extends Config {
        @PluginProperty(title = 'abcdef')
        String dummy
    }

    static class SubBogart extends Bogart {
        @PluginProperty(title = 'wangle')
        int crummy
    }

    def "list properties"() {
        when:
        List<Property> properties = Config.listProperties(Bogart)
        then:
        properties.size() == 1

        def dummyProp = properties.find { it.name == 'dummy' }
        dummyProp != null
        dummyProp.title == 'abcdef'
    }

    def "list properties subtype"() {
        when:
        List<Property> properties = Config.listProperties(SubBogart)
        then:
        properties.size() == 2

        def dummyProp = properties.find { it.name == 'dummy' }
        dummyProp != null
        dummyProp.title == 'abcdef'
        dummyProp.type == Property.Type.String

        def crummyProp = properties.find { it.name == 'crummy' }
        crummyProp != null
        crummyProp.title == 'wangle'
        crummyProp.type == Property.Type.Integer
    }

    def "configure"(Map input, Map other) {
        given:
        def bogart = new Bogart()
        when:
        Config.configure(bogart, input)
        then:
        bogart.dummy == input.dummy
        bogart.rawInput == input
        bogart.otherInput == other

        where:
        input                          | other
        [dummy: 'steak']               | [:]
        [milky: 'raw']                 | [milky: 'raw']
        [dummy: 'tweak', milky: 'raw'] | [milky: 'raw']
        [:]                            | [:]
    }

    def "configure subtype"(Map input, Map other, int crummyInt) {
        given:
        def bogart = new SubBogart()
        when:
        Config.configure(bogart, input)
        then:
        bogart.dummy == input.dummy
        bogart.crummy == crummyInt
        bogart.rawInput == input
        bogart.otherInput == other

        where:
        input                           | other          | crummyInt
        [dummy: 'steak']                | [:]            | 0
        [milky: 'raw']                  | [milky: 'raw'] | 0
        [dummy: 'tweak', milky: 'raw']  | [milky: 'raw'] | 0
        [:]                             | [:]            | 0
        [crummy: '1']                   | [:]            | 1
        [dummy: 'bilge', crummy: '-84'] | [:]            | -84
    }

    def "create"(Map input, Map other) {
        when:
        def bogart = Config.create(Bogart, input)

        then:
        bogart.dummy == input.dummy
        bogart.rawInput == input
        bogart.otherInput == other

        where:
        input                          | other
        [dummy: 'steak']               | [:]
        [milky: 'raw']                 | [milky: 'raw']
        [dummy: 'tweak', milky: 'raw'] | [milky: 'raw']
        [:]                            | [:]
    }

    def "create subtype"(Map input, Map other, int crummyInt) {
        when:
        def bogart = Config.create(SubBogart, input)

        then:
        bogart.dummy == input.dummy
        bogart.crummy == crummyInt
        bogart.rawInput == input
        bogart.otherInput == other

        where:
        input                           | other          | crummyInt
        [dummy: 'steak']                | [:]            | 0
        [milky: 'raw']                  | [milky: 'raw'] | 0
        [dummy: 'tweak', milky: 'raw']  | [milky: 'raw'] | 0
        [:]                             | [:]            | 0
        [crummy: '1']                   | [:]            | 1
        [dummy: 'bilge', crummy: '-84'] | [:]            | -84
    }

    def "substituteDefaultValue"(){
        given:
        List<Property> properties = Config.listProperties(Bogart)
        when:
        List<Property> replaced = Config.substituteDefaultValue(properties, 'dummy', 'monkey')

        then:
        properties.size() == 1
        replaced.size() == 1

        def dummyProp = properties.find { it.name == 'dummy' }
        dummyProp != null
        dummyProp.title == 'abcdef'
        dummyProp.defaultValue == null


        def newProp = replaced.find { it.name == 'dummy' }
        newProp != null
        newProp.title == 'abcdef'
        newProp.defaultValue == 'monkey'



    }
}
