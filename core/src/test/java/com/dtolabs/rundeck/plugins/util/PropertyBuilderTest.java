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
* PropertyBuilderTest.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/3/12 2:26 PM
* 
*/
package com.dtolabs.rundeck.plugins.util;

import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;


/**
 * PropertyBuilderTest is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PropertyBuilderTest extends TestCase {

    PropertyValidator testValidator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testValidator = new PropertyValidator() {
            @Override
            public boolean isValid(String value) throws ValidationException {
                return false;
            }
        };
    }


    public void testTypeRequired() {
        try {
            PropertyBuilder.builder().build();
            fail("name should be required");
        } catch (IllegalStateException e) {
            assertEquals("type is required", e.getMessage());
        }
    }

    public void testNameRequired() {
        try {
            PropertyBuilder.builder().type(Property.Type.String).build();
            fail("name should be required");
        } catch (IllegalStateException e) {
            assertEquals("name is required", e.getMessage());
        }
    }

    public void testBasic() {
        Property test = PropertyBuilder.builder()
            .type(Property.Type.String)
            .name("test")
            .build();

        assertProperty(test, "test", Property.Type.String, null, null, null, null, null, false, false);
    }

    private void assertProperty(Property test,
                                final String name,
                                final Property.Type type,
                                final String description,
                                final String defaultValue,
                                final String title,
                                final List<String> select,
                                final PropertyValidator validator,
                                final boolean validatorNotNull,
                                final boolean required) {

        assertNotNull(test);
        assertEquals(name, test.getName());
        assertEquals(type, test.getType());
        assertEquals(description, test.getDescription());
        assertEquals(defaultValue, test.getDefaultValue());
        assertEquals(title, test.getTitle());
        assertEquals(select, test.getSelectValues());
        if (null != validator) {
            assertEquals(validator, test.getValidator());
        } else {
            assertEquals(validatorNotNull, null != test.getValidator());
        }
        assertEquals(required, test.isRequired());
    }

    public void testStringProperties() {
        Property test = PropertyBuilder.builder()
            .type(Property.Type.String)
            .name("test")
            .description("desc test")
            .defaultValue("def test")
            .title("title test")
            .values("a", "b", "c")
            .required(true)
            .validator(testValidator)
            .build();

        assertProperty(test,
                       "test",
                       Property.Type.String,
                       "desc test",
                       "def test",
                       "title test",
                       null,//not used for String
                       testValidator,
                       true,
                       true);
    }

    public void testSelectValues() {
        Property test = PropertyBuilder.builder()
            .type(Property.Type.Select)
            .name("test")
            .description("desc test")
            .defaultValue("def test")
            .title("title test")
            .values("a", "b", "c")
            .required(true)
            .build();

        assertProperty(test,
                       "test",
                       Property.Type.Select,
                       "desc test",
                       "def test",
                       "title test",
                       Arrays.asList("a", "b", "c"),
                       null,
                       true, //select validator
                       true);
    }

    public void testFreeSelectValues() {
        Property test = PropertyBuilder.builder()
            .type(Property.Type.FreeSelect)
            .name("test")
            .description("desc test")
            .defaultValue("def test")
            .title("title test")
            .values("a", "b", "c")
            .required(true)
            .build();

        assertProperty(test,
                       "test",
                       Property.Type.FreeSelect,
                       "desc test",
                       "def test",
                       "title test",
                       Arrays.asList("a", "b", "c"),
                       null,
                       false,
                       true);
    }

    public void testFactoryTypesBool() {
        Property test = PropertyBuilder.builder()
            .booleanType("test")
            .name("test")
            .build();

        assertProperty(test,
                       "test",
                       Property.Type.Boolean,
                       null,
                       null,
                       null,
                       null,
                       null,
                       true, //boolean validator
                       false);
    }

    public void testFactoryTypesString() {
        Property test = PropertyBuilder.builder()
            .string("test")
            .name("test")
            .build();

        assertProperty(test,
                       "test",
                       Property.Type.String,
                       null,
                       null,
                       null,
                       null,
                       null,
                       false,
                       false);
    }

    public void testFactoryTypesSelect() {
        Property test = PropertyBuilder.builder()
            .select("test")
            .name("test")
            .build();

        assertProperty(test,
                       "test",
                       Property.Type.Select,
                       null,
                       null,
                       null,
                       null,
                       null,
                       true,
                       false);
    }

    public void testFactoryTypesFreeSelect() {
        Property test = PropertyBuilder.builder()
            .freeSelect("test")
            .name("test")
            .build();

        assertProperty(test,
                       "test",
                       Property.Type.FreeSelect,
                       null,
                       null,
                       null,
                       null,
                       null,
                       false,
                       false);
    }

    public void testFactoryTypesInt() {
        Property test = PropertyBuilder.builder()
            .integer("test")
            .name("test")
            .build();

        assertProperty(test,
                       "test",
                       Property.Type.Integer,
                       null,
                       null,
                       null,
                       null,
                       null,
                       true, //int validator
                       false);
    }

    public void testFactoryTypesLong() {
        Property test = PropertyBuilder.builder()
            .longType("test")
            .name("test")
            .build();

        assertProperty(test,
                       "test",
                       Property.Type.Long,
                       null,
                       null,
                       null,
                       null,
                       null,
                       true, //int validator
                       false);
    }

    public void testFactoryProperty() {
        Property test1 = PropertyBuilder.builder()
            .type(Property.Type.String)
            .name("test")
            .description("desc test")
            .defaultValue("def test")
            .title("title test")
            .values("a", "b", "c")
            .required(true)
            .validator(testValidator)
            .build();
        PropertyBuilder builder = PropertyBuilder.builder(test1);

        assertProperty(builder.build(),
                       test1.getName(),
                       test1.getType(),
                       test1.getDescription(),
                       test1.getDefaultValue(),
                       test1.getTitle(),
                       test1.getSelectValues(),
                       test1.getValidator(),
                       true,
                       test1.isRequired());
    }
    public void testFactoryPropertySelect() {
        Property test1 = PropertyBuilder.builder()
            .type(Property.Type.Select)
            .name("test")
            .description("desc test")
            .defaultValue("def test")
            .title("title test")
            .values("a", "b", "c")
            .required(true)
            .build();
        PropertyBuilder builder = PropertyBuilder.builder(test1);

        assertProperty(builder.build(),
                       test1.getName(),
                       test1.getType(),
                       test1.getDescription(),
                       test1.getDefaultValue(),
                       test1.getTitle(),
                       test1.getSelectValues(),
                       null,
                       true,
                       test1.isRequired());
    }
}
