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

import grails.util.Holders
import org.eclipse.jetty.jaas.JAASRole
import org.springframework.context.ApplicationContext
import rundeck.services.ConfigurationService
import spock.lang.Specification
import spock.lang.Unroll

import javax.naming.NamingEnumeration
import javax.naming.NamingException
import javax.naming.directory.Attribute
import javax.naming.directory.Attributes
import javax.naming.directory.BasicAttributes
import javax.naming.directory.DirContext
import javax.naming.directory.SearchResult
import javax.naming.ldap.LdapContext
import javax.security.auth.Subject
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.login.LoginException
import java.security.Principal


class JettyCachingLdapLoginModuleTest extends Specification {
    def "DecodeBase64EncodedPwd"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        expect:
        module.decodeBase64EncodedPwd("noencoding") == "noencoding"
        module.decodeBase64EncodedPwd("MD5:tmXytOxIA6rGWhEKPFfv3A==") == "MD5:b665f2b4ec4803aac65a110a3c57efdc"
        module.decodeBase64EncodedPwd("MD5:038703c7230ae012e3c783ace1d09d64") == "MD5:038703c7230ae012e3c783ace1d09d64"
    }

    def "IsBase64"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        expect:
        !module.isBase64("notbase64")
        !module.isBase64("noencoding")
        module.isBase64("bXl0ZXN0c3RyaW5n")
        module.isBase64("bXl0ZXN0c3RyaW5nCg==")
    }

    def "IsHex"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        expect:
        module.isHex("b665f2b4ec4803aac65a110a3c57efdc")
        !module.isHex("b665f2b4ec4803aac65a110a3c57efd")
        !module.isHex("dWJlcjE3NjAzdGFzdGljc3dlZXRuZXNz")
        !module.isHex("dWJlcjE5MjJ0YXN0aWNzd2VldG5lc3M=")
    }

    def "bindingLogin user not found"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._forceBindingLogin = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        }
        def dirContext = Mock(DirContext) {
            1 * search(
                _,
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._userObjectClass, module._userIdAttribute, username],
                _
            ) >>
            Mock(NamingEnumeration) {
                _ * hasMoreElements() >> false
            }
            0 * _(*_)
        }
        module._rootContext = dirContext
        Subject testSubject = new Subject()
        when:
        boolean result = module.login()

        then:
        LoginException thrown = thrown()
        thrown.message == 'User not found.'


        where:
        username | _
        'auser'  | _
    }

    def "bindingLogin invalid password"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._forceBindingLogin = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = useRootContext
        module._roleBaseDn = 'roleBaseDn'
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        }
        def found = [Mock(SearchResult) {
            getNameInNamespace() >> "cn=$username,dc=test,dc=com"
        }]
        def dirContext = Mock(DirContext) {
            1 * search(
                _,
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._userObjectClass, module._userIdAttribute, username], _
            ) >>{new EnumImpl<SearchResult>(found)}
            0 * search(*_)
        }
        module._rootContext = dirContext

        module.userBindDirContextCreator = { String user, Object pass ->
            throw new NamingException("Login failure")
        }
        when:
        boolean result = module.login()

        then:
        !result

        LoginException thrown = thrown()
        thrown.message == 'Error obtaining user info.'


        where:
        username | useRootContext
        'auser'  | true
        'auser'  | false
    }

    def "bindingLogin should set user roles without pagination"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._forceBindingLogin = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = false
        module._roleBaseDn = 'roleBaseDn'
        module.rolePagination = false
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        }
        def found = [Mock(SearchResult) {
            getNameInNamespace() >> "cn=$username,dc=test,dc=com"
            getAttributes() >> new BasicAttributes()
        }]
        def dirContext = Mock(DirContext) {
            1 * search(
                _,
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._userObjectClass, module._userIdAttribute, username], _
            ) >> {new EnumImpl<SearchResult>(found)}
            0 * search(*_)
        }
        module._rootContext = dirContext
        def stringRoles = ['role1', 'role2']
        def foundRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> {new EnumImpl<String>(stringRoles)}
                }
            }
        }]
        DirContext userDir = Mock(DirContext) {
            1 * search(
                'roleBaseDn',
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._roleObjectClass, 'roleUsernameMemberAttribute', username],
                _
            ) >> {new EnumImpl<SearchResult>(foundRoles)}
            0 * _(*_)
        }
        module.userBindDirContextCreator = { String user, Object pass ->
            userDir
        }
        Subject testSubject = new Subject()
        when:
        boolean result = module.login()
        module.currentUser.setJAASInfo(testSubject)

        then:
        result
        null != testSubject.getPrincipals(Principal)
        username == testSubject.getPrincipals(Principal).first().name
        null != testSubject.getPrincipals(JAASRole)
        2 == testSubject.getPrincipals(JAASRole).size()
        ['role1', 'role2'] == testSubject.getPrincipals(JAASRole)*.name


        where:
        username | _
        'auser'  | _
    }
    class EnumImpl<T> implements NamingEnumeration<T>{
        List<T> list

        EnumImpl(final List<T> list) {
            this.list = new ArrayList<>(list)
        }

        @Override
        T next() throws NamingException {
            return list.remove(0)
        }

        @Override
        boolean hasMore() throws NamingException {
            return list.size()> 0
        }

        @Override
        void close() throws NamingException {

        }

        @Override
        boolean hasMoreElements() {
            hasMore()
        }

        @Override
        T nextElement() {
            next()
        }
    }

    def "bindingLogin while cached without pagination"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._cacheDuration = Integer.MAX_VALUE
        module._forceBindingLogin = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = false
        module._roleBaseDn = 'roleBaseDn'
        module.rolePagination = false
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        module.callbackHandler = Mock(CallbackHandler) {
            2 * handle(_) >> { it[0][0].name = username; it[0][1].object = password }
        }
        def found = [Mock(SearchResult) {
            getNameInNamespace() >> "cn=$username,dc=test,dc=com"
            getAttributes() >> new BasicAttributes()
        }]
        def dirContext = Mock(DirContext) {
            1 * search(
                _,
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._userObjectClass, module._userIdAttribute, username], _
            ) >> {new EnumImpl<SearchResult>(found)}

            0 * search(*_)
        }
        module._rootContext = dirContext
        def stringRoles = ['role1', 'role2']
        def foundRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> {new EnumImpl<String>(stringRoles)}
                }
            }
        }]
        DirContext userDir = Mock(DirContext) {
            1 * search(
                'roleBaseDn',
                JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                [module._roleObjectClass, 'roleUsernameMemberAttribute', username],
                _
            ) >> {new EnumImpl<SearchResult>(foundRoles)}

            0 * _(*_)
        }
        module.userBindDirContextCreator = { String user, Object pass ->
            userDir
        }
        Subject testSubject = new Subject()
        when:
        boolean result = module.login()
        boolean result2 = module.login()
        module.currentUser.setJAASInfo(testSubject)

        then:
        result
        result2
        null != testSubject.getPrincipals(Principal)
        username == testSubject.getPrincipals(Principal).first().name
        null != testSubject.getPrincipals(JAASRole)
        2 == testSubject.getPrincipals(JAASRole).size()
        ['role1', 'role2'] == testSubject.getPrincipals(JAASRole)*.name


        where:
        username | password
        'auser'  | 'apassword'
    }

    @Unroll
    def "get ldapBind pwd from configuration service"() {
        setup:
        ConfigurationService cfgSvc = null
        if(hasCfgService) {
            cfgSvc = Mock(ConfigurationService) {
                getString("security.ldap.bindPassword") >> ldapBindPwd
            }
        }

        when:
        TestJettyCachingLdapLoginModule ldapMod = new TestJettyCachingLdapLoginModule(cfgSvc)
        String actual = ldapMod.attemptBindPwdFromRdkConfig()

        then:
        actual == expected

        where:
        hasCfgService | ldapBindPwd   | expected
        false         | "123"         | null
        false         | "123"         | null
        true          | "123"         | "123"
        true          | null          | null
    }

    def "bindingLogin should set user roles paged"() {
        JettyCachingLdapLoginModule module = new JettyCachingLdapLoginModule()
        module._debug = true
        module._forceBindingLogin = true
        module._contextFactory = "notnull"
        module._providerUrl = "notnull"
        module._forceBindingLoginUseRootContextForRoles = false
        module._roleBaseDn = 'roleBaseDn'
        module._roleUsernameMemberAttribute = 'roleUsernameMemberAttribute'
        module.rolePagination = true
        module.callbackHandler = Mock(CallbackHandler) {
            1 * handle(_) >> { it[0][0].name = username; it[0][1].object = 'apassword' }
        }
        def found = [Mock(SearchResult) {
            getNameInNamespace() >> "cn=$username,dc=test,dc=com"
            getAttributes() >> new BasicAttributes()
        }]
        def dirContext = Mock(DirContext) {
            1 * search(
                    _,
                    JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                    [module._userObjectClass, module._userIdAttribute, username], _
            ) >> {new EnumImpl<SearchResult>(found)}
            0 * search(*_)
        }
        module._rootContext = dirContext
        def stringRoles = ['role1', 'role2']
        def foundRoles = [Mock(SearchResult) {
            getAttributes() >> Mock(Attributes) {
                get(module._roleNameAttribute) >> Mock(Attribute) {
                    getAll() >> {new EnumImpl<String>(stringRoles)}
                }
            }
        }]
        def ldapContext = Mock(LdapContext){
            1 * search(
                    'roleBaseDn',
                    JettyCachingLdapLoginModule.OBJECT_CLASS_FILTER,
                    [module._roleObjectClass, 'roleUsernameMemberAttribute', username],
                    _
            ) >> {new EnumImpl<SearchResult>(foundRoles)}
            0 * search(*_)
        }
        module.ldapContext = ldapContext
        DirContext userDir = Mock(DirContext) {
            0 * _(*_)
        }
        module.userBindDirContextCreator = { String user, Object pass ->
            userDir
        }
        Subject testSubject = new Subject()
        when:
        boolean result = module.login()
        module.currentUser.setJAASInfo(testSubject)

        then:
        result
        null != testSubject.getPrincipals(Principal)
        username == testSubject.getPrincipals(Principal).first().name
        null != testSubject.getPrincipals(JAASRole)
        2 == testSubject.getPrincipals(JAASRole).size()
        ['role1', 'role2'] == testSubject.getPrincipals(JAASRole)*.name


        where:
        username | _
        'auser'  | _
    }

    static class TestJettyCachingLdapLoginModule extends JettyCachingLdapLoginModule {

        private final ConfigurationService cfgSvc

        TestJettyCachingLdapLoginModule(ConfigurationService cfgSvc) {
            this.cfgSvc = cfgSvc
        }

        @Override
        def ConfigurationService getConfigurationService() {
            return cfgSvc
        }
    }
}
