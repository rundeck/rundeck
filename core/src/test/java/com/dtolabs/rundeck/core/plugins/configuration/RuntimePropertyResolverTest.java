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
* RuntimePropertyResolverTest.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/4/12 7:00 AM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import com.dtolabs.rundeck.core.common.PropertyRetriever;
import junit.framework.TestCase;

import java.util.*;


/**
 * RuntimePropertyResolverTest is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class RuntimePropertyResolverTest extends TestCase {


    static class mapRetriever implements PropertyRetriever {
        private Map<String, String> map;

        mapRetriever(Map<String, String> map) {
            this.map = map;
        }

        @Override
        public String getProperty(String name) {
            return map.get(name);
        }

        public static mapRetriever forMap(Map<String, String> map) {
            return new mapRetriever(map);
        }

        public static mapRetriever create(String key, String value) {
            HashMap<String, String> map1 = new HashMap<String, String>();
            map1.put(key, value);
            return new mapRetriever(map1);
        }
    }

    static PropertyRetriever nullRetriever = new PropertyRetriever() {
        @Override
        public String getProperty(String name) {
            return null;
        }
    };

    //unspecified
    public void testUnspecifiedScope() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(nullRetriever,
                                                                   nullRetriever,
                                                                   mapRetriever.create("a", "b"));
        try {
            test.resolvePropertyValue("a", PropertyScope.Unspecified);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {

        }
    }

    //null
    public void testNullScope() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(nullRetriever,
                                                                   nullRetriever,
                                                                   mapRetriever.create("a", "b"));
        try {
            test.resolvePropertyValue("a", null);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {

        }
    }

    //resolve properties only at framework scope
    public void testFwkScope() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(mapRetriever.create("x", "a"),
                                                                   mapRetriever.create("x", "b"),
                                                                   mapRetriever.create("x", "c"));

        assertEquals("c", test.resolvePropertyValue("x", PropertyScope.Framework));
    }

    //resolve properties only at framework scope
    public void testFwkScopeNull() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(mapRetriever.create("x", "a"),
                                                                   mapRetriever.create("x", "b"),
                                                                   nullRetriever);

        assertEquals(null, test.resolvePropertyValue("x", PropertyScope.Framework));
    }

    //resolve properties at project scope
    public void testProjectScopeAll() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(mapRetriever.create("x", "a"),
                                                                   mapRetriever.create("x", "b"),
                                                                   mapRetriever.create("x", "c"));

        assertEquals("b", test.resolvePropertyValue("x", PropertyScope.Project));
    }

    //resolve properties at project scope, fallback to framework
    public void testProjectScopeFallback() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(mapRetriever.create("x", "a"),
                                                                   nullRetriever,
                                                                   mapRetriever.create("x", "c"));

        assertEquals("c", test.resolvePropertyValue("x", PropertyScope.Project));
    }

    //resolve properties at project ONLY scope
    public void testProjectOnlyScopeAll() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(mapRetriever.create("x", "a"),
                                                                   mapRetriever.create("x", "b"),
                                                                   mapRetriever.create("x", "c"));

        assertEquals("b", test.resolvePropertyValue("x", PropertyScope.ProjectOnly));
    }

    //resolve properties at project Only scope, fallback to framework
    public void testProjectOnlyScopeFallback() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(mapRetriever.create("x", "a"),
                                                                   nullRetriever,
                                                                   mapRetriever.create("x", "c"));

        assertEquals(null, test.resolvePropertyValue("x", PropertyScope.ProjectOnly));
    }


    //resolve properties at instance scope
    public void testInstanceScopeAll() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(mapRetriever.create("x", "a"),
                                                                   mapRetriever.create("x", "b"),
                                                                   mapRetriever.create("x", "c"));

        assertEquals("a", test.resolvePropertyValue("x", PropertyScope.Instance));
    }

    //resolve properties at instance scope, fallback to project
    public void testInstanceScopeFallbackProject() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(nullRetriever,
                                                                   mapRetriever.create("x", "b"),
                                                                   mapRetriever.create("x", "c"));

        assertEquals("b", test.resolvePropertyValue("x", PropertyScope.Instance));
    }

    //resolve properties at instance scope, fallback to framework
    public void testInstanceScopeFallbackFramework() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(nullRetriever,
                                                                   nullRetriever,
                                                                   mapRetriever.create("x", "c"));

        assertEquals("c", test.resolvePropertyValue("x", PropertyScope.Instance));
    }

    //resolve properties at instance ONLY scope
    public void testInstanceOnlyScopeAll() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(mapRetriever.create("x", "a"),
                                                                   mapRetriever.create("x", "b"),
                                                                   mapRetriever.create("x", "c"));

        assertEquals("a", test.resolvePropertyValue("x", PropertyScope.InstanceOnly));
    }

    //resolve properties at instance ONLY scope
    public void testInstanceOnlyScopeFallback() {
        RuntimePropertyResolver test = new RuntimePropertyResolver(nullRetriever,
                                                                   mapRetriever.create("x", "b"),
                                                                   mapRetriever.create("x", "c"));

        assertEquals(null, test.resolvePropertyValue("x", PropertyScope.InstanceOnly));
    }
}
