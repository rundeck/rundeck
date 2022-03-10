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

import grails.test.hibernate.HibernateSpec
import org.grails.orm.hibernate.HibernateDatastore
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.springframework.transaction.PlatformTransactionManager
import testhelper.RundeckHibernateSpec

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
class ReportFilterSpec extends RundeckHibernateSpec {

    List<Class> getDomainClasses() { [ReportFilter] }

    void "testValidation"() {
        when:
        def filter = new ReportFilter(name:'name',user:new User(),projFilter: 'project')
        filter.validate()
        then:
        assertFalse(filter.errors.allErrors.collect { it.toString() }.join("; "),filter.hasErrors())
    }

    void "testInvalidName"() {
        when:
        def filter = new ReportFilter(name:'a bad < character',user:new User(), projFilter: 'project')
        filter.validate()
        then:
        assertTrue(filter.errors.allErrors.collect { it.toString() }.join("; "),filter.hasErrors())
        assertTrue(filter.errors.hasFieldErrors('name'))
    }

    void "testMissingProject"() {
        when:
        def filter = new ReportFilter(name:'a name',user:new User(), projFilter: null)
        filter.validate()
        then:
        assertTrue(filter.errors.allErrors.collect { it.toString() }.join("; "),filter.hasErrors())
        assertTrue(filter.errors.hasFieldErrors('projFilter'))
    }
}
