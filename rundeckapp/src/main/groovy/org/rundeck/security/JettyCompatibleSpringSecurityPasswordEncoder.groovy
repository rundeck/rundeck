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
import org.springframework.security.authentication.encoding.PlaintextPasswordEncoder

/**
 * This class checks passwords the same way the Jetty jaas module.
 * When the grails spring security plugin updates to spring security 5.x this class will no longer
 * work and will need to be migrated to the new encoder technique
 */
@Deprecated
class JettyCompatibleSpringSecurityPasswordEncoder extends PlaintextPasswordEncoder {

    @Override
    String encodePassword(final String rawPass, final Object salt) {
        return super.encodePassword(rawPass,salt)
    }

    @Override
    boolean isPasswordValid(final String encPass, final String rawPass, final Object salt) {
        if(!encPass || !rawPass) return false
        if(encPass.startsWith("MD5:")) return Credential.MD5.digest(rawPass) == encPass
        if(encPass.startsWith("OBF:")) return Password.obfuscate(rawPass) == encPass
        if(encPass.startsWith("CRYPT:")) return Credential.Crypt.crypt(username, rawPass) == encPass
        return super.isPasswordValid(encPass,rawPass,salt)
    }

    String getUsername() {
        WebUtils.retrieveGrailsWebRequest().getCurrentRequest().getParameter("j_username")
    }
}
