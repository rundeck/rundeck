/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package rundeck

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(NodeFilter)
class NodeFilterTests {

    void setUp() {
        // Setup logic here
    }

    void tearDown() {
        // Tear down logic here
    }

    void testValidation() {
        def filter = new NodeFilter(name:'abc', user:new User())
        filter.validate()
        assertTrue(filter.errors.allErrors.collect{it.toString()}.join("; "),!filter.hasErrors())
    }
    void testInvalidName() {
        def filter = new NodeFilter(name:'a name with < bad char', user:new User())
        filter.validate()
        assertTrue(filter.errors.allErrors.collect{it.toString()}.join("; "),filter.hasErrors())
        assertTrue(filter.errors.allErrors.collect{it.toString()}.join("; "),filter.errors.hasFieldErrors('name'))
    }
}
