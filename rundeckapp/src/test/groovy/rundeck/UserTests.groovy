/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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
import grails.testing.gorm.DataTest
import org.springframework.context.support.StaticMessageSource
import spock.lang.Specification

import static org.junit.Assert.assertFalse

class UserTests extends Specification implements DataTest {
    @Override
    Class[] getDomainClassesToMock() {
        [User]
    }

    void "testBasic"() {
        when:
        def user = new User(login: 'login')
        user.validate()
        then:
        assertFalse(user.errors.allErrors.collect { it.toString() }.join("; "),user.hasErrors())
    }
    void "testValidationChars"() {
        when:
        def user = new User(login: 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ  @ 1234567890 .,(-) \\/_')
        user.validate()
        then:
        assertFalse(user.errors.allErrors.collect { it.toString() }.join("; "),user.hasErrors())
    }
    void "testValidationAccountName"() {
        when:
        def user = new User(login: 'Lastname, Firstname (1234560)')
        user.validate()
        then:
        assertFalse(user.errors.allErrors.collect { it.toString() }.join("; "),user.hasErrors())
    }
    void "testValidationLastname"() {
        when:
        def user = new User(login: 'lastname',lastName: 'abcdEFGHI12390 ,.- ()')
        user.validate()
        then:
        assertFalse(user.errors.allErrors.collect { it.toString() }.join("; "),user.hasErrors())
    }
    void "testValidationFirstname"() {
        when:
        def user = new User(login: 'firstname',firstName: 'abcdEFGHI12390 ,.- ()')
        user.validate()
        then:
        assertFalse(user.errors.allErrors.collect { it.toString() }.join("; "),user.hasErrors())
    }
    void "testValidationFirstnameWithAccentedChars"() {
        when:
        def user = new User(login: 'firstname',firstName: 'áéíóúÁÉÍÓÚÃšçž',lastName: 'áéíóúÁÉÍÓÚÃšçž')
        user.validate()
        then:
        assertFalse(user.errors.allErrors.collect { it.toString() }.join("; "),user.hasErrors())
    }

    /**
     * Regression guard for https://github.com/rundeck/rundeck/issues/10493: an explicit
     * {@code identity} id generator requires the database column itself to auto-generate the
     * value, which fails against long-lived Postgres installations whose {@code rduser.id}
     * column predates/lacks a real DB-level identity or sequence. Unlike the persistence tests
     * in UserIntegrationSpec -- which run against a freshly-created (Hibernate ddl-auto)
     * schema that would mask this regression by always matching whatever generator is
     * currently declared -- this asserts directly on the source mapping, so it fails
     * immediately if the identity generator is reintroduced, regardless of test schema shape.
     */
    void "does not map id with an explicit generator"() {
        given:
        def candidatePaths = [
            "rundeckapp/grails-app/domain/rundeck/User.groovy",
            "grails-app/domain/rundeck/User.groovy",
        ]
        File userSource = candidatePaths
            .collect { new File(it).absoluteFile }
            .find { it.exists() }

        expect:
        userSource != null
        !(userSource.text =~ /id\s+generator\s*:/)
    }
}
