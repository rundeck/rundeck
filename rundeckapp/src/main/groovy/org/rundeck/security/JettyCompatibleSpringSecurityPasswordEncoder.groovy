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

import org.eclipse.jetty.util.security.Credential
import org.eclipse.jetty.util.security.Password
import org.grails.web.util.WebUtils
import org.rundeck.jaas.jetty.BcryptCredentialProvider
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
class JettyCompatibleSpringSecurityPasswordEncoder implements PasswordEncoder {
    String getUsername() {
        WebUtils.retrieveGrailsWebRequest().getCurrentRequest().getParameter("j_username")
    }
    @Override
    String encode(final CharSequence rawPassword) {
        //does not encode password
        return rawPassword
    }
    @Override
    boolean matches(final CharSequence rawPass, final String encPass) {
        if(!encPass || !rawPass) return false
        if(encPass.startsWith("MD5:")) return Credential.MD5.digest(rawPass.toString()) == encPass
        if(encPass.startsWith("OBF:")) return Password.obfuscate(rawPass.toString()) == encPass
        if(encPass.startsWith("CRYPT:")) return Credential.Crypt.crypt(username, rawPass.toString()) == encPass
        if(encPass.startsWith("BCRYPT:")) return new BcryptCredentialProvider().getCredential(encPass).check(rawPass)
        return encPass == rawPass
    }
}
