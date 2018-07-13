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
package com.dtolabs.rundeck.jetty.jaas

import spock.lang.Specification


class JettyCachingLdapLoginModuleTest extends Specification {
    def "DecodeBase64EncodedPwd"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        expect:
        module.decodeBase64EncodedPwd("noencoding") == "noencoding"
        module.decodeBase64EncodedPwd("MD5:tmXytOxIA6rGWhEKPFfv3A==") == "MD5:b665f2b4ec4803aac65a110a3c57efdc"
    }

    def "IsBase64"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        expect:
        !module.isBase64("notbase64")
        !module.isBase64("noencoding")
        module.isBase64("bXl0ZXN0c3RyaW5n")
        module.isBase64("bXl0ZXN0c3RyaW5nCg==")
    }
}
