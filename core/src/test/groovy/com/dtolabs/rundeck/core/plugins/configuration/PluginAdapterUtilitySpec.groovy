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

package com.dtolabs.rundeck.core.plugins.configuration

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.SelectLabels
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import spock.lang.Specification

/**
 * @author greg
 * @since 4/6/17
 */
class PluginAdapterUtilitySpec extends Specification {

    /**
     * test property types
     */
    @Plugin(name = "typeTest1", service = "x")
    static class Configuretest1 {
        @PluginProperty
        String testString;
        @PluginProperty
        @SelectValues(values = ["a", "b", "c"])
        String testSelect1;
        @PluginProperty
        @SelectValues(values = ["a", "b", "c"], freeSelect = true)
        String testSelect2;
        @PluginProperty
        @SelectValues(values = ["a", "b", "c"], multiOption = true)
        String testSelect3;
        @PluginProperty
        @SelectValues(values = ["a", "b", "c"], multiOption = true)
        Set<String> testSelect4;
        @PluginProperty
        @SelectValues(values = ["a", "b", "c"], multiOption = true)
        String[] testSelect5;
        @PluginProperty
        @SelectValues(values = ["a", "b", "c"], multiOption = true)
        List<String> testSelect6;

        @PluginProperty(description = 'String select with labels')
        @SelectValues(values = ["a", "b", "c"])
        @SelectLabels(values = ["A", "B", "C"])
        String testSelect7;

        @PluginProperty(description = 'List String multioption select with labels')
        @SelectValues(values = ["a", "b", "c"])
        @SelectLabels(values = ["A", "B", "C"])
        List<String> testSelect8;

        @PluginProperty(description = 'String multioption select with labels')
        @SelectValues(values = ["a", "b", "c"], multiOption = true)
        @SelectLabels(values = ["A", "B", "C"])
        String testSelect9;

        @PluginProperty(description = 'String free select with labels')
        @SelectValues(values = ["a", "b", "c"], freeSelect = true)
        @SelectLabels(values = ["A", "B", "C"])
        String testSelect10;

        @PluginProperty
        Boolean testbool1;
        @PluginProperty
        boolean testbool2;
        @PluginProperty
        int testint1;
        @PluginProperty
        Integer testint2;
        @PluginProperty
        long testlong1;
        @PluginProperty
        Long testlong2;
    }

    static class mapResolver implements PropertyResolver {
        private Map<String, Object> map;

        mapResolver(Map<String, Object> map) {
            this.map = map;
        }

        @Override
        public Object resolvePropertyValue(String name, PropertyScope scope) {
            return map.get(name);
        }
    }


    def "configure options value string"() {
        given:
        Configuretest1 test = new Configuretest1();
        when:

        HashMap<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("testSelect3", value);
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);

        then:
        test.testSelect3 == value

        where:
        value   | _
        'a'     | _
        'a,b'   | _
        'a,b,c' | _
    }

    def "configure options value set"() {
        given:
        Configuretest1 test = new Configuretest1();
        when:

        HashMap<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("testSelect4", value);
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);

        then:
        test.testSelect4 == (expect as Set)

        where:
        value   | expect
        'a'     | ['a']
        'a,b'   | ['a', 'b']
        'a,b,c' | ['a', 'b', 'c']
        'a,c'   | ['a', 'c']
    }

    def "configure options value array"() {
        given:
        Configuretest1 test = new Configuretest1();
        when:

        HashMap<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("testSelect5", value);
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);

        then:
        test.testSelect5 == expect

        where:
        value   | expect
        'a'     | ['a']
        'a,b'   | ['a', 'b']
        'a,b,c' | ['a', 'b', 'c']
        'a,c'   | ['a', 'c']
    }
    def "configure options value list"() {
        given:
        Configuretest1 test = new Configuretest1();
        when:

        HashMap<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("testSelect6", value);
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);

        then:
        test.testSelect6 == expect

        where:
        value   | expect
        'a'     | ['a']
        'a,b'   | ['a', 'b']
        'a,b,c' | ['a', 'b', 'c']
        'a,c'   | ['a', 'c']
    }

    def "build String select property with value labels"() {
        given:

        when:
        def list = PluginAdapterUtility.buildFieldProperties(Configuretest1)

        then:
        def t7 = list.find { it.name == 'testSelect7' }
        t7 != null
        t7.type == Property.Type.Select
        t7.selectValues == ['a', 'b', 'c']
        t7.selectLabels == [a: 'A', b: 'B', c: 'C']
    }


    def "build List of String select property with value labels"() {
        given:

        when:
        def list = PluginAdapterUtility.buildFieldProperties(Configuretest1)

        then:
        def t7 = list.find { it.name == 'testSelect8' }
        t7 != null
        t7.type == Property.Type.Options
        t7.selectValues == ['a', 'b', 'c']
        t7.selectLabels == [a: 'A', b: 'B', c: 'C']
    }


    def "build multioption String select property with value labels"() {
        given:

        when:
        def list = PluginAdapterUtility.buildFieldProperties(Configuretest1)

        then:
        def t7 = list.find { it.name == 'testSelect9' }
        t7 != null
        t7.type == Property.Type.Options
        t7.selectValues == ['a', 'b', 'c']
        t7.selectLabels == [a: 'A', b: 'B', c: 'C']
    }


    def "build String free select property with value labels"() {
        given:

        when:
        def list = PluginAdapterUtility.buildFieldProperties(Configuretest1)

        then:
        def t7 = list.find { it.name == 'testSelect10' }
        t7 != null
        t7.type == Property.Type.FreeSelect
        t7.selectValues == ['a', 'b', 'c']
        t7.selectLabels == [a: 'A', b: 'B', c: 'C']
    }

    def "configure options invalid"() {
        given:
        Configuretest1 test = new Configuretest1();
        when:

        HashMap<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("testSelect4", value);
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);

        then:
        RuntimeException e = thrown()
        e.message =~ /Some options values were not allowed for property/

        where:
        value   | _
        'a,z'   | _
        'a,z,c' | _
        'qasdf'   | _
    }
}
