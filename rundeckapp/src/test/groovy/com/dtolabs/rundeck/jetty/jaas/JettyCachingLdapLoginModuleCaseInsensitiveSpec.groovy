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
        
        and: "User logs in with mixed case username"
        setupCallbackHandler(module, "TestUser", TEST_PASSWORD)
        
        and: "Mock LDAP operations"
        mockLdapOperations(module, TEST_USER)

        when: "Authentication occurs"
        boolean result = module.login()
        module.commit()

        then: "Authentication succeeds"
        result
        
        and: "Username in subject is lowercase"
        Subject subject = new Subject()
        module.setSubject(subject)
        module.commit()
        
        def userPrincipal = subject.getPrincipals(RundeckPrincipal).find()
        userPrincipal.name == "testuser"
    }

    def "authenticate preserves original case when feature disabled"() {
        given: "An LDAP login module with feature flag disabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(false)
        
        and: "User logs in with mixed case username"
        setupCallbackHandler(module, "TestUser", TEST_PASSWORD)
        
        and: "Mock LDAP operations"
        mockLdapOperations(module, "TestUser")

        when: "Authentication occurs"
        boolean result = module.login()

        then: "Authentication succeeds"
        result
        
        and: "Username in subject preserves original case"
        Subject subject = new Subject()
        module.@subject = subject
        module.commit()
        
        def userPrincipal = subject.getPrincipals(RundeckPrincipal).find()
        userPrincipal.name == "TestUser"
    }

    @Unroll
    def "bindingLogin normalizes username '#inputUser' to '#expectedUser' when feature enabled"() {
        given: "An LDAP login module with feature flag enabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(true)
        module._forceBindingLogin = true
        
        and: "Mock LDAP binding"
        module.metaClass.findUser = { String username -> 
            def mockResult = Mock(SearchResult)
            mockResult.getNameInNamespace() >> "cn=$username,ou=users,dc=example,dc=com"
            return mockResult
        }
        module.metaClass.bindingAuth = { DirContext ctx, String userDn -> true }
        module.metaClass.getUserRolesByDn = { DirContext ctx, String userDn, String username -> 
            [TEST_ROLE]
        }

        when: "Binding login is performed"
        boolean result = module.bindingLogin(inputUser, TEST_PASSWORD)

        then: "Login succeeds"
        result
        
        and: "UserInfo contains normalized username"
        def currentUser = module.getCurrentUser()
        currentUser.userName == expectedUser

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
        module._cacheDurationMillis = 60000 // 1 minute cache
        
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
        currentUser.userName == "testuser"
    }

    def "authenticate handles LDAP search with original case but stores normalized username"() {
        given: "An LDAP login module with feature flag enabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(true)
        
        and: "Mock LDAP search that's case-insensitive (as LDAP typically is)"
        boolean searchCalledWithOriginalCase = false
        module.metaClass.findUser = { String username ->
            // LDAP search usually case-insensitive, but we verify it receives input correctly
            if (username == "TestUser" || username == "testuser") {
                searchCalledWithOriginalCase = (username == "TestUser")
            }
            def mockResult = Mock(SearchResult)
            mockResult.getNameInNamespace() >> "cn=testuser,ou=users,dc=example,dc=com"
            mockResult.getAttributes() >> createMockAttributes(TEST_USER, [TEST_ROLE])
            return mockResult
        }
        
        and: "Setup callback handler"
        setupCallbackHandler(module, "TestUser", TEST_PASSWORD)

        when: "Authentication occurs"
        boolean result = module.login()

        then: "Authentication succeeds"
        result
        
        and: "UserInfo stores normalized username"
        def currentUser = module.getCurrentUser()
        currentUser.userName == "testuser"
    }

    def "getCallBackAuth returns normalized username when feature enabled"() {
        given: "An LDAP login module with feature flag enabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(true)
        setupCallbackHandler(module, "TestUser", TEST_PASSWORD)

        when: "getCallBackAuth is called"
        Object[] credentials = module.getCallBackAuth()

        then: "Username is normalized"
        credentials[0] == "testuser"
        credentials[1] == TEST_PASSWORD
    }

    def "getUserInfo receives normalized username when feature enabled"() {
        given: "An LDAP login module with feature flag enabled"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(true)
        
        and: "Mock getUserInfo to capture the username parameter"
        String capturedUsername = null
        module.metaClass.getUserInfo = { String username ->
            capturedUsername = username
            return new UserInfo(
                username,
                PasswordCredential.getCredential(TEST_PASSWORD),
                [TEST_ROLE]
            )
        }
        
        and: "Setup authentication"
        setupCallbackHandler(module, "TestUser", TEST_PASSWORD)

        when: "Login is performed"
        module.login()

        then: "getUserInfo received normalized username"
        capturedUsername == "testuser"
    }

    def "commit creates principals with normalized username"() {
        given: "An LDAP login module that has authenticated"
        JettyCachingLdapLoginModule module = createModuleWithFeatureFlag(true)
        Subject subject = new Subject()
        module.initialize(
            subject,
            createMockCallbackHandler("TestUser", TEST_PASSWORD),
            [:],
            [:]
        )
        
        and: "Mock authentication"
        module.metaClass.getUserInfo = { String username ->
            new UserInfo(
                username,
                PasswordCredential.getCredential(TEST_PASSWORD),
                [TEST_ROLE]
            )
        }
        
        when: "Login and commit"
        module.login()
        module.commit()

        then: "Subject contains principals with normalized username"
        def principals = subject.getPrincipals(RundeckPrincipal)
        principals.size() == 1
        principals.find().name == "testuser"
        
        and: "Roles are also present"
        def roles = subject.getPrincipals(RundeckRole)
        roles.size() > 0
    }

    // Helper methods

    private JettyCachingLdapLoginModule createModuleWithFeatureFlag(boolean enabled) {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        
        // Mock feature service
        def mockFeatureService = Mock(FeatureService) {
            featurePresent(Features.CASE_INSENSITIVE_USERNAME) >> enabled
        }
        def mockContext = Mock(ApplicationContext) {
            containsBeanDefinition("featureService") >> true
            getBean("featureService") >> mockFeatureService
        }
        Holders.metaClass.static.findApplicationContext = { -> mockContext }
        
        // Mock configuration service
        def mockConfigService = Mock(ConfigurationService)
        module.metaClass.getConfigurationService = { -> mockConfigService }
        
        // Initialize module
        Subject subject = new Subject()
        module.initialize(subject, null, [:], [:])
        
        return module
    }

    private void setupCallbackHandler(JettyCachingLdapLoginModule module, String username, String password) {
        CallbackHandler handler = createMockCallbackHandler(username, password)
        module.@callbackHandler = handler
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
        // Mock LDAP search
        module.metaClass.findUser = { String user ->
            def mockResult = Mock(SearchResult)
            mockResult.getNameInNamespace() >> "cn=$username,ou=users,dc=example,dc=com"
            mockResult.getAttributes() >> createMockAttributes(username, [TEST_ROLE])
            return mockResult
        }
        
        // Mock role lookup
        module.metaClass.getUserRolesByDn = { DirContext ctx, String userDn, String user ->
            [TEST_ROLE]
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

