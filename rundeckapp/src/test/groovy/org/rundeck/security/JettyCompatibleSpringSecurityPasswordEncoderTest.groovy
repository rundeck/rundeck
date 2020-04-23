/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.security

import spock.lang.Specification


class JettyCompatibleSpringSecurityPasswordEncoderTest extends Specification {
    JettyCompatibleSpringSecurityPasswordEncoder encoder

    def setup() {
        encoder = new JettyCompatibleSpringSecurityPasswordEncoder()
        encoder.metaClass.getUsername = { -> return "jsmith" }
    }

    def "Is Password Valid Plaintext"() {
        expect:
        encoder.matches("plaintext","plaintext")
        !encoder.matches("plaintext","plaintxt")
    }

    def "Is Password Valid OBF"() {
        expect:
        encoder.matches("obfusticated","OBF:1uve1sho1w8h1vgz1vgv1wui1wtw1vfz1vfv1w991shu1uus")
        !encoder.matches("nomatch","OBF:1uve1sho1w8h1vgz1vgv1wui1wtw1vfz1vfv1w991shu1uus")
    }
    def "Is Password Valid MD5"() {
        expect:
        encoder.matches("mymd5passwd","MD5:72edc62d1e5f879981032f4ccd82be54")
        !encoder.matches("nomatch","MD5:72edc62d1e5f879981032f4ccd82be54")
    }
    def "Is Password Valid CRYPT"() {
        expect:
        encoder.matches("mycryptpass","CRYPT:jsf1JcISnTyL6",)
        !encoder.matches("nomatch","CRYPT:jsf1JcISnTyL6",)
    }
    def "Is Password Valid null"() {
        expect:
        !encoder.matches(null, "MD5:7ddf32e17a6ac5ce04a8ecbf782ca509")
        !encoder.matches("somepassword", null)
    }
}
