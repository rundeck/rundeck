/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
import com.dtolabs.rundeck.plugins.descriptions.DynamicSelectValues
import com.dtolabs.rundeck.plugins.descriptions.EmbeddedTypeProperty
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.descriptions.SelectLabels
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import spock.lang.Specification
import spock.lang.Unroll

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
        @RenderingOption(key = "a", value = "b")
        @RenderingOptions([
                @RenderingOption(key = "x", value = "y")
        ])
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
        @RenderingOption(key = "c", value = "d")
        @RenderingOptions([
                @RenderingOption(key = "z", value = "w")
        ])
        Set<String> testSelect4;
        @PluginProperty
        @SelectValues(values = ["a", "b", "c"], multiOption = true)
        @RenderingOption(key = "e", value = "f")
        @RenderingOptions([
                @RenderingOption(key = "t", value = "u")
        ])
        String[] testSelect5;
        @PluginProperty
        @SelectValues(values = ["a", "b", "c"], multiOption = true)
        @RenderingOption(key = "g", value = "h")
        @RenderingOptions([
                @RenderingOption(key = "r", value = "s")
        ])
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
        @SelectValues(values = [], dynamicValues = true)
        List<String> testSelect11;

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
        @PluginProperty
        @RenderingOption(key = "i", value = "j")
        @RenderingOptions([
                @RenderingOption(key = "p", value = "q")
        ])
        Map testMap;
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

    @Unroll
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
        value                             | expect
        'a'                               | ['a']
        'a,b'                             | ['a', 'b']
        'a,b,c'                           | ['a', 'b', 'c']
        'a,c'                             | ['a', 'c']
        ['a', 'c']                        | ['a', 'c']
        ['a', 'c'].toSet()                | ['a', 'c']
        ['a', 'c'].toArray(new String[2]) | ['a', 'c']
    }

    @Unroll
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
        value                             | expect
        'a'                               | ['a']
        'a,b'                             | ['a', 'b']
        'a,b,c'                           | ['a', 'b', 'c']
        'a,c'                             | ['a', 'c']
        ['a', 'c']                        | ['a', 'c']
        ['a', 'c'].toSet()                | ['a', 'c']
        ['a', 'c'].toArray(new String[2]) | ['a', 'c']
    }

    @Unroll
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
        value                             | expect
        'a'                               | ['a']
        'a,b'                             | ['a', 'b']
        'a,b,c'                           | ['a', 'b', 'c']
        'a,c'                             | ['a', 'c']
        ['a', 'c']                        | ['a', 'c']
        ['a', 'c'].toSet()                | ['a', 'c']
        ['a', 'c'].toArray(new String[2]) | ['a', 'c']
    }
    @Unroll
    def "configure options value invalid"() {
        given:
        Configuretest1 test = new Configuretest1();
        when:

        HashMap<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("testSelect6", value);
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);

        then:
        RuntimeException e = thrown()
        e.message.contains 'Some options values were not allowed for property '

        where:
        value                             | expect
        'x'                               | ['a']
        'a,x'                             | ['a', 'b']
        ['a', 'x']                        | ['a', 'c']
        ['a', 'x'].toSet()                | ['a', 'c']
        ['a', 'x'].toArray(new String[2]) | ['a', 'c']
    }

    @Unroll
    def "configure options value dynamic"() {
        given:
        Configuretest1 test = new Configuretest1();
        when:

        HashMap<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("testSelect11", value);
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);

        then:
        test.testSelect11 == expect

        where:
        value                             | expect
        'a'                               | ['a']
        'a,b'                             | ['a', 'b']
        'a,b,c'                           | ['a', 'b', 'c']
        'a,c'                             | ['a', 'c']
        ['a', 'c']                        | ['a', 'c']
        ['a', 'c'].toSet()                | ['a', 'c']
        ['a', 'c'].toArray(new String[2]) | ['a', 'c']
    }

    def "configure map value Map"() {
        given:
        Configuretest1 test = new Configuretest1();
        when:

        HashMap<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("testMap", value);
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);

        then:
        test.testMap == expect

        where:
        value           | expect
        [a: 'b']        | [a: 'b']
        [a: 1]          | [a: 1]
        [a: ['b', 'c']] | [a: ['b', 'c']]
        [a: [b: 'c']]   | [a: [b: 'c']]
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


    def "build Map property "() {
        given:

        when:
        def list = PluginAdapterUtility.buildFieldProperties(Configuretest1)

        then:
        def t7 = list.find { it.name == 'testMap' }
        t7 != null
        t7.type == Property.Type.Map

    }

    def "rendering options allowed for all types"() {

        given:
        when:
        def list = PluginAdapterUtility.buildFieldProperties(Configuretest1)
        then:

        list.find { it.name == 'testString' }?.renderingOptions.a == 'b'
        list.find { it.name == 'testString' }?.renderingOptions.x == 'y'
        list.find { it.name == 'testString' }?.renderingOptions.displayType?.toString() == 'SINGLE_LINE'
        list.find { it.name == 'testSelect4' }?.renderingOptions == [c: 'd', z: 'w']
        list.find { it.name == 'testSelect5' }?.renderingOptions == [e: 'f', t: 'u']
        list.find { it.name == 'testSelect6' }?.renderingOptions == [g: 'h', r: 's']
        list.find { it.name == 'testMap' }?.renderingOptions == [i: 'j', p: 'q']

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

    static class TestValidator1 implements PropertyValidator {
        @Override
        boolean isValid(final String value) throws ValidationException {
            return false
        }
    }

    static class TestGen1 implements ValuesGenerator {

    }
    /**
     * test property types
     */
    @Plugin(name = "typeTest1", service = "x")
    static class Configuretest2 {
        @PluginProperty(validatorClass = TestValidator1)
        String testValidator1;

        @PluginProperty(validatorClassName = 'com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtilitySpec$TestValidator1')
        String testValidator2;

        @PluginProperty
        @SelectValues(values = ['a', 'b'])
        @DynamicSelectValues(generatorClass = TestGen1)
        String testString2;
        @PluginProperty
        @SelectValues(values = ['a', 'b'])
        @DynamicSelectValues(generatorClassName = 'com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtilitySpec$TestGen1')
        String testString3;
    }

    def "values generator class annotation "() {
        given:

        when:
        def list = PluginAdapterUtility.buildFieldProperties(Configuretest2)

        then:
        def field1 = list.find { it.name == 'testString2' }
        field1 != null
        field1.type == Property.Type.Select
        field1.valuesGenerator != null
        field1.valuesGenerator instanceof TestGen1
        def field2 = list.find { it.name == 'testString3' }
        field2 != null
        field2.type == Property.Type.Select
        field2.valuesGenerator != null
        field2.valuesGenerator instanceof TestGen1

    }
    def "validator class annotation "() {
        given:

        when:
        def list = PluginAdapterUtility.buildFieldProperties(Configuretest2)

        then:
        def field1 = list.find { it.name == 'testValidator1' }
        field1 != null
        field1.type == Property.Type.String
        field1.validator != null
        field1.validator instanceof TestValidator1
        def field2 = list.find { it.name == 'testValidator2' }
        field2 != null
        field2.type == Property.Type.String
        field2.validator != null
        field2.validator instanceof TestValidator1

    }

    static class MyClass {
        @PluginProperty
        String aString;
    }
    /**
     * test property types
     */
    @Plugin(name = "typeTest3", service = "x")
    static class Configuretest3 {

        @PluginProperty
        @EmbeddedTypeProperty
        MyClass someField;

        @PluginProperty
        @EmbeddedTypeProperty(type = MyClass)
        List<MyClass> someList;

        @PluginProperty
        @EmbeddedTypeProperty(type = MyClass)
        Set<MyClass> someSet;
    }

    def "embedded type annotation "() {
        given:

        when:
        def list = PluginAdapterUtility.buildFieldProperties(Configuretest3)

        then:
        def field1 = list.find { it.name == 'someField' }
        field1 != null
        field1.type == Property.Type.Embedded
        field1.embeddedType != null
        field1.embeddedType == MyClass
        def field2 = list.find { it.name == 'someList' }
        field2 != null
        field2.type == Property.Type.Options
        field2.embeddedType != null
        field2.embeddedType == MyClass
        def field3 = list.find { it.name == 'someSet' }
        field3 != null
        field3.type == Property.Type.Options
        field3.embeddedType != null
        field3.embeddedType == MyClass

    }

    def "configure single embedded type"() {
        given:
        def obj = new Configuretest3()

        when:
        PluginAdapterUtility.configureObjectFieldsWithProperties(obj, inputConfig)

        then:
        obj.someField != null
        obj.someField.aString == 'the string'

        where:
        inputConfig                          || _
        [someField: [aString: 'the string']] || _

    }

    def "configure list embedded type"() {
        given:
        def obj = new Configuretest3()

        when:
        PluginAdapterUtility.configureObjectFieldsWithProperties(obj, inputConfig)

        then:
        obj.someList != null
        obj.someList.size() == 2
        obj.someList.every { it instanceof MyClass }
        obj.someList*.aString == ['a string', 'b string']

        where:
        inputConfig || _
        [someList: [
            [aString: 'a string'],
            [aString: 'b string'],
        ]]          || _

    }

    def "configure set embedded type"() {
        given:
        def obj = new Configuretest3()

        when:
        PluginAdapterUtility.configureObjectFieldsWithProperties(obj, inputConfig)

        then:
        obj.someSet != null
        obj.someSet.size() == 2
        obj.someSet.every { it instanceof MyClass }
        obj.someSet*.aString.sort() == ['a string', 'b string']

        where:
        inputConfig || _
        [someSet: [
            [aString: 'a string'],
            [aString: 'b string'],
        ]]          || _

    }
}
