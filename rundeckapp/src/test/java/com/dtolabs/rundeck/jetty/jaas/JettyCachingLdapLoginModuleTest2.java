/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.jetty.jaas;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.jetty.jaas.callback.ObjectCallback;
import org.eclipse.jetty.jaas.spi.UserInfo;
import org.junit.Assert;
import org.junit.Test;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyString;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("rawtypes")
public class JettyCachingLdapLoginModuleTest2 {

    private final String user1       = "user1";
    private final String user2       = "user2";
    private final String password    = "password";
    private final String role1       = "role1";
    private final String role2       = "role2";
    private final String nestedRole1 = "nestedRole1";

    @Test
    public void testTimeoutDefaults() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._contextFactory = "foo";
        module._providerUrl = "ldap://localhost";

        Hashtable env = module.getEnvironment();
        Assert.assertTrue(
            "Expected ldap read timeout default",
            env.containsKey("com.sun.jndi.ldap.read.timeout")
        );
        Assert.assertEquals("Expected ldap read timeout default", "0",
                            env.get("com.sun.jndi.ldap.read.timeout")
        );
        Assert.assertTrue(
            "Expected ldap connect timeout default",
            env.containsKey("com.sun.jndi.ldap.connect.timeout")
        );
        Assert.assertEquals(
            "Expected ldap connect timeout default", "0",
            env.get("com.sun.jndi.ldap.connect.timeout")
        );
    }

    @Test
    public void testTimeoutReadInitialize() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module.initializeOptions(new HashMap() {{
            put("timeoutRead", "100");
        }});

        Assert.assertEquals("Expected ldap read timeout value", 100, module._timeoutRead);
        Assert.assertEquals("Expected ldap connect timeout default", 0, module._timeoutConnect);
    }

    @Test
    public void testTimeoutConnectInitialize() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module.initializeOptions(new HashMap() {{
            put("timeoutConnect", "200");
        }});

        Assert.assertEquals("Expected ldap read timeout default", 0, module._timeoutRead);
        Assert.assertEquals("Expected ldap connect timeout value", 200, module._timeoutConnect);
    }

    @Test
    public void testTimeoutRead() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._contextFactory = "foo";
        module._providerUrl = "ldap://localhost";
        module._timeoutRead = 1000;

        Hashtable env = module.getEnvironment();
        Assert.assertTrue(
            "Expected ldap read timeout default",
            env.containsKey("com.sun.jndi.ldap.read.timeout")
        );
        Assert.assertEquals("Expected ldap read timeout value", "1000",
                            env.get("com.sun.jndi.ldap.read.timeout")
        );

        Assert.assertTrue(
            "Expected ldap connect timeout default",
            env.containsKey("com.sun.jndi.ldap.connect.timeout")
        );
        Assert.assertEquals(
            "Expected ldap connect timeout default", "0",
            env.get("com.sun.jndi.ldap.connect.timeout")
        );
    }

    @Test
    public void testTimeoutConnect() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._contextFactory = "foo";
        module._providerUrl = "ldap://localhost";
        module._timeoutConnect = 5000;

        Hashtable env = module.getEnvironment();
        Assert.assertTrue(
            "Expected ldap read timeout default",
            env.containsKey("com.sun.jndi.ldap.read.timeout")
        );
        Assert.assertEquals("Expected ldap read timeout default", "0",
                            env.get("com.sun.jndi.ldap.read.timeout")
        );

        Assert.assertTrue(
            "Expected ldap connect timeout default",
            env.containsKey("com.sun.jndi.ldap.connect.timeout")
        );
        Assert.assertEquals(
            "Expected ldap connect timeout value", "5000",
            env.get("com.sun.jndi.ldap.connect.timeout")
        );
    }

    @Test
    public void testGetEnvironmentNoSSL() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._contextFactory = "foo";
        module._providerUrl = "ldap://localhost";

        Hashtable env = module.getEnvironment();
        Assert.assertFalse(
            "Expected ldap socket factory to be unset",
            env.containsKey("java.naming.ldap.factory.socket")
        );
    }

    @Test
    public void testGetEnvironmentSSLProviderUrl() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._contextFactory = "foo";
        String host = "somehost";
        module._providerUrl = String.format("ldaps://%s", host);

        Hashtable env = module.getEnvironment();
        Assert.assertEquals(
            "Expected ldap socket factory to be unset",
            "com.dtolabs.rundeck.jetty.jaas.HostnameVerifyingSSLSocketFactory",
            env.get("java.naming.ldap.factory.socket")
        );
        Assert.assertEquals(
            "Expected target host to be localhost",
            host,
            HostnameVerifyingSSLSocketFactory.getTargetHost()
        );
    }

    @Test
    public void testShouldGetNestedGroups() {
        JettyCachingLdapLoginModule module = getJettyCachingLdapLoginModule(false);
        module._nestedGroups = true;
        module.rolesPerPage = 1000;
        module._providerUrl = "ldap://localhost";
        try {
            UserInfo userInfo = module.getUserInfo(user1);
            assertThat(userInfo.getUserName(), is(user1));

            List<String> actualRoles = userInfo.getRoleNames();
            List<String> expectedRoles = Arrays.asList(role1, role2, nestedRole1);
            assertThat(actualRoles, is(expectedRoles));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testShouldGetNestedGroupsWithAD() {
        JettyCachingLdapLoginModule module = getJettyCachingLdapLoginModule(true);
        module._nestedGroups = true;
        module.rolesPerPage = 1000;
        module._providerUrl = "ldap://localhost";
        try {
            UserInfo userInfo = module.getUserInfo(user1);
            assertThat(userInfo.getUserName(), is(user1));

            List<String> actualRoles = userInfo.getRoleNames();
            List<String> expectedRoles = Arrays.asList(role1, role2, nestedRole1);
            assertThat(actualRoles, is(expectedRoles));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testShouldGetPaginatedGroupsWithAD() {
        JettyCachingLdapLoginModule module = getJettyCachingLdapLoginModule(true);
        module._nestedGroups = true;
        module.rolePagination = true;
        module.rolesPerPage = 1000;
        module._providerUrl = "ldap://localhost";
        try {
            UserInfo userInfo = module.getUserInfo(user1);
            assertThat(userInfo.getUserName(), is(user1));

            List<String> actualRoles = userInfo.getRoleNames();
            List<String> expectedRoles = Arrays.asList(role1, role2, nestedRole1);
            assertThat(actualRoles, is(expectedRoles));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testShouldNotGetNestedGroups() {
        JettyCachingLdapLoginModule module = getJettyCachingLdapLoginModule(false);

        try {
            UserInfo userInfo = module.getUserInfo(user1);
            assertThat(userInfo.getUserName(), is(user1));

            List<String> actualRoles = userInfo.getRoleNames();
            List<String> expectedRoles = Arrays.asList(role1, role2);
            assertThat(actualRoles, is(expectedRoles));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }


    private CallbackHandler createCallbacks(final String user1, final Object password) {
        return new CallbackHandler() {
            @Override
            public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                NameCallback name = (NameCallback) callbacks[0];
                name.setName(user1);
                ObjectCallback pass = (ObjectCallback) callbacks[1];
                pass.setObject(password);
            }
        };
    }

    @Test
    public void testDisallowEmptyPassword() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._debug = true;
        module.setCallbackHandler(createCallbacks("user1", ""));
        expectLoginException(module);
    }

    private void expectLoginException(final JettyCachingLdapLoginModule module) {
        try {
            assertFalse(module.login());
        } catch (FailedLoginException e) {
            assertTrue("expected", true);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testDisallowNullPasword() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._debug = true;
        module.setCallbackHandler(createCallbacks("user1", null));
        expectLoginException(module);
    }

    @Test
    public void testDisallowEmptyCharArrayPasword() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._debug = true;
        char[] empty = {};
        module.setCallbackHandler(createCallbacks("user1", empty));
        expectLoginException(module);
    }

    @Test
    public void testDisallowEmptyUsername() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._debug = true;
        module.setCallbackHandler(createCallbacks("", "xyz"));
        expectLoginException(module);
    }

    @Test
    public void testDisallowNullUsername() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        module._debug = true;
        module.setCallbackHandler(createCallbacks(null, "xyz"));
        try {
            assertFalse(module.login());

        } catch (FailedLoginException e) {
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetUserAttributesEmail_FirstName_LastName() {
        JettyCachingLdapLoginModule module = getJettyCachingLdapLoginModule(false);
        module._debug = true;
        module.setCallbackHandler(createCallbacks("user1", "password"));
        module.setSubject(new Subject());
        try {
            assertTrue(module.login());
            assertTrue(module.commit());
            assertTrue(module.getSubject().getPrincipals().stream().anyMatch(p -> p instanceof LdapEmailPrincipal ));
            assertTrue(module.getSubject().getPrincipals().stream().anyMatch(p -> p instanceof LdapFirstNamePrincipal ));
            assertTrue(module.getSubject().getPrincipals().stream().anyMatch(p -> p instanceof LdapLastNamePrincipal ));

        } catch (FailedLoginException e) {
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testSetOptions_Email_First_Last() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();
        HashMap options = new HashMap();
        options.put("userLastNameAttribute","lastAttrib");
        options.put("userFirstNameAttribute","firstAttrib");
        options.put("userEmailAttribute","emailAttrib");
        module.initializeOptions(options);
        assertEquals("lastAttrib",module._userLastNameAttribute);
        assertEquals("firstAttrib",module._userFirstNameAttribute);
        assertEquals("emailAttrib",module._userEmailAttribute);
    }


    private JettyCachingLdapLoginModule getJettyCachingLdapLoginModule(boolean activeDirectory) {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule();

        module._userBaseDn = "ou=users,dc=example,dc=com";
        module._roleBaseDn = "ou=groups,dc=example,dc=com";

        DirContext rootContext = mock(DirContext.class);
        LdapContext ldapContext = mock(LdapContext.class);
        NamingEnumeration<SearchResult> userSearchResults = mock(NamingEnumeration.class);
        when(userSearchResults.hasMoreElements()).thenReturn(true);
        SearchResult userSearchResult = mock(SearchResult.class);
        Attributes userAttributes = mock(Attributes.class);
        Attribute passwordAttribute = mock(Attribute.class);
        when(userAttributes.get(module._userPasswordAttribute)).thenReturn(passwordAttribute);
        when(userSearchResult.getAttributes()).thenReturn(userAttributes);
        when(userSearchResults.nextElement()).thenReturn(userSearchResult);
        when(userAttributes.get(module._userEmailAttribute)).thenReturn(new BasicAttribute(module._userEmailAttribute,"user@example.com"));
        when(userAttributes.get(module._userFirstNameAttribute)).thenReturn(new BasicAttribute(module._userFirstNameAttribute,"First"));
        when(userAttributes.get(module._userLastNameAttribute)).thenReturn(new BasicAttribute(module._userLastNameAttribute,"Last"));

        try {
            when(passwordAttribute.get()).thenReturn(password.getBytes());
            when(rootContext.search(
                eq(module._userBaseDn),
                anyString(),
                any(Object[].class),
                any(SearchControls.class)
            )).thenReturn(userSearchResults);
        } catch (NamingException e) {
            e.printStackTrace();
            fail();
        }

        NamingEnumeration<SearchResult> roleSearchResults = mock(NamingEnumeration.class);
        when(roleSearchResults.hasMoreElements()).thenReturn(true, false);
        SearchResult rolesSearchResult = mock(SearchResult.class);
        Attributes roleAttributes = mock(Attributes.class);
        when(rolesSearchResult.getAttributes()).thenReturn(roleAttributes);
        Attribute roleAttribute = mock(Attribute.class);
        when(roleAttributes.get(module._roleNameAttribute)).thenReturn(roleAttribute);
        NamingEnumeration roles = mock(NamingEnumeration.class);

        NamingEnumeration<SearchResult> allRolesSearchResults = mock(NamingEnumeration.class);
        when(allRolesSearchResults.hasMoreElements()).thenReturn(true, true, true, false);

        SearchResult role1SearchResult = mock(SearchResult.class);
        Attributes role1Attributes = mock(Attributes.class);
        when(role1SearchResult.getAttributes()).thenReturn(role1Attributes);
        Attribute role1NameAttribute = mock(Attribute.class);
        Attribute role1MemberAttribute = mock(Attribute.class);
        when(role1Attributes.get(eq(module._roleNameAttribute))).thenReturn(role1NameAttribute);
        when(role1Attributes.get(eq(module._roleMemberAttribute))).thenReturn(role1MemberAttribute);
        NamingEnumeration role1Roles = mock(NamingEnumeration.class);


        SearchResult role2SearchResult = mock(SearchResult.class);
        Attributes role2Attributes = mock(Attributes.class);
        when(role2SearchResult.getAttributes()).thenReturn(role2Attributes);
        Attribute role2NameAttribute = mock(Attribute.class);
        Attribute role2MemberAttribute = mock(Attribute.class);
        when(role2Attributes.get(eq(module._roleNameAttribute))).thenReturn(role2NameAttribute);
        when(role2Attributes.get(eq(module._roleMemberAttribute))).thenReturn(role2MemberAttribute);
        NamingEnumeration role2Roles = mock(NamingEnumeration.class);


        SearchResult nestedRole1SearchResult = mock(SearchResult.class);
        Attributes nestedRole1Attributes = mock(Attributes.class);
        when(nestedRole1SearchResult.getAttributes()).thenReturn(nestedRole1Attributes);
        Attribute nestedRole1NameAttribute = mock(Attribute.class);
        Attribute nestedRole1MemberAttribute = mock(Attribute.class);
        when(nestedRole1Attributes.get(eq(module._roleNameAttribute))).thenReturn(nestedRole1NameAttribute);
        when(nestedRole1Attributes.get(eq(module._roleMemberAttribute))).thenReturn(nestedRole1MemberAttribute);
        NamingEnumeration nestedRole1Roles = mock(NamingEnumeration.class);
        NamingEnumeration nestedRole1Members = mock(NamingEnumeration.class);

        try {
            when(rootContext.search(
                eq(module._roleBaseDn),
                anyString(),
                any(Object[].class),
                any(SearchControls.class)
            )).thenReturn(roleSearchResults);
            when(roleSearchResults.nextElement()).thenReturn(rolesSearchResult);
            when(roleAttribute.getAll()).thenReturn(roles);
            when(roles.hasMore()).thenReturn(true, true, false);
            when(roles.next()).thenReturn(role1, role2);

            when((LdapContext) rootContext.lookup(anyString())).thenReturn(ldapContext).thenReturn(ldapContext);
            when(ldapContext.search(
                eq(module._roleBaseDn),
                eq(module._roleMemberFilter),
                any(SearchControls.class)
            )).thenReturn(allRolesSearchResults);
            when(allRolesSearchResults.nextElement()).thenReturn(
                role1SearchResult,
                role2SearchResult,
                nestedRole1SearchResult
            );

            when(ldapContext.search(
                    eq(module._roleBaseDn),
                    anyString(),
                    any(Object[].class),
                    any(SearchControls.class)
            )).thenReturn(roleSearchResults);

            when(role1NameAttribute.getAll()).thenReturn(role1Roles);
            when(role1Roles.next()).thenReturn(role1);

            when(role2NameAttribute.getAll()).thenReturn(role2Roles);
            when(role2Roles.next()).thenReturn(role2);

            when(nestedRole1NameAttribute.getAll()).thenReturn(nestedRole1Roles);
            when(nestedRole1Roles.hasMore()).thenReturn(true);
            when(nestedRole1Roles.next()).thenReturn(nestedRole1);
            when(nestedRole1MemberAttribute.getAll()).thenReturn(nestedRole1Members);
            when(nestedRole1Members.hasMore()).thenReturn(true, true, true, false);
            if (activeDirectory) {
                when(nestedRole1Members.next()).thenReturn(
                    "CN=" + role1 + "," + module._roleBaseDn,
                    "uid=" + user2 + "," + module._roleBaseDn
                );
            } else {
                when(nestedRole1Members.next()).thenReturn(
                    "cn=" + role1 + "," + module._roleBaseDn,
                    "uid=" + user2 + "," + module._roleBaseDn
                );
            }


        } catch (NamingException e) {
            e.printStackTrace();
            fail();
        }

        module._rootContext = rootContext;
        module.ldapContext = ldapContext;
        return module;
    }


}