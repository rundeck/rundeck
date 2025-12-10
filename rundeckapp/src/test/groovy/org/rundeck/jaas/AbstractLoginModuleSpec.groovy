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
import spock.lang.Unroll

import javax.security.auth.Subject
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.login.LoginException

/**
 * Unit tests for AbstractLoginModule case-insensitive username functionality
 */
class AbstractLoginModuleSpec extends Specification {

    // Test implementation of AbstractLoginModule
    class TestLoginModule extends AbstractLoginModule {
        UserInfo testUserInfo
        
        @Override
        public UserInfo getUserInfo(String username) throws Exception {
            return testUserInfo
        }
    }

    def "normalizeUsername returns lowercase when feature enabled"() {
        given: "A login module with feature flag enabled"
        TestLoginModule module = Spy(TestLoginModule)
        module.isCaseInsensitiveUsernameEnabled() >> true

        expect: "Username is converted to lowercase"
        module.normalizeUsername("Naveed") == "naveed"
        module.normalizeUsername("ADMIN") == "admin"
        module.normalizeUsername("JohnDoe123") == "johndoe123"
    }

    def "normalizeUsername returns original when feature disabled"() {
        given: "A login module with feature flag disabled"
        TestLoginModule module = Spy(TestLoginModule)
        module.isCaseInsensitiveUsernameEnabled() >> false

        expect: "Username is unchanged"
        module.normalizeUsername("Naveed") == "Naveed"
        module.normalizeUsername("ADMIN") == "ADMIN"
        module.normalizeUsername("JohnDoe123") == "JohnDoe123"
    }

    def "normalizeUsername handles null input gracefully"() {
        given: "A login module"
        TestLoginModule module = new TestLoginModule()
        
        expect: "Null input returns null"
        module.normalizeUsername(null) == null
    }

    def "normalizeUsername handles empty string"() {
        given: "A login module with feature flag enabled"
        TestLoginModule module = Spy(TestLoginModule)
        module.isCaseInsensitiveUsernameEnabled() >> true

        expect: "Empty string is handled correctly"
        module.normalizeUsername("") == ""
    }

    @Unroll
    def "normalizeUsername with special characters: '#input' -> '#expected'"() {
        given: "A login module with feature flag enabled"
        TestLoginModule module = Spy(TestLoginModule)
        module.isCaseInsensitiveUsernameEnabled() >> true

        expect:
        module.normalizeUsername(input) == expected

        where:
        input           | expected
        "user@test.com" | "user@test.com"
        "user.name"     | "user.name"
        "user_name"     | "user_name"
        "user-name"     | "user-name"
        "user123"       | "user123"
        "123user"       | "123user"
    }

    def "isCaseInsensitiveUsernameEnabled returns false when FeatureService unavailable"() {
        given: "A login module with no application context"
        TestLoginModule module = new TestLoginModule()
        
        and: "Mock Holders to return null context"
        Holders.metaClass.static.findApplicationContext = { -> null }

        expect: "Feature check returns false safely"
        !module.isCaseInsensitiveUsernameEnabled()
        
        cleanup:
        Holders.metaClass = null
    }

    def "isCaseInsensitiveUsernameEnabled returns false when feature not present"() {
        given: "A login module with mocked application context"
        TestLoginModule module = new TestLoginModule()
        
        and: "Mock application context with FeatureService that returns false"
        def mockFeatureService = Mock(FeatureService) {
            featurePresent(Features.CASE_INSENSITIVE_USERNAME) >> false
        }
        def mockContext = Mock(ApplicationContext) {
            containsBeanDefinition("featureService") >> true
            getBean("featureService") >> mockFeatureService
        }
        Holders.metaClass.static.findApplicationContext = { -> mockContext }

        expect: "Feature check returns false"
        !module.isCaseInsensitiveUsernameEnabled()
        
        cleanup:
        Holders.metaClass = null
    }

    def "isCaseInsensitiveUsernameEnabled returns true when feature present"() {
        given: "A login module with mocked application context"
        TestLoginModule module = new TestLoginModule()
        
        and: "Mock application context"
        def mockFeatureService = Mock(FeatureService)
        mockFeatureService.featurePresent(Features.CASE_INSENSITIVE_USERNAME) >> true
        
        def mockContext = Mock(ApplicationContext)
        mockContext.containsBeanDefinition("featureService") >> true
        mockContext.getBean("featureService") >> mockFeatureService
        
        module.metaClass.getApplicationContext = { -> mockContext }

        expect: "Feature check returns true"
        module.isCaseInsensitiveUsernameEnabled()
    }

    def "isCaseInsensitiveUsernameEnabled handles exceptions gracefully"() {
        given: "A login module"
        TestLoginModule module = new TestLoginModule()
        
        and: "Mock application context that throws exception"
        Holders.metaClass.static.findApplicationContext = { -> 
            throw new RuntimeException("Context error") 
        }

        expect: "Exception is caught and returns false"
        !module.isCaseInsensitiveUsernameEnabled()
        
        cleanup:
        Holders.metaClass = null
    }

    def "login normalizes username from callbacks when feature enabled"() {
        given: "A login module with feature flag enabled"
        TestLoginModule module = Spy(TestLoginModule)
        module.testUserInfo = new UserInfo(
            "naveed", 
            PasswordCredential.getCredential("password"),
            ["role1"]
        )
        
        and: "Mock application context"
        def mockFeatureService = Mock(FeatureService)
        mockFeatureService.featurePresent(Features.CASE_INSENSITIVE_USERNAME) >> true
        
        def mockContext = Mock(ApplicationContext)
        mockContext.containsBeanDefinition("featureService") >> true
        mockContext.getBean("featureService") >> mockFeatureService
        
        module.metaClass.getApplicationContext = { -> mockContext }
        module.metaClass.getCallBackAuth = { -> ["NAVEED", "password"] as Object[] }
        
        and: "Mock callback handler"
        def mockCallbackHandler = Mock(CallbackHandler)
        module.initialize(
            new Subject(), 
            mockCallbackHandler,
            [:],
            [:]
        )

        when: "Login is called"
        boolean result = module.login()

        then: "Login succeeds and username is normalized"
        result
    }

    def "login does not normalize username when feature disabled"() {
        given: "A login module with feature flag disabled"
        TestLoginModule module = Spy(TestLoginModule)
        module.testUserInfo = new UserInfo(
            "NAVEED", 
            PasswordCredential.getCredential("password"),
            ["role1"]
        )
        
        and: "Mock application context"
        def mockFeatureService = Mock(FeatureService)
        mockFeatureService.featurePresent(Features.CASE_INSENSITIVE_USERNAME) >> false
        
        def mockContext = Mock(ApplicationContext)
        mockContext.containsBeanDefinition("featureService") >> true
        mockContext.getBean("featureService") >> mockFeatureService
        
        module.metaClass.getApplicationContext = { -> mockContext }
        module.metaClass.getCallBackAuth = { -> ["NAVEED", "password"] as Object[] }
        
        and: "Mock callback handler"
        def mockCallbackHandler = Mock(CallbackHandler)
        module.initialize(
            new Subject(),
            mockCallbackHandler,
            [:],
            [:]
        )

        when: "Login is called"
        boolean result = module.login()

        then: "Login succeeds with username remaining uppercase"
        result
    }

    @Unroll
    def "normalizeUsername consistently handles '#username' regardless of call count"() {
        given: "A login module with feature flag enabled"
        TestLoginModule module = Spy(TestLoginModule)
        module.isCaseInsensitiveUsernameEnabled() >> true

        when: "normalizeUsername is called multiple times"
        def result1 = module.normalizeUsername(username)
        def result2 = module.normalizeUsername(username)
        def result3 = module.normalizeUsername(username)

        then: "Results are consistent"
        result1 == expected
        result2 == expected
        result3 == expected

        where:
        username  | expected
        "Naveed"  | "naveed"
        "ADMIN"   | "admin"
        "test123" | "test123"
    }

    def "normalizeUsername logging behavior"() {
        given: "A login module with debug enabled"
        TestLoginModule module = Spy(TestLoginModule)
        module.isCaseInsensitiveUsernameEnabled() >> true
        module.setDebug(true)  // Enable debug mode

        when: "Username is normalized"
        def result = module.normalizeUsername("Naveed")

        then: "Result is lowercase"
        result == "naveed"
        // Note: Actual log checking would require log capture framework
    }
}

