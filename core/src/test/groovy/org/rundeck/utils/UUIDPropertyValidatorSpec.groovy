/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.utils

import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

class UUIDPropertyValidatorSpec extends Specification {
    def "isValid"() {
        expect:
            UUIDPropertyValidator.isValidUUID('01234567-89ab-cdef-0123-456789abcdef')
            UUIDPropertyValidator.isValidUUID('00000000-0000-0000-0000-000000000000')
    }

    @Unroll
    def "isValid wrong len"() {
        expect:
            !UUIDPropertyValidator.isValidUUID(uuid)

        where:
            uuid << [
                    '',
                    ' 01234567-89ab-cdef-0123-456789abcdef',
                    '01234567-89ab-cdef-0123-456789abcdef ',
                    'A01234567-89ab-cdef-0123-456789abcdef',
                    '1234567-89ab-cdef-0123-456789abcdef',
            ]
    }


    @Unroll
    def "validate wrong len"() {
        when:
            UUIDPropertyValidator.validate(uuid)
        then:
            ValidationException e = thrown()
            e.message.contains('Expected 36 characters')

        where:
            uuid << [
                    '',
                    ' 01234567-89ab-cdef-0123-456789abcdef',
                    '01234567-89ab-cdef-0123-456789abcdef ',
                    'A01234567-89ab-cdef-0123-456789abcdef',
                    '1234567-89ab-cdef-0123-456789abcdef',
            ]
    }

    @Unroll
    def "isValid wrong chars"() {
        expect:
            !UUIDPropertyValidator.isValidUUID(uuid)

        where:
            uuid << [
                    'Z1234567-89ab-cdef-0123-456789abcdef',
                    '01234567-89ab-cdef-01234-56789abcdef',
                    '01234567-89ab-cdef0-123-456789abcdef',
                    '01234567-89abc-def-0123-456789abcdef',
                    '012345678-9ab-cdef-0123-456789abcdef',

            ]
    }

    @Unroll
    def "validation wrong chars"() {
        when:
            UUIDPropertyValidator.validate(uuid)
        then:
            ValidationException e = thrown()
            e.message.contains('Expected valid UUID')

        where:
            uuid << [
                    'Z1234567-89ab-cdef-0123-456789abcdef',
                    '01234567-89ab-cdef-01234-56789abcdef',
                    '01234567-89ab-cdef0-123-456789abcdef',
                    '01234567-89abc-def-0123-456789abcdef',
                    '012345678-9ab-cdef-0123-456789abcdef',

            ]
    }
}
