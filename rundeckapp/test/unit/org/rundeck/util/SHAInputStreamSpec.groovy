/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.util

import spock.lang.Specification

import java.security.MessageDigest

/**
 * @author greg
 * @since 2/22/17
 */
class SHAInputStreamSpec extends Specification {
    def "read byte array"() {
        given:
        def data = [0, 1, 2, 3] as byte[]
        def arr1 = new ByteArrayInputStream(data)
        def sha = new SHAInputStream(arr1)
        def buff = new byte[data.length]
        MessageDigest digest = MessageDigest.getInstance("SHA-256")

        when:
        def len = sha.read(buff)
        digest.update(data)
        def other = digest.digest()
        def osha = String.format("%064x", new BigInteger(1, other))

        then:
        len == 4
        buff == data
        sha.SHAString == osha

    }

    def "read byte array with offset"() {
        given:
        def data = [0, 1, 2, 3, 4, 5, 6] as byte[]
        def arr1 = new ByteArrayInputStream(data)
        def sha = new SHAInputStream(arr1)
        def buff = new byte[data.length]
        MessageDigest digest = MessageDigest.getInstance("SHA-256")

        when:
        def len = sha.read(buff, 1, 5)
        digest.update(data, 0, 5)
        def other = digest.digest()
        def osha = String.format("%064x", new BigInteger(1, other))

        then:
        len == 5
        buff == [0, 0, 1, 2, 3, 4, 0] as byte[]
        sha.SHAString == osha
    }

    def "read single byte"() {
        given:
        def data = [val] as byte[]
        def arr1 = new ByteArrayInputStream(data)
        def sha = new SHAInputStream(arr1)
        MessageDigest digest = MessageDigest.getInstance("SHA-256")

        when:
        def abyte = sha.read()
        digest.update((byte) val)
        def other = digest.digest()
        def osha = String.format("%064x", new BigInteger(1, other))

        then:
        abyte == val
        sha.SHAString == osha

        where:
        val  | _
        1    | _
        0xFF | _
        0x75 | _
    }
}
