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
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil
import org.eclipse.jetty.util.security.Credential
import org.eclipse.jetty.util.security.Password


class HiddenInputPasswordUtilityEncrypter implements PasswordUtilityEncrypter {
    private List<Property> formProperties = []

    HiddenInputPasswordUtilityEncrypter() {
        formProperties.add(PropertyUtil.string("username", "Username", "Optional, but necessary for Crypt encoding", false, null));
        Map<String,Object> passwordRO = new HashMap<>();
        passwordRO.put("displayType","PASSWORD");
        formProperties.add(PropertyUtil.string("pwd", "Password", "The password", true, null, null, PropertyScope.Unspecified, passwordRO));
        formProperties.add(PropertyUtil.string("pwdCheck","Password Verification","Password verification",true,null,null,PropertyScope.Unspecified,passwordRO));
    }
    @Override
    String name() {
        return "Hidden Input";
    }

    @Override
    Map encrypt(final Map params) {
        Map returnVal = new HashMap();
        String un = (String)params.get("username");
        String pwd = (String)params.get("pwd");
        String pwdCheck = (String)params.get("pwdCheck");
        if(!pwd) returnVal.put("Error","Password is null");
        if(!pwdCheck) returnVal.put("Error","Password Verification value is null");
        if(!pwd.equals(pwdCheck)) returnVal.put("Error","Password and Password Verification values do not match");
        if(!returnVal.isEmpty()) return returnVal;
        returnVal.put("obfuscate", Password.obfuscate(pwd));
        returnVal.put("md5", Credential.MD5.digest(pwd));
        if(un) returnVal.put("crypt", Credential.Crypt.crypt(un,pwd));
        return returnVal;
    }

    @Override
    public List<Property> formProperties() {
        return formProperties;
    }
}
