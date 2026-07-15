/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
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

import com.dtolabs.rundeck.core.config.Features
import grails.util.Holders
import org.rundeck.jaas.PasswordCredential
import org.rundeck.jaas.RundeckPrincipal
import org.rundeck.jaas.RundeckRole
import org.rundeck.jaas.UserInfo
import org.springframework.context.ApplicationContext
import rundeck.services.ConfigurationService
import rundeck.services.feature.FeatureService
import spock.lang.Specification
import spock.lang.Unroll

import javax.naming.NamingEnumeration
import javax.naming.NamingException
import javax.naming.directory.Attribute
import javax.naming.directory.Attributes
import javax.naming.directory.BasicAttribute
import javax.naming.directory.BasicAttributes
import javax.naming.directory.DirContext
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult
import javax.security.auth.Subject
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.callback.NameCallback
import javax.security.auth.callback.PasswordCallback
import java.security.Principal

/**
 * Integration tests for case-insensitive username normalization in LDAP authentication
 */
class JettyCachingLdapLoginModuleCaseInsensitiveSpec extends Specification {

    private static final String TEST_USER = "testuser"
    private static final String TEST_PASSWORD = "password"
    private static final String TEST_ROLE = "developers"

    def "authenticate normalizes username to lowercase when feature enabled"() {
        given: "An LDAP login module with feature flag enabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(true)
        Subject testSubject = module.getSubject()
        
        and: "User logs in with mixed case username"
        setupCallbackHandler(module, "TestUser", TEST_PASSWORD)
        
        and: "Mock LDAP operations"
        mockLdapOperations(module, TEST_USER)

        when: "Authentication occurs"
        boolean result = module.login()
        // Manually add principals to subject (pattern from working tests)
        module.getCurrentUser().setJAASInfo(testSubject)

        then: "Authentication succeeds"
        result
        
        and: "Username in subject is lowercase"
        def userPrincipal = testSubject.getPrincipals(RundeckPrincipal).find()
        userPrincipal.name == "testuser"
    }

    def "authenticate preserves original case when feature disabled"() {
        given: "An LDAP login module with feature flag disabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(false)
        Subject testSubject = module.getSubject()
        
        and: "User logs in with mixed case username"
        setupCallbackHandler(module, "TestUser", TEST_PASSWORD)
        
        and: "Mock LDAP operations"
        mockLdapOperations(module, "TestUser")

        when: "Authentication occurs"
        boolean result = module.login()
        // Manually add principals to subject (pattern from working tests)
        module.getCurrentUser().setJAASInfo(testSubject)

        then: "Authentication succeeds"
        result
        
        and: "Username in subject preserves original case"
        def userPrincipal = testSubject.getPrincipals(RundeckPrincipal).find()
        userPrincipal.name == "TestUser"
    }

    @Unroll
    def "bindingLogin normalizes username '#inputUser' to '#expectedUser' when feature enabled"() {
        given: "An LDAP login module with feature flag enabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(true)
        module._forceBindingLogin = true
        module._userBaseDn = "ou=users,dc=example,dc=com"
        module._userIdAttribute = "uid"
        module._userObjectClass = "inetOrgPerson"
        module._roleBaseDn = "ou=roles,dc=example,dc=com"
        module._roleUsernameMemberAttribute = "memberUid"
        module._roleObjectClass = "groupOfUniqueNames"
        module._roleNameAttribute = "cn"
        module.rolePagination = false  // Disable role pagination to avoid ldapContext NPE
        
        and: "Mock LDAP search for findUser"
        def mockSearchResult = Mock(SearchResult) {
            getNameInNamespace() >> "cn=${expectedUser},ou=users,dc=example,dc=com"
            getAttributes() >> createMockAttributes(expectedUser, [TEST_ROLE])
        }
        
        def mockDirContext = Mock(DirContext) {
            search(*_) >> new EnumImpl<SearchResult>([mockSearchResult])
            close() >> {}
        }
        module._rootContext = mockDirContext
        
        and: "Mock role lookup"
        def mockRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get('cn') >> Mock(Attribute) {
                    getAll() >> new EnumImpl<String>([TEST_ROLE])
                }
            }
        }]
        
        DirContext userDir = Mock(DirContext) {
            search(*_) >> new EnumImpl<SearchResult>(mockRoles)
        }
        module.userBindDirContextCreator = { String user, Object pass -> userDir }

        when: "Binding login is performed"
        boolean result = module.bindingLogin(inputUser, TEST_PASSWORD)

        then: "Login succeeds"
        result
        
        and: "UserInfo contains normalized username"
        def currentUser = module.getCurrentUser()
        currentUser.getUserInfo().userName == expectedUser

        where:
        inputUser   | expectedUser
        "TestUser"  | "testuser"
        "TESTUSER"  | "testuser"
        "testuser"  | "testuser"
        "TeStUser" | "testuser"
    }

    def "authenticate with LDAP caching stores normalized username in cache"() {
        given: "An LDAP login module with caching and feature flag enabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(true)
        module._cacheDuration = 60000 // 1 minute cache (milliseconds)
        
        and: "Setup authentication"
        setupCallbackHandler(module, "TestUser", TEST_PASSWORD)
        mockLdapOperations(module, TEST_USER)

        when: "First authentication"
        boolean result1 = module.login()

        then: "Authentication succeeds"
        result1
        
        when: "Second authentication with different case"
        setupCallbackHandler(module, "TESTUSER", TEST_PASSWORD)
        boolean result2 = module.login()

        then: "Second authentication also succeeds (from cache)"
        result2
        
        and: "Both use normalized username"
        def currentUser = module.getCurrentUser()
        currentUser.getUserInfo().userName == "testuser"
    }

    def "authenticate handles LDAP search with original case but stores normalized username"() {
        given: "An LDAP login module with feature flag enabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(true)
        Subject testSubject = module.getSubject()
        
        and: "Setup callback handler with mixed case username"
        setupCallbackHandler(module, "TestUser", TEST_PASSWORD)
        
        and: "Mock LDAP operations - getUserInfo will receive normalized username"
        String capturedUsername = null
        module.getUserInfo(_) >> { String username ->
            capturedUsername = username
            return new UserInfo(
                username,
                PasswordCredential.getCredential(TEST_PASSWORD),
                [TEST_ROLE]
            )
        }

        when: "Authentication occurs"
        boolean result = module.login()
        module.getCurrentUser().setJAASInfo(testSubject)

        then: "Authentication succeeds"
        result
        
        and: "getUserInfo received normalized username, not original case"
        capturedUsername == "testuser"
        
        and: "UserInfo stores normalized username"
        def userPrincipal = testSubject.getPrincipals(RundeckPrincipal).find()
        userPrincipal.name == "testuser"
    }

    def "getCallBackAuth returns normalized username when feature enabled"() {
        given: "An LDAP login module with feature flag enabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(true)
        setupCallbackHandler(module, "TestUser", TEST_PASSWORD)

        when: "getCallBackAuth is called"
        Object[] credentials = module.getCallBackAuth()

        then: "Username is normalized"
        credentials[0] == "testuser"
        // Password is returned as char[] from PasswordCallback
        new String(credentials[1] as char[]) == TEST_PASSWORD
    }

    def "getUserInfo receives normalized username when feature enabled"() {
        given: "An LDAP login module with feature flag enabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(true)
        
        and: "Mock getUserInfo to capture the username parameter"
        String capturedUsername = null
        module.getUserInfo(_) >> { String username ->
            capturedUsername = username
            return new UserInfo(
                username,
                PasswordCredential.getCredential(TEST_PASSWORD),
                [TEST_ROLE]
            )
        }
        
        and: "Setup authentication with mixed case username"
        setupCallbackHandler(module, "TestUser", TEST_PASSWORD)

        when: "Login is performed"
        module.login()

        then: "getUserInfo received normalized username"
        capturedUsername == "testuser"
    }

    def "commit creates principals with normalized username"() {
        given: "An LDAP login module with feature flag enabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(true)
        Subject testSubject = module.getSubject()
        
        and: "Setup authentication with mixed case username"
        setupCallbackHandler(module, "TestUser", TEST_PASSWORD)
        mockLdapOperations(module, TEST_USER)
        
        when: "Login and commit"
        boolean result = module.login()
        module.getCurrentUser().setJAASInfo(testSubject)
        module.commit()

        then: "Authentication succeeds"
        result
        
        and: "Subject contains principals with normalized username"
        def principals = testSubject.getPrincipals(RundeckPrincipal)
        principals.size() == 1
        principals.find().name == "testuser"
        
        and: "Roles are also present"
        def roles = testSubject.getPrincipals(RundeckRole)
        roles.size() > 0
    }

    // Helper methods

    private JettyCachingLdapLoginModule createModuleWithFeatureFlag(boolean enabled) {
        // Pattern from working tests: Create real instance, set fields directly, don't call initialize()
        JettyCachingLdapLoginModule module = Spy(JettyCachingLdapLoginModule)
        
        // Set required LDAP config fields (needed to avoid NPE)
        module._contextFactory = "com.sun.jndi.ldap.LdapCtxFactory"
        module._providerUrl = "ldap://localhost:389"
        module._debug = true
        module._forceBindingLogin = false
        
        // Stub isCaseInsensitiveUsernameEnabled() to control feature flag
        module.isCaseInsensitiveUsernameEnabled() >> enabled
        
        // Mock configuration service
        def mockConfigService = Mock(ConfigurationService)
        module.getConfigurationService() >> mockConfigService
        
        // Set subject (like working tests do)
        module.setSubject(new Subject())
        
        return module
    }

    private void setupCallbackHandler(JettyCachingLdapLoginModule module, String username, String password) {
        CallbackHandler handler = createMockCallbackHandler(username, password)
        module.setCallbackHandler(handler)
    }

    private CallbackHandler createMockCallbackHandler(String username, String password) {
        return { callbacks ->
            callbacks.each { callback ->
                if (callback instanceof NameCallback) {
                    callback.setName(username)
                } else if (callback instanceof PasswordCallback) {
                    callback.setPassword(password.toCharArray())
                }
            }
        } as CallbackHandler
    }

    private void mockLdapOperations(JettyCachingLdapLoginModule module, String username) {
        // Stub getUserInfo to return a valid UserInfo (for non-binding login path)
        module.getUserInfo(_) >> { String user ->
            return new UserInfo(
                user,
                PasswordCredential.getCredential(TEST_PASSWORD),
                [TEST_ROLE]
            )
        }
        
        // Mock _rootContext for findUser (used by binding login)
        def mockSearchResult = Mock(SearchResult) {
            getNameInNamespace() >> "cn=$username,ou=users,dc=example,dc=com"
            getAttributes() >> createMockAttributes(username, [TEST_ROLE])
        }
        
        def mockDirContext = Mock(DirContext) {
            search(*_) >> new EnumImpl<SearchResult>([mockSearchResult])
            close() >> {}  // Stub close() to avoid NullPointerException in commit/abort
        }
        module._rootContext = mockDirContext
        
        // Mock role lookup for binding login
        def mockRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get('cn') >> Mock(Attribute) {
                    getAll() >> new EnumImpl<String>([TEST_ROLE])
                }
            }
        }]
        
        // Set userBindDirContextCreator closure (like working tests)
        DirContext userDir = Mock(DirContext) {
            search(*_) >> new EnumImpl<SearchResult>(mockRoles)
        }
        module.userBindDirContextCreator = { String user, Object pass -> userDir }
    }
    
    // Helper class from working tests
    class EnumImpl<T> implements NamingEnumeration<T> {
        List<T> list

        EnumImpl(List<T> list) {
            this.list = new ArrayList<>(list)
        }

        @Override
        T next() throws NamingException {
            return list.remove(0)
        }

        @Override
        boolean hasMore() throws NamingException {
            return list.size() > 0
        }

        @Override
        void close() throws NamingException {}

        @Override
        boolean hasMoreElements() {
            hasMore()
        }

        @Override
        T nextElement() {
            next()
        }
    }

    private Attributes createMockAttributes(String username, List<String> roles) {
        Attributes attrs = new BasicAttributes()
        attrs.put(new BasicAttribute("cn", username))
        
        if (roles) {
            Attribute roleAttr = new BasicAttribute("memberOf")
            roles.each { role ->
                roleAttr.add("cn=$role,ou=groups,dc=example,dc=com")
            }
            attrs.put(roleAttr)
        }
        
        return attrs
    }

    def cleanup() {
        Holders.metaClass = null
    }
}

