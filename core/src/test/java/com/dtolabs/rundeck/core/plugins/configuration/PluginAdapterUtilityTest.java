/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* PluginAdapterUtilityTest.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/29/12 3:22 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.plugins.descriptions.*;
import junit.framework.TestCase;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.DisplayType;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;


/**
 * PluginAdapterUtilityTest is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PluginAdapterUtilityTest extends TestCase {
    public static final String TEST_VALIDATOR_CLASSNAME = TestValidator.class.toString();
    //test reflection of property values

    /**
     * invalid: doesn't have @Plugin annotation
     */
    static class invalidTest1  {
        @PluginProperty
        private String testString;
        @PluginProperty(title = "test2", description = "testdesc2")
        private String testString2;
        @PluginProperty(name = "test3", title = "test3", description = "testdesc3")
        private String testString3;
    }

    public void testInvalid() {
        invalidTest1 test = new invalidTest1();
        Description description = null;
        try {
            description = PluginAdapterUtility.buildDescription(test, DescriptionBuilder.builder());
            fail("no plugin annotation and no buildDescription method should cause exception");
        } catch (IllegalStateException e) {

        }

    }

    /**
     * basic annotation test
     */
    @Plugin(name = "basicTest1", service = "x")
    static class basicTest1  {

    }

    public void testBasic1() {
        basicTest1 test = new basicTest1();
        Description description = PluginAdapterUtility.buildDescription(test, DescriptionBuilder.builder());
        assertNotNull(description);
        assertNotNull(description.getName());
        assertEquals("basicTest1", description.getName());
        assertEquals("basicTest1", description.getTitle());
        assertEquals("", description.getDescription());
        assertNotNull(description.getProperties());
        assertEquals(0, description.getProperties().size());
    }

    /**
     * basic annotation test2, has PluginDescription
     */
    @Plugin(name = "basicTest2", service = "x")
    @PluginDescription(title = "basictest2 title", description = "basicTest Description")
    static class basicTest2  {

    }

    public void testBasic2() {
        basicTest2 test = new basicTest2();
        Description description = PluginAdapterUtility.buildDescription(test, DescriptionBuilder.builder());;
        assertNotNull(description);
        assertNotNull(description.getName());
        assertEquals("basicTest2", description.getName());
        assertEquals("basictest2 title", description.getTitle());
        assertEquals("basicTest Description", description.getDescription());

        assertNotNull(description.getProperties());
        assertEquals(0, description.getProperties().size());
    }

    /**
     * string property test
     */
    @Plugin(name = "stringTest1", service = "x")
    static class stringTest1  {
        @PluginProperty
        private String testString;
        @PluginProperty(title = "test2", description = "testdesc2")
        private String testString2;
        @PluginProperty(name = "test3", title = "test3title", description = "testdesc3")
        private String testString3;
        @PluginProperty(defaultValue = "elf1")
        private String testString4;
        @PluginProperty(required = true)
        private String testString5;
    }

    public void testPropertiesStringDefault() {
        stringTest1 test1 = new stringTest1();
        Description description = PluginAdapterUtility.buildDescription(test1, DescriptionBuilder.builder());;
        assertNotNull(description);
        assertEquals("stringTest1", description.getName());
        assertNotNull(description.getProperties());
        assertEquals(5, description.getProperties().size());
        HashMap<String, Property> map = mapOfProperties(description);
        assertTrue(map.containsKey("testString"));
        assertTrue(map.containsKey("testString2"));
        assertTrue(map.containsKey("test3"));
        assertTrue(map.containsKey("testString4"));
        assertTrue(map.containsKey("testString5"));
    }


    static class TestValidator implements PropertyValidator{
        @Override
        public boolean isValid(final String value) throws ValidationException {
            return "monkey".equalsIgnoreCase(value);
        }
    }
    /**
     * validator class test
     */
    @Plugin(name = "validatorTest", service = "x")
    static class validatorTest  {
        @PluginProperty(validatorClass = TestValidator.class)
        private String testString;
    }
    /**
     * validator class name test
     */
    @Plugin(name = "validatorTest2", service = "x")
    static class validatorTest2  {
        @PluginProperty(validatorClassName = "com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtilityTest$TestValidator")
        private String testString;
    }
    public void testValidatorClass() {
        validatorTest test1 = new validatorTest();
        Description description = PluginAdapterUtility.buildDescription(test1, DescriptionBuilder.builder());;
        assertNotNull(description);
        assertEquals("validatorTest", description.getName());
        assertNotNull(description.getProperties());
        assertEquals(1, description.getProperties().size());
        HashMap<String, Property> map = mapOfProperties(description);
        assertTrue(map.containsKey("testString"));
        Property property = map.get("testString");
        assertNotNull(property.getValidator());
        assertTrue(property.getValidator() instanceof TestValidator);

    }
    public void testValidatorClassname() {
        validatorTest2 test1 = new validatorTest2();
        Description description = PluginAdapterUtility.buildDescription(test1, DescriptionBuilder.builder());;
        System.out.println(TestValidator.class.getName());
        assertNotNull(description);
        assertEquals("validatorTest2", description.getName());
        assertNotNull(description.getProperties());
        assertEquals(1, description.getProperties().size());
        HashMap<String, Property> map = mapOfProperties(description);
        assertTrue(map.containsKey("testString"));
        Property property = map.get("testString");
        assertNotNull(property.getValidator());
        assertTrue(property.getValidator() instanceof TestValidator);

    }

    /**
     * validator class name test
     */
    @Plugin(name = "validatorTest2", service = "x")
    static class renderingOptionTest  {
        @PluginProperty
        @RenderingOptions(
                {
                        @RenderingOption(key = "abc", value = "monkey"),
                        @RenderingOption(key = "xyz", value = "donkey")
                }
        )
        private String testMultiple;

        @PluginProperty
        @RenderingOption(key = "def", value = "cookie")
        private String testSingle;
    }
    /**
     * rendering option values
     */
    public void testRenderingOptionSingle() {
        renderingOptionTest test1 = new renderingOptionTest();
        Description description = PluginAdapterUtility.buildDescription(test1, DescriptionBuilder.builder());;
        HashMap<String, Property> map = mapOfProperties(description);

        Property p1 = map.get("testSingle");
        assertEquals(Property.Type.String, p1.getType());
        assertEquals("cookie", p1.getRenderingOptions().get("def"));
    }
    /**
     * rendering option values
     */
    public void testRenderingOptionMultiple() {
        renderingOptionTest test1 = new renderingOptionTest();
        Description description = PluginAdapterUtility.buildDescription(test1, DescriptionBuilder.builder());;
        HashMap<String, Property> map = mapOfProperties(description);

        Property p1 = map.get("testMultiple");
        assertEquals(Property.Type.String, p1.getType());
        assertEquals("monkey", p1.getRenderingOptions().get("abc"));
        assertEquals("donkey", p1.getRenderingOptions().get("xyz"));
    }
    /**
     * Default annotation values
     */
    public void testPropertiesStringAnnotationsDefault() {
        stringTest1 test1 = new stringTest1();
        Description description = PluginAdapterUtility.buildDescription(test1, DescriptionBuilder.builder());;
        HashMap<String, Property> map = mapOfProperties(description);

        Property p1 = map.get("testString");
        assertEquals(Property.Type.String, p1.getType());
        assertEquals(null, p1.getDefaultValue());
        assertEquals("", p1.getDescription());
        assertEquals("testString", p1.getTitle());
        assertEquals(null, p1.getSelectValues());
        assertEquals(null, p1.getValidator());
        assertEquals(false, p1.isRequired());
        assertEquals(DisplayType.SINGLE_LINE, p1.getRenderingOptions().get(StringRenderingConstants.DISPLAY_TYPE_KEY));
    }

    /**
     * Default annotation values
     */
    public void testPropertiesStringAnnotationsTitle() {
        stringTest1 test1 = new stringTest1();
        Description description = PluginAdapterUtility.buildDescription(test1, DescriptionBuilder.builder());
        HashMap<String, Property> map = mapOfProperties(description);

        Property p1 = map.get("testString2");
        assertEquals(Property.Type.String, p1.getType());
        assertEquals(null, p1.getDefaultValue());
        assertEquals("testdesc2", p1.getDescription());
        assertEquals("test2", p1.getTitle());
        assertEquals(null, p1.getSelectValues());
        assertEquals(null, p1.getValidator());
        assertEquals(false, p1.isRequired());
    }

    /**
     * Default annotation values
     */
    public void testPropertiesStringAnnotationsName() {
        stringTest1 test1 = new stringTest1();
        Description description = PluginAdapterUtility.buildDescription(test1, DescriptionBuilder.builder());
        HashMap<String, Property> map = mapOfProperties(description);

        Property p1 = map.get("test3");
        assertEquals(Property.Type.String, p1.getType());
        assertEquals(null, p1.getDefaultValue());
        assertEquals("testdesc3", p1.getDescription());
        assertEquals("test3title", p1.getTitle());
        assertEquals(null, p1.getSelectValues());
        assertEquals(null, p1.getValidator());
        assertEquals(false, p1.isRequired());
    }

    /**
     * Default annotation values
     */
    public void testPropertiesStringAnnotationsDefaultValue() {
        stringTest1 test1 = new stringTest1();
        Description description = PluginAdapterUtility.buildDescription(test1, DescriptionBuilder.builder());
        HashMap<String, Property> map = mapOfProperties(description);

        Property p1 = map.get("testString4");
        assertEquals("elf1", p1.getDefaultValue());
    }

    /**
     * Default annotation values
     */
    public void testPropertiesStringAnnotationsRequired() {
        stringTest1 test1 = new stringTest1();
        Description description = PluginAdapterUtility.buildDescription(test1, DescriptionBuilder.builder());
        HashMap<String, Property> map = mapOfProperties(description);

        Property p1 = map.get("testString5");
        assertEquals(true, p1.isRequired());
    }

    private HashMap<String, Property> mapOfProperties(Description description) {
        List<Property> properties = description.getProperties();
        HashMap<String, Property> map = new HashMap<String, Property>();
        for (final Property property : properties) {
            assertFalse(map.containsKey(property.getName()));
            map.put(property.getName(), property);
        }
        return map;
    }


    /**
     * test property types
     */
    @Plugin(name = "typeTest1", service = "x")
    static class typeTest1  {
        @PluginProperty
        private String testString;
        @PluginProperty
        private Boolean testbool1;
        @PluginProperty
        private boolean testbool2;
        @PluginProperty
        private int testint1;
        @PluginProperty
        private Integer testint2;
        @PluginProperty
        private long testlong1;
        @PluginProperty
        private Long testlong2;
        @TextArea
        @PluginProperty(name = "textArea", title = "textAreaTitle", description = "textAreaDescription")
        private String textArea;
        @Password
        @PluginProperty
        private String password;
    }

    public void testFieldTypesString() throws Exception {
        typeTest1 test = new typeTest1();
        Description desc = PluginAdapterUtility.buildDescription(test, DescriptionBuilder.builder());
        assertNotNull(desc);
        HashMap<String, Property> map = mapOfProperties(desc);
        assertPropertyType(map, "testString", Property.Type.String);
        assertPropertyType(map, "testbool1", Property.Type.Boolean);
        assertPropertyType(map, "testbool2", Property.Type.Boolean);
        assertPropertyType(map, "testint1", Property.Type.Integer);
        assertPropertyType(map, "testint2", Property.Type.Integer);
        assertPropertyType(map, "testlong1", Property.Type.Long);
        assertPropertyType(map, "testlong2", Property.Type.Long);
    }

    private void assertPropertyType(HashMap<String, Property> map, String fieldName, Property.Type fieldType) {
        Property prop1 = map.get(fieldName);
        assertNotNull(prop1);
        assertEquals(fieldType, prop1.getType());
    }

    public void testTextArea() {
        typeTest1 test = new typeTest1();
        Description desc = PluginAdapterUtility.buildDescription(test, DescriptionBuilder.builder());
        assertNotNull(desc);
        HashMap<String, Property> map = mapOfProperties(desc);

        Property p1 = map.get("textArea");
        assertEquals(Property.Type.String, p1.getType());
        assertEquals(null, p1.getDefaultValue());
        assertEquals("textAreaDescription", p1.getDescription());
        assertEquals("textAreaTitle", p1.getTitle());
        assertEquals(DisplayType.MULTI_LINE, p1.getRenderingOptions().get(StringRenderingConstants.DISPLAY_TYPE_KEY));
    }

    public void testPassword() {
        typeTest1 test = new typeTest1();
        Description desc = PluginAdapterUtility.buildDescription(test, DescriptionBuilder.builder());
        assertNotNull(desc);
        HashMap<String, Property> map = mapOfProperties(desc);

        Property p1 = map.get("password");
        assertEquals(DisplayType.PASSWORD, p1.getRenderingOptions().get(StringRenderingConstants.DISPLAY_TYPE_KEY));
    }

    public void testSetTextArea() {
        typeTest1 test = new typeTest1();
        String value = "some value";
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("textArea", value);
        PropertyResolver resolver = new mapResolver(values);
        PluginAdapterUtility.configureProperties(resolver, test);
        assertEquals(value, test.textArea);
    }

    /**
     * test property types
     */
    @Plugin(name = "typeSelect1", service = "x")
    static class typeSelect1  {
        @PluginProperty
        @SelectValues(values = {"a", "b"})
        private String testSelect1;
        @PluginProperty
        @SelectValues(values = {"a", "b", "c"}, freeSelect = true)
        private String testSelect2;
    }

    public void testSelectFields() {
        typeSelect1 test = new typeSelect1();
        Description desc = PluginAdapterUtility.buildDescription(test, DescriptionBuilder.builder());
        assertNotNull(desc);
        HashMap<String, Property> map = mapOfProperties(desc);
        assertPropertyType(map, "testSelect1", Property.Type.Select);
        Property select1 = map.get("testSelect1");
        assertNotNull(select1.getSelectValues());
        assertEquals(2, select1.getSelectValues().size());
        assertEquals(Arrays.asList("a", "b"), select1.getSelectValues());

        assertPropertyType(map, "testSelect2", Property.Type.FreeSelect);
        Property select2 = map.get("testSelect2");
        assertNotNull(select2.getSelectValues());
        assertEquals(3, select2.getSelectValues().size());
        assertEquals(Arrays.asList("a", "b", "c"), select2.getSelectValues());
    }

    /**
     * test property types
     */
    @Plugin(name = "typeTest1", service = "x")
    static class configuretest1  {
        @PluginProperty
        String testString;
        @PluginProperty
        @SelectValues(values = {"a", "b", "c"})
        String testSelect1;
        @PluginProperty
        @SelectValues(values = {"a", "b", "c"}, freeSelect = true)
        String testSelect2;
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
        private Map<String,Object> map;

        mapResolver(Map<String, Object> map) {
            this.map = map;
        }

        @Override
        public Object resolvePropertyValue(String name, PropertyScope scope) {
            return map.get(name);
        }
    }

    public void testConfigurePropertiesEmpty() throws Exception {
        configuretest1 test = new configuretest1();
        assertNull(test.testString);
        assertNull(test.testSelect1);
        assertNull(test.testSelect2);
        assertNull(test.testbool1);
        assertFalse(test.testbool2);
        assertEquals(0, test.testint1);
        assertNull(test.testint2);
        assertEquals(0, test.testlong1);
        assertNull(test.testlong2);
        PluginAdapterUtility.configureProperties(new mapResolver(new HashMap<String, Object>()),test);
        assertNull(test.testString);
        assertNull(test.testSelect1);
        assertNull(test.testSelect2);
        assertNull(test.testbool1);
        assertFalse(test.testbool2);
        assertEquals(0, test.testint1);
        assertNull(test.testint2);
        assertEquals(0, test.testlong1);
        assertNull(test.testlong2);
    }

    public void testConfigurePropertiesString() throws Exception {
        configuretest1 test = new configuretest1();
        HashMap<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("testString", "monkey");
        configuration.put("testSelect1", "a");
        configuration.put("testSelect2", "b");
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);
        assertEquals("monkey", test.testString);
        assertEquals("a", test.testSelect1);
        assertEquals("b", test.testSelect2);
    }
    public void testConfigurePropertiesSelectAllowed() throws Exception {
        configuretest1 test = new configuretest1();
        String[] values = {"a", "b", "c"};
        for (final String value : values) {
            HashMap<String, Object> configuration = new HashMap<String, Object>();
            configuration.put("testSelect1", value);
            configuration.put("testSelect2", value);
            PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);
            assertEquals(value, test.testSelect1);
            assertEquals(value, test.testSelect2);
        }
    }
    public void testConfigurePropertiesSelectInvalidSelect() throws Exception{
        configuretest1 test = new configuretest1();
        //invalid for select field
        String[] invalid = {"monkey", "spaghetti", "wheel"};
        for (final String value : invalid) {
            HashMap<String, Object> config = new HashMap<String, Object>();
            config.put("testSelect1", value);
            try {
                PluginAdapterUtility.configureProperties(new mapResolver(config), test);
                fail("Should not allow value: " + value);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            assertNull(test.testSelect1);
        }
    }

    public void testConfigurePropertiesSelectInvalidFreeSelect() throws Exception {
        configuretest1 test = new configuretest1();
        //invalid for select field
        String[] invalid = {"monkey", "spaghetti", "wheel"};
        for (final String value : invalid) {
            HashMap<String, Object> config = new HashMap<String, Object>();
            config.put("testSelect2", value);
            PluginAdapterUtility.configureProperties(new mapResolver(config),test);
            assertEquals(value,test.testSelect2);
        }
    }

    public void testConfigurePropertiesBool() throws Exception {
        configuretest1 test = new configuretest1();
        HashMap<String, Object> configuration = new HashMap<String, Object>();
        //true value string
        configuration.put("testbool1", "true");
        configuration.put("testbool2", "true");
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);
        assertTrue(test.testbool1);
        assertTrue(test.testbool2);

        //false value string
        configuration.put("testbool1", "false");
        configuration.put("testbool2", "false");
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);
        assertFalse(test.testbool1);
        assertFalse(test.testbool2);

        test.testbool1=true;
        test.testbool2=true;

        //other value string
        configuration.put("testbool1", "monkey");
        configuration.put("testbool2", "elf");
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);
        assertFalse(test.testbool1);
        assertFalse(test.testbool2);
    }

    public void testConfigurePropertiesInt() throws Exception {
        configuretest1 test = new configuretest1();
        HashMap<String, Object> configuration = new HashMap<String, Object>();
        //int values
        configuration.put("testint1", "1");
        configuration.put("testint2", "2");
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);
        assertEquals(1, test.testint1);
        assertEquals(2, (int) test.testint2);

        //invalid values
        configuration.put("testint1", "asdf");
        configuration.put("testint2", "fdjkfd");
        try {
            PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);
            fail("shouldn't succeed");
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        assertEquals(1, test.testint1);
        assertEquals(2, (int) test.testint2);
    }

    public void testConfigurePropertiesLong() throws Exception {
        configuretest1 test = new configuretest1();
        HashMap<String, Object> configuration = new HashMap<String, Object>();
        //int values
        configuration.put("testlong1", "1");
        configuration.put("testlong2", "2");
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);
        assertEquals(1,test.testlong1);
        assertEquals(2,(long)test.testlong2);

        //invalid values
        configuration.put("testlong1", "asdf");
        configuration.put("testlong2", "fdjkfd");
        try {
            PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);
            fail("shouldn't succeed");
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        assertEquals(1,test.testlong1);
        assertEquals(2,(long)test.testlong2);
    }


    /**
     * Inheritance test for properties
     */
    @Plugin(name = "typeTest2", service = "x")
    @PluginDescription(title = "Inherited Test",description = "my test description")
    static class configuretest2 extends configuretest1 {
        @PluginProperty String testString2;
        @PluginProperty @SelectValues(values = {"a", "b", "c"}) String testSelect3;
        @PluginProperty Boolean testbool3;
        @PluginProperty int testint3;
        @PluginProperty long testlong3;
    }
    /**
     * Inherited properties
     */
    public void testPropertiesAnnotationsInherited() {
        configuretest2 test1 = new configuretest2();
        Description description = PluginAdapterUtility.buildDescription(test1, DescriptionBuilder.builder());
        assertEquals("typeTest2",description.getName());
        assertEquals("my test description",description.getDescription());
        assertEquals("Inherited Test",description.getTitle());

        HashMap<String, Property> map = mapOfProperties(description);

        assertNotNull(map.get("testString"));
        assertNotNull(map.get("testString2"));
        assertNotNull(map.get("testSelect1"));
        assertNotNull(map.get("testSelect2"));
        assertNotNull(map.get("testSelect3"));
        assertNotNull(map.get("testbool1"));
        assertNotNull(map.get("testbool2"));
        assertNotNull(map.get("testbool3"));
        assertNotNull(map.get("testint1"));
        assertNotNull(map.get("testint2"));
        assertNotNull(map.get("testint3"));
        assertNotNull(map.get("testlong1"));
        assertNotNull(map.get("testlong2"));
        assertNotNull(map.get("testlong3"));
    }
    public void testConfigurePropertiesInherited() throws Exception {
        configuretest2 test = new configuretest2();
        HashMap<String, Object> configuration = new HashMap<String, Object>();
        //int values
        configuration.put("testlong1", "1");
        configuration.put("testlong2", "2");
        configuration.put("testlong3", "3");
        PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);
        assertEquals(1, test.testlong1);
        assertEquals(2,(long)test.testlong2);
        assertEquals(3,test.testlong3);
    }
    public void testConfigurePropertiesInheritedInvalid() throws Exception {
        configuretest2 test = new configuretest2();
        HashMap<String, Object> configuration = new HashMap<String, Object>();
        //int values
        //invalid values
        configuration.put("testlong1", "asdf");
        try {
            PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);
            fail("shouldn't succeed");
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        //invalid values
        configuration.put("testlong1", "1");
        configuration.put("testlong3", "asdf");
        try {
            PluginAdapterUtility.configureProperties(new mapResolver(configuration), test);
            fail("shouldn't succeed");
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
}
