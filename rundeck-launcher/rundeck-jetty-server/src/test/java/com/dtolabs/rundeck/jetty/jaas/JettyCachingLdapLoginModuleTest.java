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

import java.util.Collections;
import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.auth.login.LoginException;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("rawtypes")
public class JettyCachingLdapLoginModuleTest {

    @Test
    public void testGetEnvironmentNoSSL() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._contextFactory = "foo";
        module._providerUrl = "ldap://localhost";

        Hashtable env = module.getEnvironment();
        assertFalse("Expected ldap socket factory to be unset",
                           env.containsKey("java.naming.ldap.factory.socket"));
    }

    @Test
    public void testGetEnvironmentSSLProviderUrl() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._contextFactory = "foo";
        String host = "somehost";
        module._providerUrl = String.format("ldaps://%s", host);

        Hashtable env = module.getEnvironment();
        assertEquals("Expected ldap socket factory to be unset",
                            "com.dtolabs.rundeck.jetty.jaas.HostnameVerifyingSSLSocketFactory",
                            env.get("java.naming.ldap.factory.socket"));
        assertEquals("Expected target host to be localhost", host, HostnameVerifyingSSLSocketFactory.getTargetHost());
    }
    
    @Test
    public void testBindingLoginInvalidLogin() throws LoginException, NamingException {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._contextFactory = "foo";
        module._providerUrl = "ldap://localhost";
        module._userBaseDn = "cn=foo,ou=bar,o=baz";
        JettyCachingLdapLoginModule spy = spy(module);
        doThrow(new NamingException("Something went wrong")).when(spy).newInitialDirContext(any(Hashtable.class));
        
        try {
            spy.bindingLogin("user", "someBadPassword");
            fail("Should have thrown a NamingException");
        } catch (NamingException e) {
            assertNull("Current user should not be set after authentication failure", spy.getCurrentUser());
            assertFalse(spy.isAuthenticated());
        }
    }
    
    @Test
    public void testBindingLoginSuccess() throws LoginException, NamingException {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        JettyCachingLdapLoginModule spy = spy(module);
        spy._contextFactory = "foo";
        spy._providerUrl = "ldap://localhost";
        spy._userBaseDn = "cn=foo,ou=bar,o=baz";
        DirContext mockContext = mock(InitialDirContext.class);
        doReturn(mockContext).when(spy).newInitialDirContext(any(Hashtable.class));
        doReturn(Collections.emptyList()).when(spy)
            .getUserRolesByDn(any(DirContext.class), any(String.class), any(String.class));
        
        assertTrue("bindingLogin should have returned true", spy.bindingLogin("user", "theRightPassword"));
        assertNotNull("CurrentUser should be set after authentication success", spy.getCurrentUser());
        assertEquals("user",spy.getCurrentUser().getUserName());
        assertTrue(spy.isAuthenticated());
    }
}
