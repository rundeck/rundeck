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
package org.rundeck.security.passwordutil

import spock.lang.Specification


class HiddenInputPasswordUtilityEncrypterTest extends Specification {
    def "Encrypt"() {
        when:
        HiddenInputPasswordUtilityEncrypter encrypter = new HiddenInputPasswordUtilityEncrypter();
        Map params = [:]
        params.username = "auser"
        params.pwd = "test"
        params.pwdCheck = "test"

        Map result = encrypter.encrypt(params);

        then:
        result.obfuscate
        result.md5
        result.crypt
    }

    def "Password and verify no match check"() {

        when:
        HiddenInputPasswordUtilityEncrypter encrypter = new HiddenInputPasswordUtilityEncrypter();
        Map params = [:]
        params.pwd = "test"
        params.pwdCheck = "nomatch"

        Map result = encrypter.encrypt(params);

        then:
        result.Error == "Password and Password Verification values do not match"
    }
}
