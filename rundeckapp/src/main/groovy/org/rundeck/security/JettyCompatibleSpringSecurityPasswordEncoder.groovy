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

import org.grails.web.util.WebUtils
import org.rundeck.jaas.PasswordCredential
import org.rundeck.jaas.jetty.BcryptCredentialProvider
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Password encoder compatible with Jetty password formats.
 * Updated for JAAS refactoring - uses org.rundeck.jaas.PasswordCredential
 * instead of removed org.eclipse.jetty.util.security classes.
 */
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
        
        // Use PasswordCredential for MD5 and CRYPT
        if(encPass.startsWith("MD5:") || encPass.startsWith("CRYPT:")) {
            PasswordCredential credential = PasswordCredential.getCredential(encPass)
            return credential.check(rawPass.toString())
        }
        
        // OBF format check - fall back to false (Jetty-specific, deprecated)
        if(encPass.startsWith("OBF:")) {
            // OBF format was Jetty-specific and is no longer supported
            // Passwords should be migrated to MD5, CRYPT, or BCRYPT
            return false
        }
        
        // BCRYPT uses special provider
        if(encPass.startsWith("BCRYPT:")) {
            return new BcryptCredentialProvider().getCredential(encPass).check(rawPass)
        }
        
        // Plain text comparison
        return encPass == rawPass
    }
}
