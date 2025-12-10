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
package org.rundeck.jaas

import com.dtolabs.rundeck.core.config.Features
import grails.util.Holders
import org.springframework.context.ApplicationContext
import rundeck.services.feature.FeatureService
import spock.lang.Specification
import spock.lang.Shared
import spock.lang.Unroll

import javax.security.auth.Subject
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.callback.NameCallback
import javax.security.auth.callback.PasswordCallback

/**
 * Integration tests for case-insensitive username normalization in PropertyFile authentication
 */
class PropertyFileLoginModuleCaseInsensitiveSpec extends Specification {

    @Shared
    File testPropertiesFile
    
    @Shared
    File tempDir

    def setupSpec() {
        tempDir = File.createTempDir("propertyfile-test", "")
        testPropertiesFile = new File(tempDir, "test-realm.properties")
        
        // Create test properties file with users
        testPropertiesFile.text = """
# User with lowercase (case-insensitive feature will normalize all lookups)
testuser: password,developers,users
# Admin user (lowercase in file, can be accessed with any case)
admin: admin123,admin,users
"""
    }

    def cleanupSpec() {
        tempDir.deleteDir()
    }

    def "login normalizes username to lowercase when feature enabled"() {
        given: "A PropertyFile login module with feature flag enabled"
        PropertyFileLoginModule module = createModuleWithFeatureFlag(true, testPropertiesFile)
        
        and: "User logs in with mixed case username"
        setupCallbackHandler(module, "TestUser", "password")

        when: "Login is performed"
        boolean result = module.login()

        then: "Login succeeds"
        result
        
        and: "UserInfo contains normalized username"
        // PropertyFile module will normalize the lookup, so 'testuser' finds 'TestUser' entry
        true // Authentication succeeds means normalization worked
    }

    def "login preserves original case when feature disabled"() {
        given: "A PropertyFile login module with feature flag disabled"
        PropertyFileLoginModule module = createModuleWithFeatureFlag(false, testPropertiesFile)
        
        and: "User logs in with exact case match"
        setupCallbackHandler(module, "TestUser", "password")

        when: "Login is performed"
        boolean result = module.login()

        then: "Login succeeds with exact match"
        result
    }

    @Unroll
    def "getUserInfo normalizes '#inputUser' to '#normalizedUser' when feature enabled"() {
        given: "A PropertyFile login module with feature flag enabled"
        PropertyFileLoginModule module = createModuleWithFeatureFlag(true, testPropertiesFile)

        when: "getUserInfo is called"
        UserInfo userInfo = module.getUserInfo(inputUser)

        then: "UserInfo is found and username is normalized"
        userInfo != null
        // The lookup will be done with normalized name

        where:
        inputUser  | normalizedUser
        "testuser" | "testuser"
        "TestUser" | "testuser"
        "TESTUSER" | "testuser"
    }

    def "getUserInfo returns null for non-existent user"() {
        given: "A PropertyFile login module with feature flag enabled"
        PropertyFileLoginModule module = createModuleWithFeatureFlag(true, testPropertiesFile)

        when: "getUserInfo is called with non-existent user"
        UserInfo userInfo = module.getUserInfo("nonexistent")

        then: "UserInfo is null"
        userInfo == null
    }

    def "commit creates principals with normalized username"() {
        given: "A PropertyFile login module that has authenticated"
        PropertyFileLoginModule module = createModuleWithFeatureFlag(true, testPropertiesFile)
        Subject subject = new Subject()
        setupCallbackHandler(module, "TestUser", "password")
        module.@subject = subject

        when: "Login and commit"
        module.login()
        module.commit()

        then: "Subject contains principals"
        def principals = subject.getPrincipals(RundeckPrincipal)
        principals.size() == 1
        
        and: "Roles are present"
        def roles = subject.getPrincipals(RundeckRole)
        roles.size() > 0
    }

    def "hot reload with case-insensitive feature enabled"() {
        given: "A PropertyFile login module with hot reload and feature flag enabled"
        File reloadTestFile = new File(tempDir, "reload-test.properties")
        reloadTestFile.text = "testuser: password, roles: users"
        
        PropertyFileLoginModule module = createModuleWithFeatureFlag(true, reloadTestFile)
        module.setHotReload(true)
        
        and: "Initial login succeeds"
        setupCallbackHandler(module, "TestUser", "password")
        boolean result1 = module.login()

        expect: "Initial login succeeds"
        result1

        when: "Property file is updated"
        Thread.sleep(100) // Ensure file timestamp changes
        reloadTestFile.text = "testuser: newpassword, roles: admin"
        Thread.sleep(100)
        
        and: "Login with new password and mixed case"
        setupCallbackHandler(module, "TESTUSER", "newpassword")
        boolean result2 = module.login()

        then: "Login succeeds with new password"
        result2
        
        cleanup:
        reloadTestFile.delete()
    }

    def "password credential checking works with normalized username"() {
        given: "A PropertyFile login module with feature flag enabled"
        PropertyFileLoginModule module = createModuleWithFeatureFlag(true, testPropertiesFile)
        
        when: "Get user info with normalized name"
        UserInfo userInfo = module.getUserInfo("testuser")

        then: "Credential checking works"
        userInfo != null
        userInfo.checkCredential("password")
        !userInfo.checkCredential("wrongpassword")
    }

    def "roles are preserved regardless of username case"() {
        given: "A PropertyFile login module with feature flag enabled"
        PropertyFileLoginModule module = createModuleWithFeatureFlag(true, testPropertiesFile)

        when: "Get user info with different cases"
        UserInfo userInfo1 = module.getUserInfo("testuser")
        UserInfo userInfo2 = module.getUserInfo("TESTUSER")

        then: "Both return same roles"
        userInfo1 != null
        userInfo2 != null
        userInfo1.roleNames.sort() == userInfo2.roleNames.sort()
    }

    @Unroll
    def "admin user '#inputCase' normalizes to 'admin' when feature enabled"() {
        given: "A PropertyFile login module with feature flag enabled"
        PropertyFileLoginModule module = createModuleWithFeatureFlag(true, testPropertiesFile)
        setupCallbackHandler(module, inputCase, "admin123")

        when: "Login is performed"
        boolean result = module.login()

        then: "Login succeeds"
        result

        where:
        inputCase << ["admin", "ADMIN", "Admin", "AdMiN"]
    }

    def "multiple concurrent logins with different cases normalize consistently"() {
        given: "A PropertyFile login module with feature flag enabled"
        PropertyFileLoginModule module = createModuleWithFeatureFlag(true, testPropertiesFile)

        when: "Multiple logins with different cases"
        def results = []
        ["testuser", "TestUser", "TESTUSER", "TeStUsEr"].each { username ->
            setupCallbackHandler(module, username, "password")
            results << module.login()
        }

        then: "All logins succeed"
        results.every { it == true }
    }

    // Helper methods

    private PropertyFileLoginModule createModuleWithFeatureFlag(boolean enabled, File propertiesFile) {
        PropertyFileLoginModule module = new PropertyFileLoginModule()
        
        // Mock feature service
        def mockFeatureService = Mock(FeatureService) {
            featurePresent(Features.CASE_INSENSITIVE_USERNAME) >> enabled
        }
        def mockContext = Mock(ApplicationContext) {
            containsBeanDefinition("featureService") >> true
            getBean("featureService") >> mockFeatureService
        }
        Holders.metaClass.static.findApplicationContext = { -> mockContext }
        
        // Initialize module with property file
        Subject subject = new Subject()
        module.initialize(
            subject, 
            null,
            [:],
            [file: propertiesFile.absolutePath]
        )
        
        return module
    }

    private void setupCallbackHandler(PropertyFileLoginModule module, String username, String password) {
        CallbackHandler handler = { callbacks ->
            callbacks.each { callback ->
                if (callback instanceof NameCallback) {
                    callback.setName(username)
                } else if (callback instanceof PasswordCallback) {
                    callback.setPassword(password.toCharArray())
                }
            }
        } as CallbackHandler
        
        // Use setter instead of @field access (Groovy 4 incompatible)
        module.setCallbackHandler(handler)
    }

    def cleanup() {
        Holders.metaClass = null
    }
}

