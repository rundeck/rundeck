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
* DescriptionBuilderTest.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/3/12 2:25 PM
* 
*/
package com.dtolabs.rundeck.plugins.util;

import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.Property;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * DescriptionBuilderTest is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class DescriptionBuilderTest extends TestCase {
    public void testNameRequired() {
        try {
            DescriptionBuilder.builder().build();
            fail("name was not set");
        } catch (IllegalStateException e) {
            assertEquals("name is not set", e.getMessage());
        }
    }

    public void testBasic() {
        Description build = DescriptionBuilder.builder()
            .name("test1")
            .description("desc1")
            .title("title1")
            .build();

        assertDescriptionBasic(build, "test1", "title1", "desc1");
    }

    public void testPropertiesNull() {

        Description build = DescriptionBuilder.builder()
            .name("test1")
            .description("desc1")
            .title("title1")
            .build();
        assertProperties(build, 0, null);
    }

    private void assertProperties(Description build, final int size, final List<String> names) {
        assertNotNull(build.getProperties());
        assertEquals(size, build.getProperties().size());
        HashMap<String, Property> props = new HashMap<String, Property>();
        for (final Property property : build.getProperties()) {
            props.put(property.getName(), property);
        }
        if (null != names) {
            for (final String name : names) {
                assertTrue(props.containsKey(name));
            }
        }
    }

    public void testPropertyBuilderFactoryNoType() {

        DescriptionBuilder builder = DescriptionBuilder.builder()
            .name("test1")
            .description("desc1")
            .title("title1");
        PropertyBuilder ptest1 = builder.property("ptest1");
        assertNotNull(ptest1);
        try {
            ptest1.build();
            fail("expected exception");
        } catch (IllegalStateException e) {
            assertEquals("type is required", e.getMessage());
        }
    }

    public void testPropertyBuilderFactoryTypes() {

        Description build = DescriptionBuilder.builder()
            .name("test1")
            .description("desc1")
            .title("title1")
            .booleanProperty("ptest1", "false", false, "title1", "desc1")
            .integerProperty("ptest2", "false", false, "title1", "desc1")
            .stringProperty("ptest3", "false", false, "title1", "desc1")
            .selectProperty("ptest4", "false", false, "title1", "desc1", Arrays.asList("a", "b", "c"))
            .freeSelectProperty("ptest5", "false", false, "title1", "desc1", Arrays.asList("d", "e", "f"))
            .build();
        assertProperties(build, 5, Arrays.asList("ptest1", "ptest2", "ptest3", "ptest4", "ptest5"));
    }

    public void testPropertyWithPropertyBuilder() {

        DescriptionBuilder builder = DescriptionBuilder.builder();
        Description build = builder
            .name("test1")
            .description("desc1")
            .title("title1")
            .property(
                builder.property("ptest1").type(Property.Type.String)
            )
            .build();
        assertProperties(build, 1, Arrays.asList("ptest1"));
    }

    public void testPropertyWithProperty() {

        DescriptionBuilder builder = DescriptionBuilder.builder();
        Description build = builder
            .name("test1")
            .description("desc1")
            .title("title1")
            .property(
                builder.property("ptest1").type(Property.Type.String)
                    .build()
            )
            .build();
        assertProperties(build, 1, Arrays.asList("ptest1"));
    }

    public void testPropertyReplace() {

        DescriptionBuilder builder = DescriptionBuilder.builder();
        builder
            .name("test1")
            .description("desc1")
            .title("title1")
            .property(
                builder
                    .property("ptest1")
                    .type(Property.Type.String)
                    .title("ptitle1")
                    .description("pdesc1")
                    .build()
            );

        Description build1 = builder.build();
        assertProperties(build1, 1, Arrays.asList("ptest1"));
        Property p1 = build1.getProperties().get(0);
        assertEquals("ptest1", p1.getName());
        assertEquals("ptitle1", p1.getTitle());
        assertEquals("pdesc1", p1.getDescription());
        assertEquals(null, p1.getDefaultValue());
        assertEquals(Property.Type.String, p1.getType());

        //replace ptest1 with another type
        builder.property(
            builder
                .property("ptest1")//get the property builder for existing property
                .type(Property.Type.Boolean) //set new type
                .description("pdesc2") //set new desc
                .defaultValue("pdef2") //set new default
        );
        Description build = builder.build();
        assertProperties(build, 1, Arrays.asList("ptest1"));
        Property p = build.getProperties().get(0);
        assertEquals("ptest1", p.getName());
        assertEquals("ptitle1", p.getTitle());
        assertEquals("pdesc2", p.getDescription());
        assertEquals("pdef2", p.getDefaultValue());
        assertEquals(Property.Type.Boolean, p.getType());
    }

    private void assertDescriptionBasic(final Description build,
                                        final String name,
                                        final String title, final String description) {
        assertNotNull(build);
        assertEquals(name, build.getName());
        assertEquals(title, build.getTitle());
        assertEquals(description, build.getDescription());
    }
}
