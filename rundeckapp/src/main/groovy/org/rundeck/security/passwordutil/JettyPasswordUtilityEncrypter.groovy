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

import com.dtolabs.rundeck.core.encrypter.PasswordUtilityEncrypter
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil
import org.eclipse.jetty.util.security.Credential
import org.eclipse.jetty.util.security.Password
import org.rundeck.jaas.jetty.BcryptCredentialProvider
import org.springframework.security.crypto.bcrypt.BCrypt


class JettyPasswordUtilityEncrypter implements PasswordUtilityEncrypter {
    private List<Property> formProperties = [
            PropertyUtil.string("username","Username","Optional, but necessary for Crypt encoding",false,null),
            PropertyUtil.string("valueToEncrypt","Value To Encrypt","The text you want to encrypt",true,null)
    ]

    @Override
    String name() {
        return "Jetty"
    }

    @Override
    Map encrypt(Map params) {
        def result = [:]
        String valToEncrypt = params.get("valueToEncrypt")
        String un = params.get("username")
        result.bcrypt = BcryptCredentialProvider.BcryptCredential.encodePassword(valToEncrypt)
        result.obfuscate = Password.obfuscate(valToEncrypt)
        result.md5 = Credential.MD5.digest(valToEncrypt)
        if(un) {
            result.crypt = Credential.Crypt.crypt(un, valToEncrypt)
        }
        return result
    }

    @Override
    List<Property> formProperties() {
        return formProperties
    }

}
