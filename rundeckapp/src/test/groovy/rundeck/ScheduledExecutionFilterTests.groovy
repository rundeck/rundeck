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
@TestFor(ScheduledExecutionFilter)
class ScheduledExecutionFilterTests {

    void setUp() {
        // Setup logic here
    }

    void tearDown() {
        // Tear down logic here
    }

    void testValidation() {
        def filter = new ScheduledExecutionFilter(name: 'name', user: new User())
        filter.validate()
        assertFalse(filter.errors.allErrors.collect { it.toString() }.join("; "), filter.hasErrors())
    }
    void testInvalidName() {
        def filter = new ScheduledExecutionFilter(name: 'a bad < char', user: new User())
        filter.validate()
        assertTrue(filter.errors.allErrors.collect { it.toString() }.join("; "), filter.hasErrors())
    }

    void testScheduledFilterNull(){
        def filter = new ScheduledExecutionFilter(name: 'name',scheduledFilter: null, user: new User())
        def query = filter.createQuery()
        assertNull(query.scheduledFilter)
    }
    void testScheduledFilterEmpty(){
        def filter = new ScheduledExecutionFilter(name: 'name',scheduledFilter: '', user: new User())
        def query = filter.createQuery()
        assertNull(query.scheduledFilter)
    }
    void testScheduledFilterTrue(){
        def filter = new ScheduledExecutionFilter(name: 'name',scheduledFilter: 'true', user: new User())
        def query = filter.createQuery()
        assertTrue(query.scheduledFilter)
    }
    void testScheduledFilterFalse(){
        def filter = new ScheduledExecutionFilter(name: 'name',scheduledFilter: 'false', user: new User())
        def query = filter.createQuery()
        assertFalse(query.scheduledFilter)
        assertFalse(query.scheduledFilter == null)
    }
    void testServerNodeUUIDFilter(){
        def uuid = '7184c972-2292-4a30-82be-93cd333e4dec'
        def filter = new ScheduledExecutionFilter(name: 'name',serverNodeUUIDFilter: uuid, user: new User())
        def query = filter.createQuery()
        assertNotNull(query.serverNodeUUIDFilter)
        assertEquals(uuid, query.serverNodeUUIDFilter)
    }

}
