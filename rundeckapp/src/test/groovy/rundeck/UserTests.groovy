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
import org.springframework.context.support.StaticMessageSource

import static org.junit.Assert.assertFalse

class UserTests extends HibernateSpec {

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
}
