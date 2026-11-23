/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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
package rundeck.services

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class UploadSizeLimitServiceSpec extends Specification implements ServiceUnitTest<UploadSizeLimitService> {

    def setup() {
        service.configurationService = new ConfigurationService()
    }

    def "limit is 2MB when the properties are unset"() {
        given:
        service.configurationService.appConfig = [:]

        expect:
        service.keyStorageMaxBytes() == UploadSizeLimitService.DEFAULT_MAX_BYTES
        service.aclMaxBytes() == UploadSizeLimitService.DEFAULT_MAX_BYTES
        service.keyStorageMaxSizeLabel() == '2MB'
        service.aclMaxSizeLabel() == '2MB'
        service.systemConfigProps*.key == [
                UploadSizeLimitService.KEY_STORAGE_CONFIG_KEY,
                UploadSizeLimitService.ACL_CONFIG_KEY
        ]
    }

    def "configured sizes replace the default and invalid values stay at 2MB"() {
        given:
        service.configurationService.appConfig = [
                (UploadSizeLimitService.KEY_STORAGE_PROPERTY): keySize,
                (UploadSizeLimitService.ACL_PROPERTY)        : aclSize
        ]

        expect:
        service.keyStorageMaxBytes() == keyBytes
        service.aclMaxBytes() == aclBytes
        service.keyStorageMaxSizeLabel() == keyLabel
        service.aclMaxSizeLabel() == aclLabel

        where:
        keySize | aclSize | keyBytes | aclBytes | keyLabel | aclLabel
        '5MB'   | '1KB'   | 5L * 1024L * 1024L | 1024L | '5MB' | '1KB'
        '0'     | 'nope'  | UploadSizeLimitService.DEFAULT_MAX_BYTES | UploadSizeLimitService.DEFAULT_MAX_BYTES | '2MB' | '2MB'
        ''      | null    | UploadSizeLimitService.DEFAULT_MAX_BYTES | UploadSizeLimitService.DEFAULT_MAX_BYTES | '2MB' | '2MB'
    }

    def "declared length and text checks"() {
        expect:
        !UploadSizeLimitService.declaredLengthExceeds(-1L, 4L)
        !UploadSizeLimitService.declaredLengthExceeds(4L, 4L)
        UploadSizeLimitService.declaredLengthExceeds(5L, 4L)
        !UploadSizeLimitService.textExceeds('abcd', 4L)
        UploadSizeLimitService.textExceeds('abcde', 4L)
        !UploadSizeLimitService.textExceeds(null, 4L)
    }

    def "readLimited stops at the maximum"() {
        when:
        UploadSizeLimitService.readLimited(new ByteArrayInputStream('12345'.bytes), 4L)

        then:
        thrown(UploadTooLargeException)
    }

    def "readLimited returns the body when it fits"() {
        expect:
        UploadSizeLimitService.readLimited(new ByteArrayInputStream('1234'.bytes), 4L) == '1234'.bytes
    }

    def "missing service uses the default limit"() {
        expect:
        UploadSizeLimitService.resolveKeyStorageMaxBytes(null) == UploadSizeLimitService.DEFAULT_MAX_BYTES
        UploadSizeLimitService.resolveAclMaxBytes(null) == UploadSizeLimitService.DEFAULT_MAX_BYTES
        UploadSizeLimitService.resolveKeyStorageMaxSizeLabel(null) == '2MB'
        UploadSizeLimitService.resolveAclMaxSizeLabel(null) == '2MB'
    }
}
