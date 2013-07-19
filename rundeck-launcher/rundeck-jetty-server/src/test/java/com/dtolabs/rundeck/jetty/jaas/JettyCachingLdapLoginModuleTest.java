/*
 * Copyright 2013 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.jetty.jaas;

import java.util.Hashtable;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("rawtypes")
public class JettyCachingLdapLoginModuleTest {

    @Test
    public void testGetEnvironmentNoSSL() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._contextFactory = "foo";
        module._providerUrl = "ldap://localhost";

        Hashtable env = module.getEnvironment();
        Assert.assertFalse("Expected ldap socket factory to be unset",
                           env.containsKey("java.naming.ldap.factory.socket"));
    }

    @Test
    public void testGetEnvironmentSSLProviderUrl() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._contextFactory = "foo";
        String host = "somehost";
        module._providerUrl = String.format("ldaps://%s", host);

        Hashtable env = module.getEnvironment();
        Assert.assertEquals("Expected ldap socket factory to be unset",
                            "com.dtolabs.rundeck.jetty.jaas.HostnameVerifyingSSLSocketFactory",
                            env.get("java.naming.ldap.factory.socket"));
        Assert.assertEquals("Expected target host to be localhost", host, HostnameVerifyingSSLSocketFactory.getTargetHost());
    }
}
